package br.com.gestaoservicos.associacao.service;

import br.com.gestaoservicos.associacao.dto.UsuarioUnidadeResponseDTO;
import br.com.gestaoservicos.associacao.dto.CriarUsuarioUnidadeRequestDTO;
import br.com.gestaoservicos.associacao.mapper.AssociacaoMapper;
import br.com.gestaoservicos.associacao.model.Associacao;
import br.com.gestaoservicos.associacao.model.Perfil;
import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.gestaoservicos.usuario.model.Usuario;

import java.util.List;
import java.util.UUID;

@Service
public class AssociacaoService {
    private final AssociacaoRepository associacoes;
    private final UsuarioRepository usuarios;
    private final UnidadeRepository unidades;
    private final AssociacaoMapper mapper;
    private final PasswordEncoder codificadorSenha;

    public AssociacaoService(AssociacaoRepository associacoes, UsuarioRepository usuarios, UnidadeRepository unidades,
                             AssociacaoMapper mapper, PasswordEncoder codificadorSenha) {
        this.associacoes = associacoes;
        this.usuarios = usuarios;
        this.unidades = unidades;
        this.mapper = mapper;
        this.codificadorSenha = codificadorSenha;
    }

    @Transactional
    public UsuarioUnidadeResponseDTO criarUsuario(UUID unidadeId, CriarUsuarioUnidadeRequestDTO requisicao) {
        validarGestor(null, requisicao.perfil());
        var email = requisicao.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCase(email)) throw new RegraAssociacaoException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.");
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
        var usuario = usuarios.save(new Usuario(requisicao.nome().trim(), email, codificadorSenha.encode(requisicao.senha())));
        return mapper.paraResponseDTO(associacoes.save(new Associacao(usuario, unidade, requisicao.perfil())));
    }

    @Transactional(readOnly = true)
    public List<UsuarioUnidadeResponseDTO> listarAtivos(UUID unidadeId) {
        return associacoes.findAllByUnidadeIdOrderByUsuarioNome(unidadeId).stream()
                .map(mapper::paraResponseDTO).toList();
    }

    @Transactional
    public UsuarioUnidadeResponseDTO adicionar(UUID unidadeId, String email, Perfil perfil) {
        validarGestor(null, perfil);
        var usuario = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RegraAssociacaoException("USUARIO_NAO_ENCONTRADO", "Não há usuário cadastrado com este e-mail."));
        var associacaoExistente = associacoes.findByUsuarioIdAndUnidadeId(usuario.getId(), unidadeId);
        associacaoExistente.ifPresent(a -> validarGestor(a.getPerfil(), perfil));
        if (associacaoExistente.filter(Associacao::isAtiva).isPresent()) {
            throw new RegraAssociacaoException("USUARIO_JA_POSSUI_ACESSO_UNIDADE", "Este usuário já possui acesso à unidade." );
        }
        if (associacaoExistente.isPresent()) {
            var associacao = associacaoExistente.get();
            associacao.reativar(perfil);
            return mapper.paraResponseDTO(associacao);
        }
        var unidade = unidades.findById(unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
        return mapper.paraResponseDTO(associacoes.save(new Associacao(usuario, unidade, perfil)));
    }

    @Transactional
    public UsuarioUnidadeResponseDTO alterarPerfil(UUID unidadeId, UUID associacaoId, Perfil perfil) {
        bloquearUnidade(unidadeId);
        var associacao = obterAtiva(unidadeId, associacaoId);
        validarGestor(associacao.getPerfil(), perfil);
        validarNaoRemoveUltimoAdmin(unidadeId, associacao.getPerfil(), perfil);
        associacao.alterarPerfil(perfil);
        return mapper.paraResponseDTO(associacao);
    }

    @Transactional
    public void desativar(UUID unidadeId, UUID associacaoId) {
        bloquearUnidade(unidadeId);
        var associacao = obterAtiva(unidadeId, associacaoId);
        validarGestor(associacao.getPerfil(), null);
        validarNaoRemoveUltimoAdmin(unidadeId, associacao.getPerfil(), null);
        associacao.desativar();
    }

    @Transactional
    public UsuarioUnidadeResponseDTO reativar(UUID unidadeId, UUID id) {
        bloquearUnidade(unidadeId);
        var associacao = associacoes.findByIdAndUnidadeId(id, unidadeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado nesta unidade"));
        validarGestor(associacao.getPerfil(), associacao.getPerfil());
        associacao.reativar(associacao.getPerfil());
        return mapper.paraResponseDTO(associacao);
    }

    @Transactional
    public UsuarioUnidadeResponseDTO atualizar(UUID unidadeId, UUID id,
            br.com.gestaoservicos.associacao.dto.AtualizarUsuarioRequestDTO dto) {
        bloquearUnidade(unidadeId);
        var associacao = obterAtiva(unidadeId, id);
        validarGestor(associacao.getPerfil(), dto.perfil());
        validarNaoRemoveUltimoAdmin(unidadeId, associacao.getPerfil(), dto.perfil());
        var usuario = associacao.getUsuario();
        if (usuario.isAdministradorGeral())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A conta de administrador geral não pode ser editada pela unidade.");
        String email = dto.email().trim().toLowerCase(java.util.Locale.ROOT);
        if ((!usuario.getNome().equals(dto.nome().trim()) || !usuario.getEmail().equals(email))
            && associacoes.findAllByUsuarioIdAndAtivaTrueOrderByUnidadeNome(usuario.getId()).size()>1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta conta participa de várias unidades. Nome e e-mail precisam ser alterados pelo próprio usuário.");
        if (usuarios.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(usuario.getId())).isPresent())
            throw new RegraAssociacaoException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.");
        usuario.atualizarCadastro(dto.nome().trim(), email);
        associacao.alterarPerfil(dto.perfil());
        return mapper.paraResponseDTO(associacao);
    }

    private void validarGestor(Perfil atual, Perfil novo) {
        if (br.com.gestaoservicos.security.ContextoUnidade.atual().map(c -> c.perfil() == Perfil.GESTOR).orElse(false)
            && (atual == Perfil.ADMIN || novo == Perfil.ADMIN))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente administradores podem gerenciar administradores.");
    }

    private Associacao obterAtiva(UUID unidadeId, UUID associacaoId) {
        return associacoes.findByIdAndUnidadeIdAndAtivaTrue(associacaoId, unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado nesta unidade"));
    }

    private void bloquearUnidade(UUID unidadeId) {
        unidades.findByIdComBloqueio(unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
    }

    private void validarNaoRemoveUltimoAdmin(UUID unidadeId, Perfil perfilAtual, Perfil novoPerfil) {
        if (perfilAtual == Perfil.ADMIN && novoPerfil != Perfil.ADMIN
                && associacoes.countByUnidadeIdAndPerfilAndAtivaTrue(unidadeId, Perfil.ADMIN) == 1) {
            throw new RegraAssociacaoException("ULTIMO_ADMIN_DA_UNIDADE", "A unidade precisa manter ao menos um administrador ativo.");
        }
    }
}
