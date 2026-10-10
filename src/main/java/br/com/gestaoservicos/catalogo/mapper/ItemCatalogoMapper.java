package br.com.gestaoservicos.catalogo.mapper;

import br.com.gestaoservicos.catalogo.dto.ItemCatalogoResponseDTO;
import br.com.gestaoservicos.catalogo.model.ItemCatalogo;
import org.springframework.stereotype.Component;

@Component
public class ItemCatalogoMapper {
    public ItemCatalogoResponseDTO paraResponseDTO(ItemCatalogo item) {
        return new ItemCatalogoResponseDTO(item.getId(), item.getTipo(), item.getNome(), item.getDescricao(), item.getUnidadeMedida(), item.getUnidadeMedidaPersonalizada(), item.getValorPadrao(), item.getCodigoReferencia(), item.isAtivo());
    }
}
