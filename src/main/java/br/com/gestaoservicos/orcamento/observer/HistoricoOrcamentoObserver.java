package br.com.gestaoservicos.orcamento.observer;

import br.com.gestaoservicos.orcamento.model.EventoOrcamento;
import br.com.gestaoservicos.orcamento.repository.EventoOrcamentoRepository;
import org.springframework.stereotype.Component;

@Component
public class HistoricoOrcamentoObserver implements ObservadorOrcamento {
    private final EventoOrcamentoRepository eventos;

    public HistoricoOrcamentoObserver(EventoOrcamentoRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    public void notificar(EventoOrcamento evento) {
        eventos.save(evento);
    }
}