package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ContratoRascunhoVersaoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void separaCadastroESituacaoPreservaHistoricoEBloqueiaEncerrado() throws Exception {
        String token = login("contrato.ciclo@test.com");
        UUID unidade = unidade(token, "Ciclo contrato");
        UUID orcamento = criarOrcamentoAprovado(token, unidade);
        String modeloJson = mvc.perform(post("/api/v1/modelos-contrato").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ciclo\",\"conteudo\":\"Inicial\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID modelo = UUID.fromString(json.readTree(modeloJson).get("id").asText());
        String corpo = contrato(orcamento, modelo, "Inicial");
        String criado = mvc.perform(post("/api/v1/contratos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURA")).andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(criado).get("id").asText());
        String operador = login("contrato.operador@test.com");
        String consulta = login("contrato.consulta@test.com");
        for (var acesso : java.util.Map.of("contrato.operador@test.com", "OPERADOR", "contrato.consulta@test.com", "CONSULTA").entrySet()) {
            mvc.perform(post("/api/v1/unidades/atual/usuarios").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("email", acesso.getKey(), "perfil", acesso.getValue()))))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/v1/contratos/{id}/versoes", id).header("Authorization", bearer(consulta)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        for (String acao : java.util.List.of("marcar-vigente", "encerrar", "inativar", "reativar")) {
            mvc.perform(post("/api/v1/contratos/{id}/{acao}", id, acao).header("Authorization", bearer(consulta)).header("X-Unidade-Id", unidade))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/v1/contratos/{id}", id).header("Authorization", bearer(operador)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk()).andExpect(jsonPath("$.numeroVersao").value(1));
        mvc.perform(post("/api/v1/contratos/{id}/encerrar", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/contratos/{id}", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk()).andExpect(jsonPath("$.numeroVersao").value(1));
        mvc.perform(post("/api/v1/contratos/{id}/marcar-vigente", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VIGENTE"));
        mvc.perform(post("/api/v1/contratos/{id}/inativar", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VIGENTE")).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/contratos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/contratos?cadastro=INATIVO&status=VIGENTE").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(put("/api/v1/contratos/{id}", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(corpo.replace("Inicial", "Revisado"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVersao").value(2)).andExpect(jsonPath("$.status").value("VIGENTE")).andExpect(jsonPath("$.ativo").value(false));
        String historico = mvc.perform(get("/api/v1/contratos/{id}/versoes?size=1&page=1", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.conteudo[0].autor").value("Ana"))
                .andReturn().getResponse().getContentAsString();
        UUID versao = UUID.fromString(json.readTree(historico).get("conteudo").get(0).get("id").asText());
        mvc.perform(post("/api/v1/contratos/{id}/versoes/{versao}/restaurar", id, versao).header("Authorization", bearer(consulta)).header("X-Unidade-Id", unidade))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/contratos/{id}/versoes/{versao}/restaurar", id, UUID.randomUUID()).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/contratos/{id}/versoes/{versao}/restaurar", id, versao).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroVersao").value(3)).andExpect(jsonPath("$.conteudoRascunho").value("Inicial"))
                .andExpect(jsonPath("$.status").value("VIGENTE")).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(post("/api/v1/contratos/{id}/encerrar", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/contratos/{id}", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/contratos/{id}/versoes/{versao}/restaurar", id, versao).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/contratos/{id}/reativar", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADO")).andExpect(jsonPath("$.ativo").value(true));
        mvc.perform(get("/api/v1/contratos/{id}/pdf", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/contratos/{id}/atividades", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(7)).andExpect(jsonPath("$.conteudo[0].autor").value("Ana"));
        String outro = login("contrato.ciclo.outra@test.com");
        UUID outra = unidade(outro, "Outro ciclo");
        mvc.perform(get("/api/v1/contratos/{id}/versoes", id).header("Authorization", bearer(outro)).header("X-Unidade-Id", outra)).andExpect(status().isNotFound());
    }

    @Test
    void aceitaPrestadorCpfEFormataDocumentosNasPrevias() throws Exception {
        String token = login("prestador.cpf@test.com");
        UUID unidade = unidade(token, "Unidade Autônomo");
        UUID prestador = empresa(token, unidade, "Prestador Autônomo", "52998224725");
        mvc.perform(post("/api/v1/empresas").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("tipo", "MATRIZ", "razaoSocial", "CPF Inválido", "cnpj", "52998224724"))))
                .andExpect(status().isBadRequest());
        UUID orcamento = criarOrcamentoAprovado(token, unidade);
        String conteudo = "@TipoDocumentoContratada @DocumentoContratada / legado @CNPJContratada / cliente @DocumentoCliente";
        UUID modelo = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/modelos-contrato").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("nome", "Modelo CPF", "conteudo", conteudo))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/v1/contratos/previa").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("orcamentoId", orcamento, "modeloContratoId", modelo, "empresaId", prestador, "conteudoRascunho", conteudo))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").value("CPF 529.982.247-25 / legado 529.982.247-25 / cliente 123.456.789-01"));
    }

    @Test
    void preservaRascunhosEVersoesDeModeloEContrato() throws Exception {
        String token = login("contrato.versoes@test.com");
        UUID unidade = unidade(token, "Unidade Versões Contrato");
        String modelo = mvc.perform(post("/api/v1/modelos-contrato").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Modelo técnico\",\"descricao\":\"Teste\",\"conteudo\":\"Cliente @RazaoSocialCliente\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RASCUNHO")).andExpect(jsonPath("$.numeroVersao").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID modeloId = UUID.fromString(json.readTree(modelo).get("id").asText());

        mvc.perform(put("/api/v1/modelos-contrato/{id}", modeloId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Modelo técnico\",\"descricao\":\"Teste\",\"conteudo\":\"Cliente @RazaoSocialCliente atualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroVersao").value(2));
        mvc.perform(get("/api/v1/modelos-contrato/{id}/versoes", modeloId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].numero").value(2)).andExpect(jsonPath("$[0].conteudo").value("Cliente @RazaoSocialCliente atualizado"));
        mvc.perform(post("/api/v1/modelos-contrato/{id}/inativar", modeloId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARQUIVADO"));
        mvc.perform(post("/api/v1/modelos-contrato/{id}/reativar", modeloId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLICADO"));

        UUID orcamento = criarOrcamentoAprovado(token, unidade);
        UUID empresaInicial = empresa(token, unidade, "Empresa Inicial LTDA", "12345678000190");
        UUID empresaNova = empresa(token, unidade, "Empresa Nova LTDA", "12345678000270");
        UUID outraUnidade = unidade(token, "Outra Unidade Contrato");
        UUID empresaExterna = empresa(token, outraUnidade, "Empresa Externa LTDA", "12345678000350");
        String corpoInicial = comEmpresa(contrato(orcamento, modeloId, "Versão inicial"), empresaInicial);
        String criado = mvc.perform(post("/api/v1/contratos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInicial))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.empresaId").value(empresaInicial.toString())).andExpect(jsonPath("$.numeroVersao").value(1)).andExpect(jsonPath("$.conteudoRascunho").value("Versão inicial"))
                .andReturn().getResponse().getContentAsString();
        UUID contratoId = UUID.fromString(json.readTree(criado).get("id").asText());

        mvc.perform(put("/api/v1/contratos/{id}", contratoId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(comEmpresa(contrato(orcamento, modeloId, "Inválido"), empresaExterna)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/contratos/{id}", contratoId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(comEmpresa(contrato(orcamento, modeloId, "Versão revisada"), empresaNova)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.empresaId").value(empresaNova.toString())).andExpect(jsonPath("$.empresaContratada").value("Empresa Nova LTDA")).andExpect(jsonPath("$.numeroVersao").value(2)).andExpect(jsonPath("$.conteudoRascunho").value("Versão revisada"));
        mvc.perform(post("/api/v1/contratos/previa").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("orcamentoId", orcamento, "modeloContratoId", modeloId, "empresaId", empresaNova))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").value("Cliente Cliente Contrato atualizado"))
                .andExpect(jsonPath("$.assinaturasHtml").value(org.hamcrest.Matchers.containsString("Empresa Nova LTDA")));
        mvc.perform(get("/api/v1/contratos/{id}/snapshots", contratoId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].numeroVersao").value(2)).andExpect(jsonPath("$[0].conteudo").value("Versão revisada")).andExpect(jsonPath("$[1].conteudo").value("Versão inicial"));
    }

    private String comEmpresa(String corpo, UUID empresaId) {
        return corpo.substring(0, corpo.length() - 1) + ",\"empresaId\":\"" + empresaId + "\"}";
    }
    private UUID empresa(String token, UUID unidade, String nome, String cnpj) throws Exception {
        return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("tipo", "MATRIZ", "razaoSocial", nome, "cnpj", cnpj))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
    }

    private UUID criarOrcamentoAprovado(String token, UUID unidade) throws Exception {
        UUID cliente = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/contratantes").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PF\",\"nomeRazaoSocial\":\"Cliente Contrato\",\"documento\":\"12345678901\",\"contatos\":[{\"nome\":\"Principal\",\"telefone\":\"11999999999\"}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
        String orcamento = mvc.perform(post("/api/v1/orcamentos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contratanteId\":\"" + cliente + "\",\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"tipo\":\"MAO_DE_OBRA\",\"descricao\":\"Visita\",\"unidadeMedida\":\"HORA\",\"quantidade\":1,\"valorUnitario\":100}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(orcamento).get("id").asText());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos/{id}/aprovar", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        return id;
    }

    private String contrato(UUID orcamentoId, UUID modeloId, String conteudo) {
        return "{\"orcamentoId\":\"" + orcamentoId + "\",\"modelo\":\"Modelo técnico\",\"objeto\":\"Serviço industrial\",\"inicioVigencia\":\"2026-10-01\",\"fimVigencia\":\"2027-04-01\",\"modeloContratoId\":\"" + modeloId + "\",\"conteudoRascunho\":\"" + conteudo + "\"}";
    }
    private String login(String email) throws Exception { mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Cadastro("Ana", email, "password123")))).andExpect(status().isCreated()); return json.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Login(email, "password123")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText(); }
    private UUID unidade(String token, String nome) throws Exception { return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/unidades").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Nome(nome)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private String bearer(String token) { return "Bearer " + token; }
    record Cadastro(String nome, String email, String senha) {}
    record Login(String email, String senha) {}
    record Nome(String nome) {}
}
