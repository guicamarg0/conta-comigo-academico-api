package br.com.gestaoservicos.catalogo.model;

import java.math.BigDecimal;

public enum UnidadeMedida {
    UNIDADE, HORA, DIARIA, METRO, METRO_QUADRADO, QUILOGRAMA, LITRO, OUTRA;

    /** Regra reutilizável para itens de orçamento: hora e diária usam inteiros; as demais, até duas casas. */
    public boolean aceitaQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) return false;
        return switch (this) {
            case HORA, DIARIA -> quantidade.stripTrailingZeros().scale() <= 0;
            default -> quantidade.scale() <= 2;
        };
    }
}
