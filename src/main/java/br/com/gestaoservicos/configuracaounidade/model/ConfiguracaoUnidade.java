package br.com.gestaoservicos.configuracaounidade.model;

import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "configuracao_unidade")
public class ConfiguracaoUnidade {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id", nullable = false, unique = true)
    private Unidade unidade;
    @Column(name = "nome_razao_social", nullable = false, length = 120) private String nomeRazaoSocial;
    @Column(nullable = false, length = 14) private String documento;
    @Column(name = "endereco_completo", nullable = false, length = 500) private String enderecoCompleto;
    @Column(name = "nome_fantasia", length = 120) private String nomeFantasia;
    @Column(length = 254) private String email;
    @Column(name = "logo_url", length = 500) private String logoUrl;
    @Column(length = 120) private String responsavel;
    @Column(name = "condicoes_pagamento_padrao", length = 2000) private String condicoesPagamentoPadrao;

    protected ConfiguracaoUnidade() {}
    public ConfiguracaoUnidade(Unidade unidade, String nomeRazaoSocial, String documento, String enderecoCompleto,
                               String nomeFantasia, String email, String logoUrl, String responsavel, String condicoesPagamentoPadrao) {
        this.id = UUID.randomUUID(); this.unidade = unidade; atualizar(nomeRazaoSocial, documento, enderecoCompleto, nomeFantasia, email, logoUrl, responsavel, condicoesPagamentoPadrao);
    }
    public void atualizar(String nomeRazaoSocial, String documento, String enderecoCompleto, String nomeFantasia, String email, String logoUrl, String responsavel, String condicoesPagamentoPadrao) {
        this.nomeRazaoSocial = nomeRazaoSocial; this.documento = documento; this.enderecoCompleto = enderecoCompleto; this.nomeFantasia = nomeFantasia; this.email = email; this.logoUrl = logoUrl; this.responsavel = responsavel; this.condicoesPagamentoPadrao = condicoesPagamentoPadrao;
    }
    public UUID getId() { return id; } public Unidade getUnidade() { return unidade; } public String getNomeRazaoSocial() { return nomeRazaoSocial; }
    public String getDocumento() { return documento; } public String getEnderecoCompleto() { return enderecoCompleto; } public String getNomeFantasia() { return nomeFantasia; }
    public String getEmail() { return email; } public String getLogoUrl() { return logoUrl; } public String getResponsavel() { return responsavel; } public String getCondicoesPagamentoPadrao() { return condicoesPagamentoPadrao; }
}
