package br.com.gestaoservicos.orcamento.dto;
import br.com.gestaoservicos.catalogo.model.*; import jakarta.validation.constraints.*; import java.math.*; import java.util.*;
public record ItemOrcamentoRequestDTO(UUID itemCatalogoId, TipoItemCatalogo tipo, @Size(max=2000) String descricao, UnidadeMedida unidadeMedida, @Size(max=60) String unidadeMedidaPersonalizada, @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=13,fraction=2) BigDecimal quantidade, @DecimalMin(value="0") @Digits(integer=13,fraction=2) BigDecimal valorUnitario) { }
