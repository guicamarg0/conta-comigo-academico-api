package br.com.gestaoservicos.contrato.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "atividade_contrato")
public class AtividadeContrato {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contrato_id") private Contrato contrato;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TipoAtividadeContrato tipo;
    @Column(nullable = false, length = 500) private String descricao;
    @Column(name = "usuario_id") private UUID usuarioId;
    @Column(name = "ocorrido_em", nullable = false) private Instant ocorridoEm;
    protected AtividadeContrato() {}
    public AtividadeContrato(Contrato contrato, TipoAtividadeContrato tipo, String descricao, UUID usuarioId) {
        this.id = UUID.randomUUID(); this.contrato = contrato; this.tipo = tipo; this.descricao = descricao;
        this.usuarioId = usuarioId; this.ocorridoEm = Instant.now();
    }
    public UUID getId() { return id; }
    public TipoAtividadeContrato getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public UUID getUsuarioId() { return usuarioId; }
    public Instant getOcorridoEm() { return ocorridoEm; }
}
