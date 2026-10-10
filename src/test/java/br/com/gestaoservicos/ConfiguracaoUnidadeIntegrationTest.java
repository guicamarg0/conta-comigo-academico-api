package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfiguracaoUnidadeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapeadorJson;

    @Test
    void consultaUnidadeSemConfiguracaoEConservaPagamentosIndependentes() throws Exception {
        String token = cadastrarEAutenticar("Admin Inicial", "config.inicial@example.com");
        UUID unidade = criarUnidade(token, "Unidade sem configuração");
        mvc.perform(get("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeRazaoSocial").value(""))
                .andExpect(jsonPath("$.formasRecebimento.length()").value(0));
        mvc.perform(put("/api/v1/unidades/atual/formas-pagamento").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content(json(java.util.Map.of("formasRecebimento", List.of(new Forma("PIX", "Pix", "Chave de exemplo", true, true))))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documento").value(""))
                .andExpect(jsonPath("$.formasRecebimento[0].nomeExibicao").value("Pix"));
    }

    @Test
    void administraConfiguracaoComFormasDeRecebimentoEValidaPadrao() throws Exception {
        String token = cadastrarEAutenticar("Admin Configuração", "admin.configuracao@example.com");
        UUID unidadeId = criarUnidade(token, "Unidade Configuração");
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidadeId)
                        .contentType(MediaType.APPLICATION_JSON).content(json(configuracao("12345678000195", List.of(new Forma("PIX", "PIX principal", "Chave pix", true, true), new Forma("CONTA_BANCARIA", "Banco", "Agência e conta", true, false))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeRazaoSocial").value("Oficina Exemplo Ltda"))
                .andExpect(jsonPath("$.formasRecebimento.length()").value(2)).andExpect(jsonPath("$.formasRecebimento[1].padrao").value(true));
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + token).header("X-Unidade-Id", unidadeId)
                        .contentType(MediaType.APPLICATION_JSON).content(json(configuracao("12345678000195", List.of(new Forma("PIX", "PIX", "chave", true, true), new Forma("OUTRO", "Outro", "instrução", true, true))))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void bloqueiaAcessoCruzadoEValidaDocumento() throws Exception {
        String tokenAlice = cadastrarEAutenticar("Alice Config", "alice.config@example.com");
        UUID unidadeAlice = criarUnidade(tokenAlice, "Unidade Alice Config");
        String tokenBob = cadastrarEAutenticar("Bob Config", "bob.config@example.com");
        UUID unidadeBob = criarUnidade(tokenBob, "Unidade Bob Config");
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + tokenAlice).header("X-Unidade-Id", unidadeAlice)
                        .contentType(MediaType.APPLICATION_JSON).content(json(configuracao("12345678000195", List.of(new Forma("PIX", "PIX", "chave", true, true))))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeAlice))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization", "Bearer " + tokenBob).header("X-Unidade-Id", unidadeBob)
                        .contentType(MediaType.APPLICATION_JSON).content(json(configuracao("11111111111", List.of(new Forma("PIX", "PIX", "chave", true, true))))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOCUMENTO_INVALIDO"));
    }

    private String cadastrarEAutenticar(String nome, String email) throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(new CredenciaisCadastro(nome, email, "password123")))).andExpect(status().isCreated());
        return mapeadorJson.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json(new CredenciaisLogin(email, "password123")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText();
    }
    private UUID criarUnidade(String token, String nome) throws Exception { return UUID.fromString(mapeadorJson.readTree(mvc.perform(post("/api/v1/unidades").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(json(new Nome(nome)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText()); }
    private String json(Object valor) throws Exception { return mapeadorJson.writeValueAsString(valor); }
    private Configuracao configuracao(String documento, List<Forma> formas) { return new Configuracao("Oficina Exemplo Ltda", documento, "Rua Exemplo, 1", "Oficina Exemplo", "contato@example.com", null, null, "À vista", formas); }
    record CredenciaisCadastro(String nome, String email, String senha) {} record CredenciaisLogin(String email, String senha) {} record Nome(String nome) {}
    record Configuracao(String nomeRazaoSocial, String documento, String enderecoCompleto, String nomeFantasia, String email, String logoUrl, String responsavel, String condicoesPagamentoPadrao, List<Forma> formasRecebimento) {}
    record Forma(String tipo, String nomeExibicao, String instrucoes, boolean ativa, boolean padrao) {}
}
