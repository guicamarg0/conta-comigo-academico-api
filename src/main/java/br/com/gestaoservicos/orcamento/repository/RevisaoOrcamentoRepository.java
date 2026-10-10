package br.com.gestaoservicos.orcamento.repository;

import br.com.gestaoservicos.orcamento.model.RevisaoOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RevisaoOrcamentoRepository extends JpaRepository<RevisaoOrcamento,UUID> {
    Optional<RevisaoOrcamento> findByOrcamentoIdAndNumeroRevisao(UUID orcamentoId,int numeroRevisao);
    List<RevisaoOrcamento> findAllByOrcamentoIdOrderByNumeroRevisaoDesc(UUID orcamentoId);
}
