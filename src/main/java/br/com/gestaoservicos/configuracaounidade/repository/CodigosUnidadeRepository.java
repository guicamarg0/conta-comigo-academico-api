package br.com.gestaoservicos.configuracaounidade.repository;
import br.com.gestaoservicos.configuracaounidade.model.CodigosUnidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CodigosUnidadeRepository extends JpaRepository<CodigosUnidade,UUID> {}
