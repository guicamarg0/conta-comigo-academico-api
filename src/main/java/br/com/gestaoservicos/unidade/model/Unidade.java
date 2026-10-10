package br.com.gestaoservicos.unidade.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unidade")
public class Unidade {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(name = "nome_normalizado", nullable = false, length = 120)
    private String nomeNormalizado;
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Unidade() {}

    public Unidade(String nome, String nomeNormalizado) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.criadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
}
