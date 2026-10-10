package br.com.gestaoservicos.modelocontrato.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "modelo_contrato")
public class ModeloContrato extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @Column(nullable = false, length = 120) private String nome;
    @Column(length = 500) private String descricao;
    @Column(nullable = false, columnDefinition = "text") private String conteudo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusModeloContrato status;
    @Column(name = "numero_versao", nullable = false) private int numeroVersao;
    @Version @Column(name = "versao") private long versao;

    protected ModeloContrato() { }
    public ModeloContrato(Unidade unidade, String nome, String descricao, String conteudo) {
        this.id = UUID.randomUUID(); this.unidade = unidade; this.numeroVersao = 0; this.status = StatusModeloContrato.RASCUNHO;
        atualizar(nome, descricao, conteudo);
    }
    public void atualizar(String nome, String descricao, String conteudo) {
        this.nome = nome.strip(); this.descricao = descricao == null ? null : descricao.strip(); this.conteudo = conteudo.strip(); this.numeroVersao++;
    }
    public void publicar() { this.status = StatusModeloContrato.PUBLICADO; }
    public void inativar() { this.status = StatusModeloContrato.ARQUIVADO; }
    public void reativar() { this.status = StatusModeloContrato.PUBLICADO; }
    public UUID getId() { return id; } public String getNome() { return nome; } public String getDescricao() { return descricao; }
    public String getConteudo() { return conteudo; } public StatusModeloContrato getStatus() { return status; } public int getNumeroVersao() { return numeroVersao; }
}
