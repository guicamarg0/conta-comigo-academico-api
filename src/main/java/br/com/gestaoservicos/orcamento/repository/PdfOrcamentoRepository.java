package br.com.gestaoservicos.orcamento.repository;

import br.com.gestaoservicos.orcamento.model.PdfOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PdfOrcamentoRepository extends JpaRepository<PdfOrcamento, UUID> {
    Optional<PdfOrcamento> findByRevisaoId(UUID revisaoId);
}
