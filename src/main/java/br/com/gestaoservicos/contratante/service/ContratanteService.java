package br.com.gestaoservicos.contratante.service;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.contratante.dto.*;
import br.com.gestaoservicos.contratante.mapper.ContratanteMapper;
import br.com.gestaoservicos.contratante.model.*;
import br.com.gestaoservicos.contratante.repository.ContratanteRepository;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service public class ContratanteService {
    private final ContratanteRepository contratantes; private final UnidadeRepository unidades; private final ContratanteMapper mapper;
    public ContratanteService(ContratanteRepository contratantes, UnidadeRepository unidades, ContratanteMapper mapper) { this.contratantes = contratantes; this.unidades = unidades; this.mapper = mapper; }
    @Transactional(readOnly = true) public PaginaResponseDTO<ContratanteResponseDTO> listar(UUID unidadeId, String busca, Boolean ativo, Pageable paginacao) { String termo = limparOpcional(busca); return PaginaResponseDTO.de(contratantes.buscar(unidadeId, termo == null ? "" : termo, ativo, paginacao).map(mapper::paraResponseDTO)); }
    @Transactional(readOnly = true) public ContratanteResponseDTO consultar(UUID unidadeId, UUID id) { return mapper.paraResponseDTO(obter(unidadeId, id)); }
    @Transactional public ContratanteResponseDTO criar(UUID unidadeId, ContratanteRequestDTO dto) {
        validarTipoDocumento(dto.tipo(), dto.documento()); validarDocumentoUnico(unidadeId, dto.documento(), null);
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
        return mapper.paraResponseDTO(contratantes.save(new Contratante(unidade, dto.tipo(), limpar(dto.nomeRazaoSocial()), limparOpcional(dto.nomeFantasia()), dto.documento(), limparOpcional(dto.endereco()), limparOpcional(dto.observacaoInterna()), contatos(dto.contatos()))));
    }
    @Transactional public ContratanteResponseDTO atualizar(UUID unidadeId, UUID id, ContratanteRequestDTO dto) {
        validarTipoDocumento(dto.tipo(), dto.documento()); Contratante contratante = obter(unidadeId, id); validarDocumentoUnico(unidadeId, dto.documento(), id);
        contratante.atualizar(dto.tipo(), limpar(dto.nomeRazaoSocial()), limparOpcional(dto.nomeFantasia()), dto.documento(), limparOpcional(dto.endereco()), limparOpcional(dto.observacaoInterna()), contatos(dto.contatos()));
        return mapper.paraResponseDTO(contratante);
    }
    @Transactional public void inativar(UUID unidadeId, UUID id) { obter(unidadeId, id).inativar(); }
    @Transactional public void reativar(UUID unidadeId, UUID id) { obter(unidadeId, id).reativar(); }
    private Contratante obter(UUID unidadeId, UUID id) { return contratantes.findByIdAndUnidadeId(id, unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contratante não encontrado")); }
    private void validarDocumentoUnico(UUID unidadeId, String documento, UUID id) { boolean existe = id == null ? contratantes.existsByUnidadeIdAndDocumento(unidadeId, documento) : contratantes.existsByUnidadeIdAndDocumentoAndIdNot(unidadeId, documento, id); if (existe) throw new DocumentoContratanteJaExisteException(); }
    private void validarTipoDocumento(TipoContratante tipo, String documento) { if ((tipo == TipoContratante.PF && documento.length() != 11) || (tipo == TipoContratante.PJ && documento.length() != 14)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O documento não corresponde ao tipo de contratante"); }
    private List<ContatoContratante> contatos(List<ContatoContratanteRequestDTO> contatos) { return contatos.stream().map(dto -> {
        String telefone = limparOpcional(dto.telefone()); String email = limparOpcional(dto.email());
        if (telefone == null && email == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada contato deve informar telefone ou e-mail");
        return mapper.paraEntidade(dto, telefone, email);
    }).toList(); }
    private String limpar(String valor) { return valor.strip().replaceAll("\\s+", " "); } private String limparOpcional(String valor) { return valor == null || valor.isBlank() ? null : limpar(valor); }
}
