package br.com.gestaoservicos.configuracaounidade.repository;

import br.com.gestaoservicos.configuracaounidade.model.FormaRecebimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FormaRecebimentoRepository extends JpaRepository<FormaRecebimento, UUID> {
    List<FormaRecebimento> findAllByUnidadeIdOrderByNomeExibicao(UUID unidadeId);
    void deleteAllByUnidadeId(UUID unidadeId);
    List<FormaRecebimento> findAllByConfiguracaoIdOrderByNomeExibicao(UUID configuracaoId);
    void deleteAllByConfiguracaoId(UUID configuracaoId);
}
