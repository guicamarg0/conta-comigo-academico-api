package br.com.gestaoservicos.contratante.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "contato_contratante")
public class ContatoContratante {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String nome;
    @Column(length = 30) private String telefone;
    @Column(length = 254) private String email;
    protected ContatoContratante() {}
    public ContatoContratante(String nome, String telefone, String email) { this.id = UUID.randomUUID(); this.nome = nome; this.telefone = telefone; this.email = email; }
    public UUID getId() { return id; } public String getNome() { return nome; } public String getTelefone() { return telefone; } public String getEmail() { return email; }
}
