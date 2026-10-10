package br.com.gestaoservicos.modelocontrato.repository;

import br.com.gestaoservicos.modelocontrato.model.ModeloContrato;
import br.com.gestaoservicos.modelocontrato.model.StatusModeloContrato;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ModeloContratoRepository extends JpaRepository<ModeloContrato, UUID> {
    Optional<ModeloContrato> findByIdAndUnidade_Id(UUID id, UUID unidadeId);
    boolean existsByUnidade_IdAndNomeIgnoreCase(UUID unidadeId, String nome);
    Page<ModeloContrato> findByUnidade_IdAndStatus(UUID unidadeId, StatusModeloContrato status, Pageable pagina);
    Page<ModeloContrato> findByUnidade_Id(UUID unidadeId, Pageable pagina);
}
