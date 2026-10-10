package br.com.gestaoservicos.autenticacao.repository;

import br.com.gestaoservicos.autenticacao.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, UUID> {
    Optional<RecuperacaoSenha> findByTokenHash(String hash);
    long countByUsuarioIdAndCriadoEmAfter(UUID usuarioId, Instant depois);
    @Modifying
    @Query("update RecuperacaoSenha r set r.usadoEm = :agora where r.usuario.id = :usuarioId and r.usadoEm is null")
    void invalidarDoUsuario(UUID usuarioId, Instant agora);
}
