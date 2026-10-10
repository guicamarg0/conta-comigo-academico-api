package br.com.gestaoservicos.convite.repository;
import br.com.gestaoservicos.convite.model.ConviteUnidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ConviteUnidadeRepository extends JpaRepository<ConviteUnidade, UUID> {
 @EntityGraph(attributePaths="unidade") List<ConviteUnidade> findAllByUnidadeIdOrderByExpiraEmDesc(UUID unidadeId);
 @EntityGraph(attributePaths="unidade") Optional<ConviteUnidade> findByTokenHash(String tokenHash);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="unidade") @Query("select convite from ConviteUnidade convite where convite.tokenHash = :tokenHash") Optional<ConviteUnidade> buscarPorTokenHashComBloqueio(@Param("tokenHash") String tokenHash);
 Optional<ConviteUnidade> findByIdAndUnidadeId(UUID id, UUID unidadeId);
}
