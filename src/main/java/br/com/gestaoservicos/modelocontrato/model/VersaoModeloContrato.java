package br.com.gestaoservicos.modelocontrato.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "modelo_contrato_versao")
public class VersaoModeloContrato extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "modelo_contrato_id") private ModeloContrato modelo;
    @Column(nullable = false) private int numero;
    @Column(nullable = false, columnDefinition = "text") private String conteudo;
    protected VersaoModeloContrato() { }
    public VersaoModeloContrato(ModeloContrato modelo) { this.id = UUID.randomUUID(); this.modelo = modelo; this.numero = modelo.getNumeroVersao(); this.conteudo = modelo.getConteudo(); }
    public UUID getId() { return id; } public int getNumero() { return numero; } public String getConteudo() { return conteudo; }
}
