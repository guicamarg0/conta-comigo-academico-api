package br.com.gestaoservicos.catalogo.dto;

import br.com.gestaoservicos.catalogo.model.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemCatalogoResponseDTO(UUID id, TipoItemCatalogo tipo, String nome, String descricao,
        UnidadeMedida unidadeMedida, String unidadeMedidaPersonalizada, BigDecimal valorPadrao,
        String codigoReferencia, boolean ativo) { }
