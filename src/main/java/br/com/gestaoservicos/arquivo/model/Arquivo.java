package br.com.gestaoservicos.arquivo.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="arquivo")
public class Arquivo {
    @Id private UUID id;
    @Column(name="unidade_id",nullable=false) private UUID unidadeId;
    @Column(nullable=false,length=200) private String nome;
    @Column(nullable=false,length=100) private String tipo;
    @Column(length=500) private String caminho;
    @Column private byte[] conteudo;
    @Column(name="criado_em",nullable=false) private Instant criadoEm;
    protected Arquivo() {}
    public Arquivo(UUID unidadeId,String nome,String tipo,String extensao) {
        id=UUID.randomUUID();this.unidadeId=unidadeId;this.nome=nome;this.tipo=tipo;
        caminho=unidadeId+"/anexos/"+id+"."+extensao;criadoEm=Instant.now();
    }
    public UUID getId(){return id;} public String getNome(){return nome;} public String getTipo(){return tipo;} public String getCaminho(){return caminho;}
    public void armazenarNoBanco(byte[] bytes) {conteudo=bytes.clone();caminho=null;}
    public byte[] getConteudo() {return conteudo==null?null:conteudo.clone();}
}
