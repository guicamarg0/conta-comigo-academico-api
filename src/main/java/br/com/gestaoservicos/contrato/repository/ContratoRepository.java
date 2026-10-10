package br.com.gestaoservicos.contrato.repository;

import br.com.gestaoservicos.contrato.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ContratoRepository extends JpaRepository<Contrato, UUID> {
    Optional<Contrato> findByIdAndUnidade_Id(UUID id, UUID unidadeId);
    boolean existsByOrcamento_Id(UUID orcamentoId);
    @Query("select c from Contrato c join c.contratante t left join c.empresa e where c.unidade.id = :unidadeId " +
           "and (:ativo is null or c.ativo = :ativo) and (:status is null or c.status = :status) and (lower(c.numero) like lower(concat('%',:busca,'%')) " +
           "or lower(e.razaoSocial) like lower(concat('%',:busca,'%')) or lower(e.nomeFantasia) like lower(concat('%',:busca,'%')) or lower(c.modelo) like lower(concat('%',:busca,'%')) or lower(c.orcamento.numero) like lower(concat('%',:busca,'%')) or lower(t.nomeRazaoSocial) like lower(concat('%',:busca,'%')))")
    Page<Contrato> buscar(UUID unidadeId, String busca, StatusContrato status, Boolean ativo, Pageable pagina);
}
