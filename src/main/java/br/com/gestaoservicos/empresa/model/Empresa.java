package br.com.gestaoservicos.empresa.model;

import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "empresa") public class Empresa {
 @Id private UUID id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "unidade_id", nullable = false) private Unidade unidade;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "matriz_id") private Empresa matriz;
 @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TipoEmpresa tipo;
 @Column(name="razao_social", nullable=false, length=160) private String razaoSocial;
 @Column(name="nome_fantasia", length=160) private String nomeFantasia;
 @Column(nullable=false,length=14) private String cnpj;
 @Column(name="inscricao_estadual",length=40) private String inscricaoEstadual;
 @Column(length=30) private String telefone; @Column(length=254) private String email;
 @Column(length=12) private String cep; @Column(length=180) private String logradouro; @Column(length=30) private String numero; @Column(length=120) private String complemento; @Column(length=120) private String bairro; @Column(length=120) private String cidade; @Column(length=2) private String estado;
 @Column(name="logo_url", columnDefinition="text") private String logoUrl;
 public String getLogoUrl(){return logoUrl;} public void atualizarLogo(String logo){logoUrl=logo;}
 @Column(nullable=false) private boolean ativo;
 protected Empresa() {}
 public Empresa(Unidade unidade, Empresa matriz, TipoEmpresa tipo, String razaoSocial, String nomeFantasia, String cnpj, String ie, String telefone, String email, String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String estado) { id=UUID.randomUUID(); this.unidade=unidade; ativo=true; atualizar(matriz,tipo,razaoSocial,nomeFantasia,cnpj,ie,telefone,email,cep,logradouro,numero,complemento,bairro,cidade,estado); }
 public void atualizar(Empresa matriz, TipoEmpresa tipo, String razaoSocial, String nomeFantasia, String cnpj, String ie, String telefone, String email, String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String estado) { this.matriz=matriz; this.tipo=tipo; this.razaoSocial=razaoSocial; this.nomeFantasia=nomeFantasia; this.cnpj=cnpj; this.inscricaoEstadual=ie; this.telefone=telefone; this.email=email; this.cep=cep; this.logradouro=logradouro; this.numero=numero; this.complemento=complemento; this.bairro=bairro; this.cidade=cidade; this.estado=estado; }
 public void inativar(){ativo=false;} public void reativar(){ativo=true;}
 public UUID getId(){return id;} public Unidade getUnidade(){return unidade;} public Empresa getMatriz(){return matriz;} public TipoEmpresa getTipo(){return tipo;} public String getRazaoSocial(){return razaoSocial;} public String getNomeFantasia(){return nomeFantasia;} public String getCnpj(){return cnpj;} public String getInscricaoEstadual(){return inscricaoEstadual;} public String getTelefone(){return telefone;} public String getEmail(){return email;} public String getCep(){return cep;} public String getLogradouro(){return logradouro;} public String getNumero(){return numero;} public String getComplemento(){return complemento;} public String getBairro(){return bairro;} public String getCidade(){return cidade;} public String getEstado(){return estado;} public boolean isAtivo(){return ativo;}
}
