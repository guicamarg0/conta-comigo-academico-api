package br.com.gestaoservicos.catalogo.dto;

import br.com.gestaoservicos.catalogo.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ItemCatalogoRequestDTO(
        @NotNull TipoItemCatalogo tipo,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 2000) String descricao,
        @NotNull UnidadeMedida unidadeMedida,
        @Size(max = 60) String unidadeMedidaPersonalizada,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) @Digits(integer = 13, fraction = 2) BigDecimal valorPadrao,
        @Size(max = 80) String codigoReferencia) { }
