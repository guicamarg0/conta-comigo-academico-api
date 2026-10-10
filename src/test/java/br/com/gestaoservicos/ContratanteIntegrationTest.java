package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ContratanteIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapeadorJson;
    @Test void cadastraBuscaInativaEGaranteDocumentoUnicoPorUnidade() throws Exception {
        String token = autenticar("Ana Contratante", "ana.contratante@example.com"); UUID unidade = criarUnidade(token, "Unidade Contratante");
        UUID id = criar(token, unidade, "12345678901", "Cliente Alfa");
        mvc.perform(get("/api/v1/contratantes").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).param("busca", "Alfa"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo.length()").value(1)).andExpect(jsonPath("$.conteudo[0].contatos.length()").value(1));
        mvc.perform(post("/api/v1/contratantes").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(contratante("12345678901", "Duplicado", true))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTRATANTE_DOCUMENTO_JA_EXISTE"));
        mvc.perform(patch("/api/v1/contratantes/{id}/inativar", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/contratantes/{id}", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/contratantes").param("ativo","true").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/contratantes").param("ativo","false").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(patch("/api/v1/contratantes/{id}/reativar",id).header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/contratantes/{id}", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
    }
    @Test void bloqueiaAcessoCruzadoEAceitaMesmoDocumentoEmOutraUnidade() throws Exception {
        String tokenAlice = autenticar("Alice Cliente", "alice.cliente@example.com"); UUID unidadeAlice = criarUnidade(tokenAlice, "Unidade Alice Cliente"); UUID id = criar(tokenAlice, unidadeAlice, "12345678901", "Cliente Alice");
        String tokenBob = autenticar("Bob Cliente", "bob.cliente@example.com"); UUID unidadeBob = criarUnidade(tokenBob, "Unidade Bob Cliente");
        mvc.perform(get("/api/v1/contratantes/{id}", id).header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeAlice)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/contratantes").header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeBob).contentType(MediaType.APPLICATION_JSON).content(json(contratante("12345678901", "Cliente Bob", true)))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/contratantes").header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeBob).contentType(MediaType.APPLICATION_JSON).content(json(contratante("12345678902", "Cliente inválido", false)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
    }
    private UUID criar(String token, UUID unidade, String documento, String nome) throws Exception { return UUID.fromString(mapeadorJson.readTree(mvc.perform(post("/api/v1/contratantes").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(contratante(documento, nome, true)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private String autenticar(String nome, String email) throws Exception { mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(new Cadastro(nome, email, "password123")))).andExpect(status().isCreated()); return mapeadorJson.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json(new Login(email, "password123")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText(); }
    private UUID criarUnidade(String token, String nome) throws Exception { return UUID.fromString(mapeadorJson.readTree(mvc.perform(post("/api/v1/unidades").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(json(new Nome(nome)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private Contratante contratante(String documento, String nome, boolean contatoValido) { return new Contratante("PF", nome, null, documento, null, null, List.of(new Contato("Principal", contatoValido ? "11999999999" : null, null))); }
    private String json(Object valor) throws Exception { return mapeadorJson.writeValueAsString(valor); }
    record Cadastro(String nome, String email, String senha) {} record Login(String email, String senha) {} record Nome(String nome) {} record Contratante(String tipo, String nomeRazaoSocial, String nomeFantasia, String documento, String endereco, String observacaoInterna, List<Contato> contatos) {} record Contato(String nome, String telefone, String email) {}
}
