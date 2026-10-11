package br.com.gestaoservicos.orcamento.model;

import br.com.gestaoservicos.orcamento.model.ajuste.*;

public enum TipoAjuste {
    VALOR(new AjusteValorFixo()),
    PERCENTUAL(new AjustePercentual());

    private final CalculoAjuste estrategia;

    TipoAjuste(CalculoAjuste estrategia) {
        this.estrategia = estrategia;
    }

    public CalculoAjuste estrategia() {
        return estrategia;
    }
}