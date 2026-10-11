package br.com.gestaoservicos.orcamento.model.ajuste;

import java.math.BigDecimal;

public interface CalculoAjuste {
    BigDecimal calcular(BigDecimal subtotal, BigDecimal valor);
}