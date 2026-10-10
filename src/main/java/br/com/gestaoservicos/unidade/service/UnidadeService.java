package br.com.gestaoservicos.unidade.service;

import br.com.gestaoservicos.associacao.model.Associacao;
import br.com.gestaoservicos.associacao.model.Perfil;
import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import br.com.gestaoservicos.unidade.dto.UnidadeResponseDTO;
import br.com.gestaoservicos.unidade.mapper.UnidadeMapper;
import br.com.gestaoservicos.unidade.model.Unidade;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UnidadeService {
    @Transactional(readOnly = true)
    public java.util.List<br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO> listarParaAdministradorGeral() {
        return unidades.findAll(org.springframework.data.domain.Sort.by("nome")).stream()
                .map(u -> new br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO(u.getId(), u.getNome(), Perfil.ADMIN)).toList();
    }
    private final UnidadeRepository unidades;
    private final UsuarioRepository usuarios;
    private final AssociacaoRepository associacoes;
    private final UnidadeMapper mapper;

    public UnidadeService(UnidadeRepository unidades, UsuarioRepository usuarios, AssociacaoRepository associacoes,
                          UnidadeMapper mapper) {
        this.unidades = unidades;
        this.usuarios = usuarios;
        this.associacoes = associacoes;
        this.mapper = mapper;
    }

    @Transactional
    public UnidadeResponseDTO criarComAdministrador(UUID usuarioId, String nome) {
        var usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        String nomeParaExibicao = NormalizadorNomeUnidade.limparParaExibicao(nome);
        String nomeNormalizado = NormalizadorNomeUnidade.normalizar(nomeParaExibicao);
        if (unidades.existsByNomeNormalizado(nomeNormalizado)) {
            throw new NomeUnidadeJaExistenteException();
        }

        Unidade unidade;
        try {
            unidade = unidades.saveAndFlush(new Unidade(nomeParaExibicao, nomeNormalizado));
        } catch (DataIntegrityViolationException excecao) {
            throw new NomeUnidadeJaExistenteException();
        }
        associacoes.save(new Associacao(usuario, unidade, Perfil.ADMIN));
        return mapper.paraResponseDTO(unidade, Perfil.ADMIN);
    }
}
