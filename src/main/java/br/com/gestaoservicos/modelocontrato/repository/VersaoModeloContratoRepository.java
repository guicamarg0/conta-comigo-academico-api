package br.com.gestaoservicos.modelocontrato.repository;

import br.com.gestaoservicos.modelocontrato.model.VersaoModeloContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface VersaoModeloContratoRepository extends JpaRepository<VersaoModeloContrato, UUID> {
    List<VersaoModeloContrato> findByModelo_IdOrderByNumeroDesc(UUID modeloId);
}
