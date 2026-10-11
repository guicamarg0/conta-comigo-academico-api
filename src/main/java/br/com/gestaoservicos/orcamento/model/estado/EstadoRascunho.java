package br.com.gestaoservicos.orcamento.model.estado;

import br.com.gestaoservicos.orcamento.model.StatusOrcamento;

public class EstadoRascunho implements EstadoOrcamento {
    @Override
    public StatusOrcamento emitir() {
        return StatusOrcamento.EMITIDO;
    }

    @Override
    public StatusOrcamento cancelar() {
        return StatusOrcamento.CANCELADO;
    }
}