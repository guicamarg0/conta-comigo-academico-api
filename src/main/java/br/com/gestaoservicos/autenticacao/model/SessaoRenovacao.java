package br.com.gestaoservicos.autenticacao.model;

import br.com.gestaoservicos.usuario.model.Usuario;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao_renovacao")
public class SessaoRenovacao {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "versao_sessao", nullable = false)
    private int versaoSessao;
    @Column(nullable = false)
    private boolean persistente;
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;
    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;
    @Column(name = "rotacionada_em")
    private Instant rotacionadaEm;
    @Column(name = "substituida_por_id")
    private UUID substituidaPorId;
    @Column(name = "revogada_em")
    private Instant revogadaEm;
    @Column(name = "motivo_revogacao", length = 40)
    private String motivoRevogacao;

    protected SessaoRenovacao() {}

    public SessaoRenovacao(Usuario usuario, UUID familiaId, String tokenHash, int versaoSessao,
                           boolean persistente, Instant expiraEm) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.familiaId = familiaId;
        this.tokenHash = tokenHash;
        this.versaoSessao = versaoSessao;
        this.persistente = persistente;
        this.criadaEm = Instant.now();
        this.expiraEm = expiraEm;
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public UUID getFamiliaId() { return familiaId; }
    public int getVersaoSessao() { return versaoSessao; }
    public boolean isPersistente() { return persistente; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean foiRotacionada() { return substituidaPorId != null; }
    public boolean podeRenovar(Instant agora) { return revogadaEm == null && expiraEm.isAfter(agora); }
    public void marcarRotacionada(UUID novaSessaoId, Instant agora) {
        substituidaPorId = novaSessaoId;
        rotacionadaEm = agora;
        revogadaEm = agora;
        motivoRevogacao = "ROTACIONADA";
    }
}
