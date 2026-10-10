package br.com.gestaoservicos.usuario.service;

import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO;
import br.com.gestaoservicos.unidade.mapper.UnidadeMapper;
import br.com.gestaoservicos.usuario.dto.MeResponseDTO;
import br.com.gestaoservicos.usuario.model.Usuario;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final AssociacaoRepository associacoes;
    private final UnidadeMapper unidadeMapper;
    private final br.com.gestaoservicos.unidade.repository.UnidadeRepository todasUnidades;

    public UsuarioService(UsuarioRepository usuarios, AssociacaoRepository associacoes, UnidadeMapper unidadeMapper,
            br.com.gestaoservicos.unidade.repository.UnidadeRepository todasUnidades) {
        this.usuarios = usuarios;
        this.associacoes = associacoes;
        this.unidadeMapper = unidadeMapper;
        this.todasUnidades = todasUnidades;
    }

    @Transactional(readOnly = true)
    public MeResponseDTO consultarMe(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        var unidades = usuario.isAdministradorGeral() ? todasUnidades.findAll(org.springframework.data.domain.Sort.by("nome")).stream()
                .map(u -> new AssociacaoUnidadeResponseDTO(u.getId(), u.getNome(), br.com.gestaoservicos.associacao.model.Perfil.ADMIN)).toList()
                : associacoes.findAllByUsuarioIdAndAtivaTrueOrderByUnidadeNome(usuarioId).stream()
                .map(unidadeMapper::paraAssociacaoResponseDTO)
                .toList();
        return new MeResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAdministradorGeral(), unidades);
    }

    @Transactional(readOnly = true)
    public AssociacaoUnidadeResponseDTO selecionarUnidade(UUID usuarioId, UUID unidadeId) {
        if (usuarios.findById(usuarioId).map(Usuario::isAdministradorGeral).orElse(false)) {
            var unidade = todasUnidades.findById(unidadeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
            return new AssociacaoUnidadeResponseDTO(unidade.getId(), unidade.getNome(), br.com.gestaoservicos.associacao.model.Perfil.ADMIN);
        }
        return associacoes.findByUsuarioIdAndUnidadeIdAndAtivaTrue(usuarioId, unidadeId)
                .map(unidadeMapper::paraAssociacaoResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuário não possui acesso à unidade informada"));
    }
}
