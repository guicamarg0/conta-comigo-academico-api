package br.com.gestaoservicos.autenticacao.repository;

import br.com.gestaoservicos.autenticacao.model.SessaoRenovacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRenovacaoRepository extends JpaRepository<SessaoRenovacao, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoRenovacao s join fetch s.usuario where s.tokenHash = :tokenHash")
    Optional<SessaoRenovacao> buscarPorTokenHashComBloqueio(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update SessaoRenovacao s set s.revogadaEm = :agora, s.motivoRevogacao = :motivo " +
            "where s.familiaId = :familiaId and s.revogadaEm is null")
    int revogarFamilia(@Param("familiaId") UUID familiaId, @Param("agora") Instant agora,
                       @Param("motivo") String motivo);

    @Modifying
    @Query("update SessaoRenovacao s set s.revogadaEm = :agora, s.motivoRevogacao = :motivo " +
            "where s.usuario.id = :usuarioId and s.revogadaEm is null")
    int revogarDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora,
                         @Param("motivo") String motivo);
}
