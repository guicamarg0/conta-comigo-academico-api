package br.com.gestaoservicos.configuracaounidade.repository;

import br.com.gestaoservicos.configuracaounidade.model.ConfiguracaoUnidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoUnidadeRepository extends JpaRepository<ConfiguracaoUnidade, UUID> {
    Optional<ConfiguracaoUnidade> findByUnidadeId(UUID unidadeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select configuracao from ConfiguracaoUnidade configuracao where configuracao.unidade.id = :unidadeId")
    Optional<ConfiguracaoUnidade> findComBloqueioByUnidadeId(UUID unidadeId);
}
