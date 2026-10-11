package br.com.gestaoservicos.orcamento.model.ajuste;

import java.math.BigDecimal;

public class AjustePercentual implements CalculoAjuste {
    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return subtotal.multiply(valor).divide(new BigDecimal("100"));
    }
}