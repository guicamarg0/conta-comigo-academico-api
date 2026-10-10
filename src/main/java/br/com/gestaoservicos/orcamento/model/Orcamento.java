package br.com.gestaoservicos.orcamento.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "orcamento")
public class Orcamento extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @Column(nullable = false, length = 30) private String numero;
    @Column(name = "revisao_atual", nullable = false) private int revisaoAtual;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusOrcamento status;
    @Column(name = "emitido_inicialmente", nullable = false) private boolean emitidoInicialmente;
    @Version @Column(name = "versao") private long versao;
    protected Orcamento() { }
    public Orcamento(Unidade unidade, String numero) { this.id = UUID.randomUUID(); this.unidade = unidade; this.numero = numero; this.revisaoAtual = 1; this.status = StatusOrcamento.RASCUNHO; }
    public void atualizarNumero(String numero) { this.numero = numero; }
    public void atualizarStatus(StatusOrcamento status) { this.status = status; if (status == StatusOrcamento.EMITIDO) this.emitidoInicialmente = true; }
    public void avancarRevisao() { revisaoAtual++; status = StatusOrcamento.RASCUNHO; }
    public UUID getUnidadeId() { return unidade.getId(); }
    public UUID getId() { return id; } public String getNumero() { return numero; } public int getRevisaoAtual() { return revisaoAtual; } public StatusOrcamento getStatus() { return status; } public boolean isEmitidoInicialmente() { return emitidoInicialmente; }
}
