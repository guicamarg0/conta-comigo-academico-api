package br.com.gestaoservicos.associacao.repository;

import br.com.gestaoservicos.associacao.model.Associacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssociacaoRepository extends JpaRepository<Associacao, UUID> {
    @EntityGraph(attributePaths = "usuario")
    List<Associacao> findAllByUnidadeIdOrderByUsuarioNome(UUID unidadeId);
    Optional<Associacao> findByIdAndUnidadeId(UUID id, UUID unidadeId);
    @EntityGraph(attributePaths = "unidade")
    List<Associacao> findAllByUsuarioIdAndAtivaTrueOrderByUnidadeNome(UUID usuarioId);

    @EntityGraph(attributePaths = {"unidade", "usuario"})
    Optional<Associacao> findByUsuarioIdAndUnidadeIdAndAtivaTrue(UUID usuarioId, UUID unidadeId);

    Optional<Associacao> findByUsuarioIdAndUnidadeId(UUID usuarioId, UUID unidadeId);

    @EntityGraph(attributePaths = "usuario")
    List<Associacao> findAllByUnidadeIdAndAtivaTrueOrderByUsuarioNome(UUID unidadeId);

    Optional<Associacao> findByIdAndUnidadeIdAndAtivaTrue(UUID id, UUID unidadeId);

    boolean existsByUsuarioIdAndUnidadeIdAndAtivaTrue(UUID usuarioId, UUID unidadeId);

    long countByUnidadeIdAndPerfilAndAtivaTrue(UUID unidadeId, br.com.gestaoservicos.associacao.model.Perfil perfil);
}
