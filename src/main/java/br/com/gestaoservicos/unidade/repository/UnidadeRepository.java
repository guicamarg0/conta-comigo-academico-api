package br.com.gestaoservicos.unidade.repository;

import br.com.gestaoservicos.unidade.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UnidadeRepository extends JpaRepository<Unidade, UUID> {
    boolean existsByNomeNormalizado(String nomeNormalizado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select unidade from Unidade unidade where unidade.id = :id")
    Optional<Unidade> findByIdComBloqueio(UUID id);
}
