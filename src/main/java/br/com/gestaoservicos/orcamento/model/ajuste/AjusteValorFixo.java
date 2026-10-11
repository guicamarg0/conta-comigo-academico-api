package br.com.gestaoservicos.orcamento.model.ajuste;

import java.math.BigDecimal;

public class AjusteValorFixo implements CalculoAjuste {
    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return valor;
    }
}