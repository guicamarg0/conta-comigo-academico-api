package br.com.gestaoservicos.contrato.repository;

import br.com.gestaoservicos.contrato.model.ContratoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ContratoSnapshotRepository extends JpaRepository<ContratoSnapshot, UUID> {
    List<ContratoSnapshot> findByContrato_IdOrderByNumeroVersaoDesc(UUID contratoId);
    Optional<ContratoSnapshot> findByIdAndContrato_Id(UUID id, UUID contratoId);
    org.springframework.data.domain.Page<ContratoSnapshot> findByContrato_IdOrderByNumeroVersaoDesc(UUID contratoId, org.springframework.data.domain.Pageable pagina);
}
