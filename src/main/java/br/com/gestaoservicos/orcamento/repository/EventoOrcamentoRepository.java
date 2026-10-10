package br.com.gestaoservicos.orcamento.repository;
import br.com.gestaoservicos.orcamento.model.EventoOrcamento; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface EventoOrcamentoRepository extends JpaRepository<EventoOrcamento,UUID> { List<EventoOrcamento> findAllByOrcamentoIdOrderByOcorridoEmDesc(UUID orcamentoId); }
