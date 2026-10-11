package br.com.gestaoservicos.orcamento.observer;

import br.com.gestaoservicos.orcamento.model.EventoOrcamento;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosOrcamento {
    private final List<ObservadorOrcamento> observadores = new ArrayList<>();

    public PublicadorEventosOrcamento(List<ObservadorOrcamento> iniciais) {
        iniciais.forEach(this::inscrever);
    }

    public void inscrever(ObservadorOrcamento observador) {
        observadores.add(observador);
    }

    public void publicar(EventoOrcamento evento) {
        for (ObservadorOrcamento observador : observadores) {
            observador.notificar(evento);
        }
    }
}