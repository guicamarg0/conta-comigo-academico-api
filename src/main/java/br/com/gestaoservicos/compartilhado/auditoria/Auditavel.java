package br.com.gestaoservicos.compartilhado.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.time.Instant;
import java.util.UUID;

/** Base opcional para entidades novas que precisam de metadados de auditoria. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditavel {
    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @CreatedBy
    @Column(name = "criado_por")
    private UUID criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public UUID getCriadoPor() { return criadoPor; }
    public UUID getAtualizadoPor() { return atualizadoPor; }
}
