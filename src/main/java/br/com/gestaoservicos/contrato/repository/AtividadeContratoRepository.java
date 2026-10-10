package br.com.gestaoservicos.contrato.repository;
import br.com.gestaoservicos.contrato.model.AtividadeContrato;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AtividadeContratoRepository extends JpaRepository<AtividadeContrato, UUID> {
    Page<AtividadeContrato> findByContrato_IdOrderByOcorridoEmDescIdDesc(UUID contratoId, Pageable pagina);
}
