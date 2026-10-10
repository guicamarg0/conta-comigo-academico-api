package br.com.gestaoservicos.orcamento.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="evento_orcamento") public class EventoOrcamento {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="orcamento_id") private Orcamento orcamento; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="revisao_orcamento_id") private RevisaoOrcamento revisao; @Enumerated(EnumType.STRING) @Column(nullable=false) private TipoEventoOrcamento tipo; private String observacao; @Column(name="ocorrido_em",nullable=false) private Instant ocorridoEm; @Column(name="usuario_id") private UUID usuarioId;
 protected EventoOrcamento(){} public EventoOrcamento(Orcamento o,RevisaoOrcamento r,TipoEventoOrcamento t,String obs,UUID usuario){id=UUID.randomUUID();orcamento=o;revisao=r;tipo=t;observacao=obs;ocorridoEm=Instant.now();usuarioId=usuario;}
 public UUID getId(){return id;} public TipoEventoOrcamento getTipo(){return tipo;} public String getObservacao(){return observacao;} public Instant getOcorridoEm(){return ocorridoEm;} public UUID getUsuarioId(){return usuarioId;} public RevisaoOrcamento getRevisao(){return revisao;}
}
