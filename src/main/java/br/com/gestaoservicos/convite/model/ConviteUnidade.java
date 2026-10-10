package br.com.gestaoservicos.convite.model;

import br.com.gestaoservicos.associacao.model.Perfil;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convite_unidade")
public class ConviteUnidade {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Perfil perfil;
    @Column(name = "token_hash", nullable = false, unique = true) private String tokenHash;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "aceita_em") private Instant aceitaEm;
    @Column(name = "revogada_em") private Instant revogadaEm;
    @Column(name = "criada_em", nullable = false) private Instant criadaEm;

    protected ConviteUnidade() {}
    public ConviteUnidade(Unidade unidade, Perfil perfil, String tokenHash, Instant expiraEm) {
        this.id = UUID.randomUUID(); this.unidade = unidade; this.perfil = perfil;
        this.tokenHash = tokenHash; this.expiraEm = expiraEm; this.criadaEm = Instant.now();
    }
    public UUID getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Perfil getPerfil() { return perfil; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean estaDisponivel() { return aceitaEm == null && revogadaEm == null && expiraEm.isAfter(Instant.now()); }
    public void aceitar() { aceitaEm = Instant.now(); }
    public void revogar() { revogadaEm = Instant.now(); }
}
