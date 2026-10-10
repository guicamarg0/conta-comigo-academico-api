package br.com.gestaoservicos.compartilhado.auditoria;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
class ConfiguracaoAuditoria {
    @Bean
    AuditorAware<UUID> auditorAtual() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> authentication.isAuthenticated())
                .flatMap(authentication -> converterId(authentication.getName()));
    }

    private Optional<UUID> converterId(String identificador) {
        try {
            return Optional.of(UUID.fromString(identificador));
        } catch (IllegalArgumentException excecao) {
            return Optional.empty();
        }
    }
}
