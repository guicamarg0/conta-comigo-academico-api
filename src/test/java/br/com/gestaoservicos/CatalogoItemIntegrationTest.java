package br.com.gestaoservicos;

import br.com.gestaoservicos.catalogo.model.UnidadeMedida;
import br.com.gestaoservicos.catalogo.service.ItemCatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CatalogoItemIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapeadorJson;
    @Autowired ItemCatalogoService catalogo;

    @Test void cadastraBuscaEditaInativaEValidaPrecisaoEMedida() throws Exception {
        String token = autenticar("Ana Catálogo", "ana.catalogo@example.com"); UUID unidade = criarUnidade(token, "Unidade Catálogo");
        UUID id = criar(token, unidade, new Item("MAO_DE_OBRA", "Visita técnica", "Avaliação", "HORA", null, new BigDecimal("150.00"), "VIS-01"));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).param("busca", "VIS-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo.length()").value(1)).andExpect(jsonPath("$.conteudo[0].nome").value("Visita técnica"));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).param("tipo", "MAO_DE_OBRA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).param("tipo", "PECA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(put("/api/v1/catalogo-itens/{id}", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(new Item("MATERIAL", "Cabo", null, "METRO", null, new BigDecimal("12.50"), null))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("MATERIAL"));
        mvc.perform(patch("/api/v1/catalogo-itens/{id}/inativar", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/catalogo-itens/{id}", id).header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        ResponseStatusException inativo = assertThrows(ResponseStatusException.class, () -> catalogo.obterAtivoParaNovoOrcamento(unidade, id));
        assertEquals(409, inativo.getStatusCode().value());
        mvc.perform(post("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(new Item("PECA", "Inválido", null, "UNIDADE", null, new BigDecimal("1.999"), null))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
        mvc.perform(post("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(new Item("PECA", "Outra", null, "OUTRA", null, new BigDecimal("1.00"), null))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
    }

    @Test void isolaItensPorUnidadeEAplicaRegraDeQuantidadePorMedida() throws Exception {
        String tokenAlice = autenticar("Alice Catálogo", "alice.catalogo@example.com"); UUID unidadeAlice = criarUnidade(tokenAlice, "Unidade Alice Catálogo"); UUID id = criar(tokenAlice, unidadeAlice, new Item("PECA", "Peça Alice", null, "UNIDADE", null, new BigDecimal("3.00"), null));
        String tokenBob = autenticar("Bob Catálogo", "bob.catalogo@example.com"); UUID unidadeBob = criarUnidade(tokenBob, "Unidade Bob Catálogo");
        mvc.perform(get("/api/v1/catalogo-itens/{id}", id).header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeBob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/catalogo-itens/{id}", id).header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeAlice)).andExpect(status().isForbidden());
        assertTrue(UnidadeMedida.HORA.aceitaQuantidade(new BigDecimal("2")));
        assertFalse(UnidadeMedida.HORA.aceitaQuantidade(new BigDecimal("2.50")));
        assertTrue(UnidadeMedida.METRO.aceitaQuantidade(new BigDecimal("2.50")));
        assertFalse(UnidadeMedida.METRO.aceitaQuantidade(new BigDecimal("2.555")));
    }

    @Test void filtraStatusEOrdenaValoresNoServidorAntesDePaginar() throws Exception {
        String token=autenticar("Ordenação", "catalogo.ordem@example.com");UUID unidade=criarUnidade(token,"Unidade Ordenação");
        UUID caro=criar(token,unidade,new Item("PECA","Peça cara",null,"UNIDADE",null,new BigDecimal("90"),"003"));
        criar(token,unidade,new Item("PECA","Peça barata",null,"UNIDADE",null,new BigDecimal("10"),"001"));
        criar(token,unidade,new Item("PECA","Peça média",null,"UNIDADE",null,new BigDecimal("50"),"002"));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade).param("size","1").param("sort","valorPadrao,asc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[0].codigoReferencia").value("001")).andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade).param("size","1").param("sort","valorPadrao,desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[0].codigoReferencia").value("003"));
        mvc.perform(patch("/api/v1/catalogo-itens/{id}/inativar",caro).header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade).param("ativo","false"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.conteudo[0].id").value(caro.toString()));
        mvc.perform(get("/api/v1/catalogo-itens").header("Authorization","Bearer "+token).header("X-Unidade-Id",unidade).param("ativo","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2));
    }

    private UUID criar(String token, UUID unidade, Item item) throws Exception { return UUID.fromString(mapeadorJson.readTree(mvc.perform(post("/api/v1/catalogo-itens").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(json(item))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private String autenticar(String nome, String email) throws Exception { mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(new Cadastro(nome, email, "password123")))).andExpect(status().isCreated()); return mapeadorJson.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json(new Login(email, "password123")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText(); }
    private UUID criarUnidade(String token, String nome) throws Exception { return UUID.fromString(mapeadorJson.readTree(mvc.perform(post("/api/v1/unidades").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(json(new Nome(nome)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private String json(Object valor) throws Exception { return mapeadorJson.writeValueAsString(valor); }
    record Cadastro(String nome, String email, String senha) {} record Login(String email, String senha) {} record Nome(String nome) {} record Item(String tipo, String nome, String descricao, String unidadeMedida, String unidadeMedidaPersonalizada, BigDecimal valorPadrao, String codigoReferencia) {}
}
