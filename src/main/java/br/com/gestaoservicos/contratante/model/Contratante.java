package br.com.gestaoservicos.contratante.model;

import br.com.gestaoservicos.compartilhado.auditoria.Auditavel;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "contratante")
public class Contratante extends Auditavel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id", nullable = false) private Unidade unidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2) private TipoContratante tipo;
    @Column(name = "nome_razao_social", nullable = false, length = 120) private String nomeRazaoSocial;
    @Column(name = "nome_fantasia", length = 120) private String nomeFantasia;
    @Column(nullable = false, length = 14) private String documento;
    @Column(length = 500) private String endereco;
    @Column(name = "observacao_interna", length = 2000) private String observacaoInterna;
    @Column(nullable = false) private boolean ativo;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) @JoinColumn(name = "contratante_id", nullable = false) private List<ContatoContratante> contatos = new ArrayList<>();
    protected Contratante() {}
    public Contratante(Unidade unidade, TipoContratante tipo, String nomeRazaoSocial, String nomeFantasia, String documento, String endereco, String observacaoInterna, List<ContatoContratante> contatos) { this.id = UUID.randomUUID(); this.unidade = unidade; this.ativo = true; atualizar(tipo, nomeRazaoSocial, nomeFantasia, documento, endereco, observacaoInterna, contatos); }
    public void atualizar(TipoContratante tipo, String nomeRazaoSocial, String nomeFantasia, String documento, String endereco, String observacaoInterna, List<ContatoContratante> contatos) { this.tipo = tipo; this.nomeRazaoSocial = nomeRazaoSocial; this.nomeFantasia = nomeFantasia; this.documento = documento; this.endereco = endereco; this.observacaoInterna = observacaoInterna; this.contatos.clear(); this.contatos.addAll(contatos); }
    public void inativar() { this.ativo = false; }
    public void reativar() { this.ativo = true; }
    public UUID getId() { return id; } public TipoContratante getTipo() { return tipo; } public String getNomeRazaoSocial() { return nomeRazaoSocial; } public String getNomeFantasia() { return nomeFantasia; } public String getDocumento() { return documento; } public String getEndereco() { return endereco; } public String getObservacaoInterna() { return observacaoInterna; } public boolean isAtivo() { return ativo; } public List<ContatoContratante> getContatos() { return List.copyOf(contatos); }
}
