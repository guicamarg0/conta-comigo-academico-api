package br.com.gestaoservicos.orcamento.observer;

import br.com.gestaoservicos.orcamento.model.EventoOrcamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogOrcamentoObserver implements ObservadorOrcamento {
    private static final Logger log = LoggerFactory.getLogger(LogOrcamentoObserver.class);

    @Override
    public void notificar(EventoOrcamento evento) {
        log.info("Orçamento {}: {}", evento.getRevisao().getOrcamento().getNumero(), evento.getTipo());
    }
}