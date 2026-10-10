package br.com.gestaoservicos.orcamento.repository;
import br.com.gestaoservicos.orcamento.model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface OrcamentoRepository extends JpaRepository<Orcamento,UUID> {
 Optional<Orcamento> findByIdAndUnidade_Id(UUID id,UUID unidadeId); boolean existsByUnidade_IdAndNumeroAndIdNot(UUID unidadeId,String numero,UUID id); boolean existsByUnidade_IdAndNumero(UUID unidadeId,String numero);
 @Query("select o from Orcamento o where o.unidade.id=:unidadeId and lower(o.numero) like lower(concat('%',:busca,'%')) and (:status is null or o.status=:status)") Page<Orcamento> buscar(UUID unidadeId,String busca,StatusOrcamento status,Pageable pageable);
}
