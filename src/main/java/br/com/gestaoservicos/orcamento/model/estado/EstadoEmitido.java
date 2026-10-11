package br.com.gestaoservicos.orcamento.model.estado;

import br.com.gestaoservicos.orcamento.model.StatusOrcamento;

public class EstadoEmitido implements EstadoOrcamento {
    @Override
    public StatusOrcamento aprovar() {
        return StatusOrcamento.APROVADO;
    }

    @Override
    public StatusOrcamento recusar() {
        return StatusOrcamento.RECUSADO;
    }

    @Override
    public StatusOrcamento cancelar() {
        return StatusOrcamento.CANCELADO;
    }
}