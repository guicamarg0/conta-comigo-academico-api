package br.com.gestaoservicos.servico.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.contratante.model.Contratante;
import br.com.gestaoservicos.orcamento.model.Orcamento;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "servico")
public class Servico extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contratante_id") private Contratante contratante;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "orcamento_id") private Orcamento orcamento;
    @Column(nullable = false, length = 30) private String codigo;
    @Column(nullable = false, length = 200) private String titulo;
    @Column(length = 4000) private String descricao;
    @Column(length = 120) private String responsavel;
    @Column(name = "local_execucao", length = 500) private String localExecucao;
    @Column(name = "inicio_previsto") private Instant inicioPrevisto;
    @Column(name = "fim_previsto") private Instant fimPrevisto;
    @Column(name = "data_programada") private java.time.LocalDate dataProgramada;
    @Column(name = "data_fim_programada") private java.time.LocalDate dataFim;
    @Column(name = "concluido_em") private Instant concluidoEm;
    @Column(name = "resumo_conclusao", length = 4000) private String resumoConclusao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private StatusServico status;
    @Version @Column(name = "versao") private long versao;

    protected Servico() {}
    public Servico(Unidade unidade, Contratante contratante, Orcamento orcamento, String titulo,
                   String descricao, String responsavel, String localExecucao) {
        this.id = UUID.randomUUID();
        this.codigo = "SV-" + id.toString().substring(0, 8).toUpperCase();
        this.unidade = unidade;
        this.contratante = contratante;
        this.orcamento = orcamento;
        this.status = StatusServico.RASCUNHO;
        atualizar(contratante, orcamento, titulo, descricao, responsavel, localExecucao);
    }
    public void atualizar(Contratante contratante, Orcamento orcamento, String titulo,
                          String descricao, String responsavel, String localExecucao) {
        this.contratante = contratante;
        this.orcamento = orcamento;
        this.titulo = titulo.strip();
        this.descricao = descricao;
        this.responsavel = responsavel;
        this.localExecucao = localExecucao;
    }
    public void agendar(Instant inicio, Instant fim, String responsavel) {
        this.inicioPrevisto = inicio;
        this.fimPrevisto = fim;
        this.responsavel = responsavel;
        this.status = StatusServico.AGENDADO;
    }
    public void agendar(java.time.LocalDate dia, java.time.LocalDate fim, String responsavel) {
        this.dataFim = fim;
        this.dataProgramada = dia;
        this.inicioPrevisto = null;
        this.fimPrevisto = null;
        this.responsavel = responsavel;
        this.status = StatusServico.AGENDADO;
    }
    public java.time.LocalDate getDataInicio() { return dataProgramada; }
    public java.time.LocalDate getDataFim() { return dataFim == null ? dataProgramada : dataFim; }
    public java.time.LocalDate getDataProgramada() { return dataProgramada; }
    public void alterarStatus(StatusServico novo, String resumo) {
        this.status = novo;
        if (novo == StatusServico.CONCLUIDO) {
            this.concluidoEm = Instant.now();
            this.resumoConclusao = resumo;
        }
    }
    public void definirCodigo(String valor) { this.codigo = valor; }
    public UUID getId() { return id; }
    public UUID getUnidadeId() { return unidade.getId(); }
    public Contratante getContratante() { return contratante; }
    public Orcamento getOrcamento() { return orcamento; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getResponsavel() { return responsavel; }
    public String getLocalExecucao() { return localExecucao; }
    public Instant getInicioPrevisto() { return inicioPrevisto; }
    public Instant getFimPrevisto() { return fimPrevisto; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public String getResumoConclusao() { return resumoConclusao; }
    public StatusServico getStatus() { return status; }
}
