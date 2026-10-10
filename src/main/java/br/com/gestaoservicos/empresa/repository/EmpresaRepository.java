package br.com.gestaoservicos.empresa.repository;
import br.com.gestaoservicos.empresa.model.Empresa; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> { List<Empresa> findByUnidadeIdOrderByRazaoSocial(UUID unidadeId); Optional<Empresa> findByIdAndUnidadeId(UUID id, UUID unidadeId); boolean existsByUnidadeIdAndCnpjAndIdNot(UUID unidadeId,String cnpj,UUID id); boolean existsByUnidadeIdAndCnpj(UUID unidadeId,String cnpj); }
