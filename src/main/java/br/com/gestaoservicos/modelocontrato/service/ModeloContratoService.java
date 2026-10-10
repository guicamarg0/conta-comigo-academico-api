package br.com.gestaoservicos.modelocontrato.service;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.modelocontrato.dto.*;
import br.com.gestaoservicos.modelocontrato.model.*;
import br.com.gestaoservicos.modelocontrato.repository.*;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class ModeloContratoService {
    private final ModeloContratoRepository modelos;
    private final VersaoModeloContratoRepository versoes;
    private final UnidadeRepository unidades;
    public ModeloContratoService(ModeloContratoRepository modelos, VersaoModeloContratoRepository versoes, UnidadeRepository unidades) {
        this.modelos = modelos; this.versoes = versoes; this.unidades = unidades;
    }
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ModeloContratoResponseDTO> listar(UUID unidadeId, StatusModeloContrato status, Pageable pagina) {
        var resultado = status == null ? modelos.findByUnidade_Id(unidadeId, pagina) : modelos.findByUnidade_IdAndStatus(unidadeId, status, pagina);
        return PaginaResponseDTO.de(resultado.map(this::resposta));
    }
    @Transactional(readOnly = true)
    public ModeloContratoResponseDTO consultar(UUID unidadeId, UUID id) { return resposta(obter(unidadeId, id)); }
    @Transactional
    public ModeloContratoResponseDTO criar(UUID unidadeId, ModeloContratoRequestDTO dto) {
        validarNome(unidadeId, dto.nome(), null);
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
        var modelo = modelos.save(new ModeloContrato(unidade, dto.nome(), dto.descricao(), dto.conteudo()));
        versoes.save(new VersaoModeloContrato(modelo));
        return resposta(modelo);
    }
    @Transactional
    public ModeloContratoResponseDTO atualizar(UUID unidadeId, UUID id, ModeloContratoRequestDTO dto) {
        var modelo = obter(unidadeId, id); validarNome(unidadeId, dto.nome(), id);
        modelo.atualizar(dto.nome(), dto.descricao(), dto.conteudo()); versoes.save(new VersaoModeloContrato(modelo));
        return resposta(modelo);
    }
    @Transactional
    public ModeloContratoResponseDTO publicar(UUID unidadeId, UUID id) { var modelo = obter(unidadeId, id); modelo.publicar(); return resposta(modelo); }
    @Transactional public ModeloContratoResponseDTO inativar(UUID unidadeId, UUID id) { var modelo = obter(unidadeId, id); modelo.inativar(); return resposta(modelo); }
    @Transactional public ModeloContratoResponseDTO reativar(UUID unidadeId, UUID id) { var modelo = obter(unidadeId, id); modelo.reativar(); return resposta(modelo); }
    @Transactional
    public ModeloContratoResponseDTO duplicar(UUID unidadeId, UUID id) {
        var origem = obter(unidadeId, id); var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var base = origem.getNome() + " (cópia)"; var nome = base; var sufixo = 2;
        while (modelos.existsByUnidade_IdAndNomeIgnoreCase(unidadeId, nome)) nome = base + " " + sufixo++;
        var copia = modelos.save(new ModeloContrato(unidade, nome, origem.getDescricao(), origem.getConteudo())); versoes.save(new VersaoModeloContrato(copia));
        return resposta(copia);
    }
    @Transactional(readOnly = true)
    public List<VersaoModeloContratoResponseDTO> versoes(UUID unidadeId, UUID id) {
        obter(unidadeId, id);
        return versoes.findByModelo_IdOrderByNumeroDesc(id).stream().map(v -> new VersaoModeloContratoResponseDTO(v.getId(), v.getNumero(), v.getConteudo(), v.getCriadoEm())).toList();
    }
    private ModeloContrato obter(UUID unidadeId, UUID id) { return modelos.findByIdAndUnidade_Id(id, unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de contrato não encontrado")); }
    private void validarNome(UUID unidadeId, String nome, UUID id) {
        modelos.findByUnidade_Id(unidadeId, Pageable.unpaged()).getContent().stream().filter(m -> !m.getId().equals(id) && m.getNome().equalsIgnoreCase(nome.strip())).findFirst()
            .ifPresent(m -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um modelo com este nome na unidade"); });
    }
    private ModeloContratoResponseDTO resposta(ModeloContrato m) { return new ModeloContratoResponseDTO(m.getId(), m.getNome(), m.getDescricao(), m.getConteudo(), m.getStatus(), m.getNumeroVersao(), m.getAtualizadoEm()); }
}
