package br.com.gestaoservicos.orcamento.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MudancaValorCatalogoResponseDTO(
        UUID itemCatalogoId,
        String descricao,
        BigDecimal valorUnitarioAnterior,
        BigDecimal valorUnitarioNovo) {
}
