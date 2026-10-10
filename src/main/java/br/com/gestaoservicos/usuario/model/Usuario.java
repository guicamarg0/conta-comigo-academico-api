package br.com.gestaoservicos.usuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 200)
    private String senhaHash;
    @Column(name = "versao_sessao", nullable = false)
    private int versaoSessao;
    @Column(name = "administrador_geral", nullable = false)
    private boolean administradorGeral;
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Usuario() {}

    public Usuario(String nome, String email, String senhaHash) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public int getVersaoSessao() { return versaoSessao; }
    public boolean isAdministradorGeral() { return administradorGeral; }
    public void habilitarAdministradorGeral() { administradorGeral = true; versaoSessao++; }
    public void atualizarCadastro(String nome, String email) {
        this.nome = nome;
        if (!this.email.equals(email)) versaoSessao++;
        this.email = email;
    }
    public void redefinirSenha(String hash) { senhaHash = hash; versaoSessao++; }
}
