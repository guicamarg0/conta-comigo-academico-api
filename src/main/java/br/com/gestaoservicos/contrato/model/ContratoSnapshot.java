package br.com.gestaoservicos.contrato.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "contrato_snapshot")
public class ContratoSnapshot extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contrato_id") private Contrato contrato;
    @Column(name = "numero_versao", nullable = false) private int numeroVersao;
    @Column(nullable = false, columnDefinition = "text") private String conteudo;
    protected ContratoSnapshot() { }
    public ContratoSnapshot(Contrato contrato) { this.id = UUID.randomUUID(); this.contrato = contrato; this.numeroVersao = contrato.getNumeroVersao(); this.conteudo = contrato.getConteudoRascunho(); }
    public UUID getId() { return id; } public int getNumeroVersao() { return numeroVersao; } public String getConteudo() { return conteudo; }
}
