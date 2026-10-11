package br.com.gestaoservicos.orcamento.observer;

import br.com.gestaoservicos.orcamento.model.EventoOrcamento;

public interface ObservadorOrcamento {
    void notificar(EventoOrcamento evento);
}