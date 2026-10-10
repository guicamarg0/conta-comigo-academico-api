package br.com.gestaoservicos.catalogo.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_catalogo")
public class ItemCatalogo extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id", nullable = false) private Unidade unidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoItemCatalogo tipo;
    @Column(nullable = false, length = 120) private String nome;
    @Column(length = 2000) private String descricao;
    @Enumerated(EnumType.STRING) @Column(name = "unidade_medida", nullable = false, length = 30) private UnidadeMedida unidadeMedida;
    @Column(name = "unidade_medida_personalizada", length = 60) private String unidadeMedidaPersonalizada;
    @Column(name = "valor_padrao", nullable = false, precision = 15, scale = 2) private BigDecimal valorPadrao;
    @Column(name = "codigo_referencia", length = 80) private String codigoReferencia;
    @Column(nullable = false) private boolean ativo;
    protected ItemCatalogo() { }
    public ItemCatalogo(Unidade unidade, TipoItemCatalogo tipo, String nome, String descricao, UnidadeMedida unidadeMedida, String unidadeMedidaPersonalizada, BigDecimal valorPadrao, String codigoReferencia) {
        this.id = UUID.randomUUID(); this.unidade = unidade; this.ativo = true;
        atualizar(tipo, nome, descricao, unidadeMedida, unidadeMedidaPersonalizada, valorPadrao, codigoReferencia);
    }
    public void atualizar(TipoItemCatalogo tipo, String nome, String descricao, UnidadeMedida unidadeMedida, String unidadeMedidaPersonalizada, BigDecimal valorPadrao, String codigoReferencia) {
        this.tipo = tipo; this.nome = nome; this.descricao = descricao; this.unidadeMedida = unidadeMedida; this.unidadeMedidaPersonalizada = unidadeMedidaPersonalizada; this.valorPadrao = valorPadrao; this.codigoReferencia = codigoReferencia;
    }
    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }
    public UUID getId() { return id; } public TipoItemCatalogo getTipo() { return tipo; } public String getNome() { return nome; } public String getDescricao() { return descricao; } public UnidadeMedida getUnidadeMedida() { return unidadeMedida; } public String getUnidadeMedidaPersonalizada() { return unidadeMedidaPersonalizada; } public BigDecimal getValorPadrao() { return valorPadrao; } public String getCodigoReferencia() { return codigoReferencia; } public boolean isAtivo() { return ativo; }
}
