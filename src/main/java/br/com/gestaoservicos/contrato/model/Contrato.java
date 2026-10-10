package br.com.gestaoservicos.contrato.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.contratante.model.Contratante;
import br.com.gestaoservicos.empresa.model.Empresa;
import br.com.gestaoservicos.orcamento.model.Orcamento;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "contrato")
public class Contrato extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contratante_id") private Contratante contratante;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "orcamento_id") private Orcamento orcamento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id") private Empresa empresa;
    @Column(nullable = false, length = 30) private String numero;
    @Column(nullable = false, length = 120) private String modelo;
    @Column(nullable = false, length = 4000) private String objeto;
    @Column(name = "clausulas_adicionais", length = 8000) private String clausulasAdicionais;
    @Column(name = "inicio_vigencia", nullable = false) private LocalDate inicioVigencia;
    @Column(name = "fim_vigencia", nullable = false) private LocalDate fimVigencia;
    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2) private BigDecimal valorTotal;
    @Column(name = "condicoes_pagamento", length = 2000) private String condicoesPagamento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private StatusContrato status;
    @Column(name = "numero_versao", nullable = false) private int numeroVersao;
    @Column(nullable = false) private boolean ativo = true;
    @Column(name = "assinado_por", length = 120) private String assinadoPor;
    @Column(name = "assinado_em") private LocalDate assinadoEm;
    @Column(name = "canal_assinatura", length = 120) private String canalAssinatura;
    @Column(name = "evidencia_url", length = 1000) private String evidenciaUrl;
    @Column(name = "modelo_contrato_id") private UUID modeloContratoId;
    @Column(name = "conteudo_rascunho", columnDefinition = "text") private String conteudoRascunho;
    @Version @Column(name = "versao") private long versao;

    protected Contrato() {}
    public Contrato(Unidade unidade, Contratante contratante, Orcamento orcamento,
                    BigDecimal valorTotal, String condicoesPagamento, String modelo, String objeto,
                    String clausulasAdicionais, LocalDate inicioVigencia, LocalDate fimVigencia, Empresa empresa) {
        id = UUID.randomUUID();
        numero = "CT-" + id.toString().substring(0, 8).toUpperCase();
        this.unidade = unidade;
        this.contratante = contratante;
        this.orcamento = orcamento;
        this.empresa = empresa;
        this.valorTotal = valorTotal;
        this.condicoesPagamento = condicoesPagamento;
        this.status = StatusContrato.AGUARDANDO_ASSINATURA;
        this.numeroVersao = 1;
        atualizar(modelo, objeto, clausulasAdicionais, inicioVigencia, fimVigencia);
    }
    public void atualizar(String modelo, String objeto, String clausulasAdicionais,
                          LocalDate inicioVigencia, LocalDate fimVigencia) {
        this.modelo = modelo.strip();
        this.objeto = objeto.strip();
        this.clausulasAdicionais = clausulasAdicionais;
        this.inicioVigencia = inicioVigencia;
        this.fimVigencia = fimVigencia;
    }
    public boolean isAtivo() { return ativo; }
    public void avancarVersao() { numeroVersao++; }
    public void definirAtivo(boolean valor) { ativo = valor; }
    public void marcarVigente() { status = StatusContrato.VIGENTE; }
    public void encerrar() { status = StatusContrato.ENCERRADO; }
    public void atualizarRascunho(UUID modeloContratoId, String conteudoRascunho) { this.modeloContratoId = modeloContratoId; this.conteudoRascunho = conteudoRascunho == null || conteudoRascunho.isBlank() ? objeto + "\n\n" + (clausulasAdicionais == null ? "" : clausulasAdicionais) : conteudoRascunho.strip(); }
    public void definirNumero(String valor) { this.numero = valor; }
    public UUID getId() { return id; }
    public Contratante getContratante() { return contratante; }
    public Orcamento getOrcamento() { return orcamento; }
    public void atualizarEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Empresa getEmpresa() { return empresa; }
    public String getNumero() { return numero; }
    public String getModelo() { return modelo; }
    public String getObjeto() { return objeto; }
    public String getClausulasAdicionais() { return clausulasAdicionais; }
    public LocalDate getInicioVigencia() { return inicioVigencia; }
    public LocalDate getFimVigencia() { return fimVigencia; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getCondicoesPagamento() { return condicoesPagamento; }
    public StatusContrato getStatus() { return status; }
    public int getNumeroVersao() { return numeroVersao; }
    public String getAssinadoPor() { return assinadoPor; }
    public LocalDate getAssinadoEm() { return assinadoEm; }
    public String getCanalAssinatura() { return canalAssinatura; }
    public String getEvidenciaUrl() { return evidenciaUrl; }
    public UUID getModeloContratoId() { return modeloContratoId; } public String getConteudoRascunho() { return conteudoRascunho; }
}
