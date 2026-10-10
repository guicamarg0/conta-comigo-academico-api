package br.com.gestaoservicos.associacao.model;

import br.com.gestaoservicos.unidade.model.Unidade;
import br.com.gestaoservicos.usuario.model.Usuario;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "associacao", uniqueConstraints = @UniqueConstraint(name = "uk_associacao_usuario_unidade", columnNames = {"usuario_id", "unidade_id"}))
public class Associacao {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;
    @Column(nullable = false)
    private boolean ativa;

    protected Associacao() {}

    public Associacao(Usuario usuario, Unidade unidade, Perfil perfil) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.unidade = unidade;
        this.perfil = perfil;
        this.criadaEm = Instant.now();
        this.ativa = true;
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Unidade getUnidade() { return unidade; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtiva() { return ativa; }
    public void alterarPerfil(Perfil perfil) { this.perfil = perfil; }
    public void desativar() { this.ativa = false; }
    public void reativar(Perfil perfil) { this.ativa = true; this.perfil = perfil; }
}
