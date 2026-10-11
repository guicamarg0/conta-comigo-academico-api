package br.com.gestaoservicos.orcamento.model.estado;

import br.com.gestaoservicos.orcamento.model.StatusOrcamento;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public interface EstadoOrcamento {
    default StatusOrcamento emitir()   { throw invalida(); }
    default StatusOrcamento aprovar()  { throw invalida(); }
    default StatusOrcamento recusar()  { throw invalida(); }
    default StatusOrcamento cancelar() { throw invalida(); }

    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Transição de status inválida");
    }
}