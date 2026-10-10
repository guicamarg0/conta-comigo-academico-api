package br.com.gestaoservicos.autenticacao.model;

import br.com.gestaoservicos.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recuperacao_senha")
public class RecuperacaoSenha {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "usado_em") private Instant usadoEm;
    protected RecuperacaoSenha() {}
    public RecuperacaoSenha(Usuario usuario, String tokenHash, Instant agora) {
        this.id = UUID.randomUUID(); this.usuario = usuario; this.tokenHash = tokenHash;
        this.criadoEm = agora; this.expiraEm = agora.plusSeconds(1800);
    }
    public Usuario getUsuario() { return usuario; }
    public boolean valida(Instant agora) { return usadoEm == null && expiraEm.isAfter(agora); }
    public void consumir(Instant agora) { usadoEm = agora; }
}
