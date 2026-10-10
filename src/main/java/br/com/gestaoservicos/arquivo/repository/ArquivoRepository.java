package br.com.gestaoservicos.arquivo.repository;
import br.com.gestaoservicos.arquivo.model.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ArquivoRepository extends JpaRepository<Arquivo,UUID> { Optional<Arquivo> findByIdAndUnidadeId(UUID id,UUID unidadeId); }
