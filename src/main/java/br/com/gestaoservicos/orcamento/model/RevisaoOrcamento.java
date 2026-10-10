package br.com.gestaoservicos.orcamento.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.contratante.model.Contratante;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity @Table(name = "revisao_orcamento")
public class RevisaoOrcamento extends Auditavel {
    @Id private UUID id; @Column(name="unidade_id", nullable=false) private UUID unidadeId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "orcamento_id") private Orcamento orcamento;
    @Column(name = "numero_revisao", nullable = false) private int numeroRevisao;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusOrcamento status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contratante_id") private Contratante contratante;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="empresa_id") private br.com.gestaoservicos.empresa.model.Empresa empresa;
    public br.com.gestaoservicos.empresa.model.Empresa getEmpresa(){return empresa;}
    public void definirEmpresa(br.com.gestaoservicos.empresa.model.Empresa empresa){this.empresa=empresa;}
    private LocalDate validade;
    @Column(name = "condicoes_pagamento") private String condicoesPagamento;
    @Column(name = "observacoes_comerciais") private String observacoesComerciais;
    @Column(name="exibir_pagamento",nullable=false) private boolean exibirPagamento=true;
    @Column(length=4000) private String referencia;
    public boolean isExibirPagamento(){return exibirPagamento;}
    public String getReferencia(){return referencia;}
    public void definirOpcoesPdf(boolean pagamento,String referencia){this.exibirPagamento=pagamento;this.referencia=referencia;}
    @Column(name = "exibir_assinatura", nullable = false) private boolean exibirAssinatura;
    @Enumerated(EnumType.STRING) @Column(name = "desconto_tipo", nullable = false) private TipoAjuste descontoTipo;
    @Column(name = "desconto_valor", nullable = false, precision = 15, scale = 2) private BigDecimal descontoValor;
    @Enumerated(EnumType.STRING) @Column(name = "acrescimo_tipo", nullable = false) private TipoAjuste acrescimoTipo;
    @Column(name = "acrescimo_valor", nullable = false, precision = 15, scale = 2) private BigDecimal acrescimoValor;
    @Column(name = "subtotal_servicos", nullable = false, precision = 15, scale = 2) private BigDecimal subtotalServicos = BigDecimal.ZERO;
    @Column(name = "subtotal_materiais", nullable = false, precision = 15, scale = 2) private BigDecimal subtotalMateriais = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "desconto_total", nullable = false, precision = 15, scale = 2) private BigDecimal descontoTotal = BigDecimal.ZERO;
    @Column(name = "acrescimo_total", nullable = false, precision = 15, scale = 2) private BigDecimal acrescimoTotal = BigDecimal.ZERO;
    @Column(name = "total_final", nullable = false, precision = 15, scale = 2) private BigDecimal totalFinal = BigDecimal.ZERO;
    @Column(name = "contratante_snapshot", length = 8000) private String contratanteSnapshot;
    @Column(name = "contratado_snapshot", columnDefinition="text") private String contratadoSnapshot;
    @Column(name = "pagamentos_snapshot", length = 8000) private String pagamentosSnapshot;
    @Version @Column(name = "versao") private long versao;
    @OneToMany(mappedBy = "revisao", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("ordem") private List<ItemRevisaoOrcamento> itens = new ArrayList<>();
    protected RevisaoOrcamento() { }
    public RevisaoOrcamento(Orcamento orcamento, int numero) { this.id=UUID.randomUUID(); this.orcamento=orcamento; this.unidadeId=orcamento.getUnidadeId(); this.numeroRevisao=numero; this.status=StatusOrcamento.RASCUNHO; this.descontoTipo=TipoAjuste.VALOR; this.acrescimoTipo=TipoAjuste.VALOR; this.descontoValor=BigDecimal.ZERO; this.acrescimoValor=BigDecimal.ZERO; }
    public void atualizar(Contratante contratante, LocalDate validade, String condicoes, String observacoes, boolean assinatura, TipoAjuste descontoTipo, BigDecimal descontoValor, TipoAjuste acrescimoTipo, BigDecimal acrescimoValor, List<ItemRevisaoOrcamento> novosItens) {
        this.contratante=contratante; this.validade=validade; this.condicoesPagamento=condicoes; this.observacoesComerciais=observacoes; this.exibirAssinatura=assinatura; this.descontoTipo=descontoTipo; this.descontoValor=descontoValor; this.acrescimoTipo=acrescimoTipo; this.acrescimoValor=acrescimoValor; itens.clear(); itens.addAll(novosItens); recalcular(); }
    public void atualizarStatus(StatusOrcamento status) { this.status=status; }
    public void registrarSnapshots(String contratante, String contratado, String pagamentos) { this.contratanteSnapshot=contratante; this.contratadoSnapshot=contratado; this.pagamentosSnapshot=pagamentos; }
    public void removerItens() { itens.clear(); }
    public void recalcular() { subtotalServicos = somaPorTipo(true); subtotalMateriais=somaPorTipo(false); subtotal=subtotalServicos.add(subtotalMateriais); descontoTotal=calcularAjuste(descontoTipo, descontoValor); acrescimoTotal=calcularAjuste(acrescimoTipo, acrescimoValor); totalFinal=subtotal.subtract(descontoTotal).add(acrescimoTotal); }
    private BigDecimal somaPorTipo(boolean servicos) { return itens.stream().filter(i -> servicos ? i.getTipo().name().equals("MAO_DE_OBRA") : !i.getTipo().name().equals("MAO_DE_OBRA")).map(ItemRevisaoOrcamento::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private BigDecimal calcularAjuste(TipoAjuste tipo, BigDecimal valor) { return (tipo == TipoAjuste.PERCENTUAL ? subtotal.multiply(valor).divide(new BigDecimal("100")) : valor).setScale(2, java.math.RoundingMode.HALF_UP); }
    public UUID getUnidadeId(){return unidadeId;} public UUID getId(){return id;} public Orcamento getOrcamento(){return orcamento;} public int getNumeroRevisao(){return numeroRevisao;} public StatusOrcamento getStatus(){return status;} public Contratante getContratante(){return contratante;} public LocalDate getValidade(){return validade;} public String getCondicoesPagamento(){return condicoesPagamento;} public String getObservacoesComerciais(){return observacoesComerciais;} public boolean isExibirAssinatura(){return exibirAssinatura;} public TipoAjuste getDescontoTipo(){return descontoTipo;} public BigDecimal getDescontoValor(){return descontoValor;} public TipoAjuste getAcrescimoTipo(){return acrescimoTipo;} public BigDecimal getAcrescimoValor(){return acrescimoValor;} public BigDecimal getSubtotalServicos(){return subtotalServicos;} public BigDecimal getSubtotalMateriais(){return subtotalMateriais;} public BigDecimal getSubtotal(){return subtotal;} public BigDecimal getDescontoTotal(){return descontoTotal;} public BigDecimal getAcrescimoTotal(){return acrescimoTotal;} public BigDecimal getTotalFinal(){return totalFinal;} public String getContratanteSnapshot(){return contratanteSnapshot;} public String getContratadoSnapshot(){return contratadoSnapshot;} public String getPagamentosSnapshot(){return pagamentosSnapshot;} public List<ItemRevisaoOrcamento> getItens(){return List.copyOf(itens);}
}
