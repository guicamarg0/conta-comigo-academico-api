package br.com.gestaoservicos.orcamento.dto; import br.com.gestaoservicos.catalogo.model.*; import java.math.*; import java.util.*;
public record ItemOrcamentoResponseDTO(UUID itemCatalogoId, TipoItemCatalogo tipo, String descricao, UnidadeMedida unidadeMedida, String unidadeMedidaPersonalizada, BigDecimal quantidade, BigDecimal valorUnitario, BigDecimal desconto, BigDecimal total) { }
