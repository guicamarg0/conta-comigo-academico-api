package br.com.gestaoservicos.orcamento.model;

import br.com.gestaoservicos.orcamento.model.estado.*;

public enum StatusOrcamento {
    RASCUNHO(new EstadoRascunho()),
    EMITIDO(new EstadoEmitido()),
    APROVADO(new EstadoAprovado()),
    RECUSADO(new EstadoRecusado()),
    CANCELADO(new EstadoCancelado());

    private final EstadoOrcamento estado;

    StatusOrcamento(EstadoOrcamento estado) {
        this.estado = estado;
    }

    public EstadoOrcamento estado() {
        return estado;
    }
}