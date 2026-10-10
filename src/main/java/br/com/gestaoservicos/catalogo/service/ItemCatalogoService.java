package br.com.gestaoservicos.catalogo.service;

import br.com.gestaoservicos.catalogo.dto.*;
import br.com.gestaoservicos.catalogo.mapper.ItemCatalogoMapper;
import br.com.gestaoservicos.catalogo.model.*;
import br.com.gestaoservicos.catalogo.repository.ItemCatalogoRepository;
import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class ItemCatalogoService {
    private final ItemCatalogoRepository itens; private final UnidadeRepository unidades; private final ItemCatalogoMapper mapper;
    private final br.com.gestaoservicos.compartilhado.SequenciaOperacional sequencia;
    private final br.com.gestaoservicos.configuracaounidade.service.CodigosUnidadeService codigos;
    public ItemCatalogoService(ItemCatalogoRepository itens, UnidadeRepository unidades, ItemCatalogoMapper mapper, br.com.gestaoservicos.compartilhado.SequenciaOperacional sequencia, br.com.gestaoservicos.configuracaounidade.service.CodigosUnidadeService codigos) { this.itens = itens; this.unidades = unidades; this.mapper = mapper; this.sequencia=sequencia; this.codigos=codigos; }
    @Transactional(readOnly = true) public PaginaResponseDTO<ItemCatalogoResponseDTO> listar(UUID unidadeId, String busca, TipoItemCatalogo tipo, Boolean ativo, Pageable paginacao) { String termo = limparOpcional(busca); return PaginaResponseDTO.de(itens.buscar(unidadeId, termo == null ? "" : termo, tipo, ativo, paginacao).map(mapper::paraResponseDTO)); }
    @Transactional(readOnly = true) public ItemCatalogoResponseDTO consultar(UUID unidadeId, UUID id) { return mapper.paraResponseDTO(obter(unidadeId, id)); }
    @Transactional public ItemCatalogoResponseDTO criar(UUID unidadeId, ItemCatalogoRequestDTO dto) {
        validarUnidadePersonalizada(dto.unidadeMedida(), dto.unidadeMedidaPersonalizada());
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
        String codigo=limparOpcional(dto.codigoReferencia());
        if(codigo==null) codigo=proximoCodigo(unidadeId);
        return mapper.paraResponseDTO(itens.save(new ItemCatalogo(unidade, dto.tipo(), limpar(dto.nome()), limparOpcional(dto.descricao()), dto.unidadeMedida(), limparOpcional(dto.unidadeMedidaPersonalizada()), dto.valorPadrao(), codigo)));
    }
    @Transactional public ItemCatalogoResponseDTO atualizar(UUID unidadeId, UUID id, ItemCatalogoRequestDTO dto) {
        validarUnidadePersonalizada(dto.unidadeMedida(), dto.unidadeMedidaPersonalizada());
        ItemCatalogo item = obter(unidadeId, id); item.atualizar(dto.tipo(), limpar(dto.nome()), limparOpcional(dto.descricao()), dto.unidadeMedida(), limparOpcional(dto.unidadeMedidaPersonalizada()), dto.valorPadrao(), limparOpcional(dto.codigoReferencia()) == null ? item.getCodigoReferencia() : limparOpcional(dto.codigoReferencia()));
        return mapper.paraResponseDTO(item);
    }
    @Transactional public void inativar(UUID unidadeId, UUID id) { obter(unidadeId, id).inativar(); }
    @Transactional public void reativar(UUID unidadeId, UUID id) { obter(unidadeId, id).reativar(); }
    /** Ponto de entrada para composição de novos orçamentos: históricos continuam referenciando itens inativos, mas novos usos são recusados. */
    @Transactional(readOnly = true) public ItemCatalogo obterAtivoParaNovoOrcamento(UUID unidadeId, UUID id) {
        ItemCatalogo item = obter(unidadeId, id);
        if (!item.isAtivo()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Item de catálogo está inativo");
        return item;
    }
    private String proximoCodigo(UUID unidadeId) {
        String codigo;
        do { codigo=codigos.formatar(unidadeId,"PC",Long.parseLong(sequencia.proximo(unidadeId,"PC").substring(3))); }
        while(itens.existsByUnidadeIdAndCodigoReferencia(unidadeId,codigo));
        return codigo;
    }
    private ItemCatalogo obter(UUID unidadeId, UUID id) { return itens.findByIdAndUnidadeId(id, unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de catálogo não encontrado")); }
    private void validarUnidadePersonalizada(UnidadeMedida unidade, String personalizada) {
        boolean informada = personalizada != null && !personalizada.isBlank();
        if ((unidade == UnidadeMedida.OUTRA) != informada) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unidade == UnidadeMedida.OUTRA ? "Informe a unidade de medida personalizada" : "Unidade de medida personalizada só é permitida para OUTRA");
    }
    private String limpar(String valor) { return valor.strip().replaceAll("\\s+", " "); }
    private String limparOpcional(String valor) { return valor == null || valor.isBlank() ? null : limpar(valor); }
}
