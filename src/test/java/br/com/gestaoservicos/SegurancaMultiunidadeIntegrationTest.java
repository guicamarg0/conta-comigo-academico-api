package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SegurancaMultiunidadeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapeadorJson;

    @Test
    void criaUnidadeComAssociacaoAdminEBloqueiaAcessoCruzado() throws Exception {
        String tokenAlice = cadastrarEAutenticar("Alice", "alice@example.com");
        UUID unidadeAlice = criarUnidade(tokenAlice, "Oficina Alice");
        String tokenBob = cadastrarEAutenticar("Bob", "bob@example.com");
        UUID unidadeBob = criarUnidade(tokenBob, "Oficina Bob");

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidades[0].id").value(unidadeAlice.toString()))
                .andExpect(jsonPath("$.unidades[0].perfil").value("ADMIN"));

        mvc.perform(post("/api/v1/me/unidades/{unidadeId}/selecionar", unidadeBob)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/unidades/atual/protegida")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .header("X-Unidade-Id", unidadeBob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.path").value("/api/v1/unidades/atual/protegida"));

        mvc.perform(get("/api/v1/unidades/atual/protegida")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .header("X-Unidade-Id", unidadeAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadeId").value(unidadeAlice.toString()))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void rotaProtegidaExigeAutenticacaoEUnidadeSelecionada() throws Exception {
        mvc.perform(get("/api/v1/unidades/atual/protegida"))
                .andExpect(status().isUnauthorized());

        String token = cadastrarEAutenticar("Carol", "carol@example.com");
        mvc.perform(get("/api/v1/unidades/atual/protegida").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cabecalhoDeUnidadeSemJwtNaoInterfereEmRotaPublica() throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro")
                        .header("X-Unidade-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(
                                new CredenciaisCadastro("Daniel", "daniel@example.com", "password123"))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejeicaoDeCabecalhoDeUnidadeInvalidoSegueContratoDeErro() throws Exception {
        String token = cadastrarEAutenticar("Erica", "erica@example.com");

        mvc.perform(get("/api/v1/unidades/atual/protegida")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Unidade-Id", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.path").value("/api/v1/unidades/atual/protegida"));
    }

    @Test
    void permitePreflightDoFrontendLocalNaPorta3000() throws Exception {
        mvc.perform(options("/api/v1/autenticacao/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void administradorGerenciaAcessosSemPermitirRemoverUltimoAdmin() throws Exception {
        String tokenAdmin = cadastrarEAutenticar("Admin Equipe", "admin.equipe@example.com");
        UUID unidadeId = criarUnidade(tokenAdmin, "Unidade Equipe");
        cadastrarEAutenticar("Operador Equipe", "operador.equipe@example.com");

        String corpoAssociacao = mvc.perform(post("/api/v1/unidades/atual/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .header("X-Unidade-Id", unidadeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new UsuarioUnidade("operador.equipe@example.com", "OPERADOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("operador.equipe@example.com"))
                .andExpect(jsonPath("$.perfil").value("OPERADOR"))
                .andReturn().getResponse().getContentAsString();
        UUID associacaoOperador = UUID.fromString(mapeadorJson.readTree(corpoAssociacao).get("id").asText());

        mvc.perform(get("/api/v1/unidades/atual/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin).header("X-Unidade-Id", unidadeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/api/v1/unidades/atual/usuarios/{id}", associacaoOperador)
                        .header("Authorization", "Bearer " + tokenAdmin).header("X-Unidade-Id", unidadeId))
                .andExpect(status().isNoContent());

        String corpoUsuarios = mvc.perform(get("/api/v1/unidades/atual/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin).header("X-Unidade-Id", unidadeId))
                .andReturn().getResponse().getContentAsString();
        UUID associacaoAdmin = UUID.fromString(mapeadorJson.readTree(corpoUsuarios).get(0).get("id").asText());
        mvc.perform(delete("/api/v1/unidades/atual/usuarios/{id}", associacaoAdmin)
                        .header("Authorization", "Bearer " + tokenAdmin).header("X-Unidade-Id", unidadeId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ULTIMO_ADMIN_DA_UNIDADE"));
    }

    @Test
    void gestorConsultaEquipeSemPromoverAdministradores() throws Exception {
        String tokenAdmin = cadastrarEAutenticar("Admin Permissoes", "admin.permissoes@example.com");
        UUID unidadeId = criarUnidade(tokenAdmin, "Unidade Permissoes");
        String tokenGestor = cadastrarEAutenticar("Gestor Permissoes", "gestor.permissoes@example.com");
        mvc.perform(post("/api/v1/unidades/atual/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                        .header("X-Unidade-Id", unidadeId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new UsuarioUnidade("gestor.permissoes@example.com", "GESTOR"))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/unidades/atual/usuarios").header("Authorization", "Bearer " + tokenGestor)
                        .header("X-Unidade-Id", unidadeId))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/unidades/atual/usuarios").header("Authorization", "Bearer " + tokenGestor).header("X-Unidade-Id", unidadeId).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin.permissoes@example.com\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejeitaNomeDeUnidadeDuplicadoIgnorandoAcentosCaixaEEspacos() throws Exception {
        String tokenAlice = cadastrarEAutenticar("Alice Duplicidade", "alice.duplicidade@example.com");
        criarUnidade(tokenAlice, "Oficina São   Bento");

        String tokenBob = cadastrarEAutenticar("Bob Duplicidade", "bob.duplicidade@example.com");
        mvc.perform(post("/api/v1/unidades")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new NomeUnidade("  OFICINA SAO BENTO  "))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UNIDADE_NOME_JA_EXISTENTE"))
                .andExpect(jsonPath("$.detail").value("Já existe uma unidade com esse nome."));
    }

    @Test
    void permiteSomenteUmaCriacaoQuandoRequisicoesConcorrentesUsamOMesmoNome() throws Exception {
        String tokenAlice = cadastrarEAutenticar("Alice Concorrência", "alice.concorrencia@example.com");
        String tokenBob = cadastrarEAutenticar("Bob Concorrência", "bob.concorrencia@example.com");
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch disparar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var respostas = List.of(tokenAlice, tokenBob).stream()
                    .map(token -> executor.submit(() -> {
                        prontas.countDown();
                        disparar.await();
                        return mvc.perform(post("/api/v1/unidades")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapeadorJson.writeValueAsString(
                                                new NomeUnidade("Unidade Concorrente"))))
                                .andReturn().getResponse().getStatus();
                    }))
                    .toList();

            prontas.await();
            disparar.countDown();

            assertThat(respostas.get(0).get()).isIn(201, 409);
            assertThat(respostas.get(1).get()).isIn(201, 409);
            assertThat(respostas.stream().map(resposta -> {
                try { return resposta.get(); }
                catch (Exception excecao) { throw new AssertionError(excecao); }
            }).toList()).containsExactlyInAnyOrder(201, 409);
        }
    }

    private String cadastrarEAutenticar(String nome, String email) throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new CredenciaisCadastro(nome, email, "password123"))))
                .andExpect(status().isCreated());
        String corpo = mvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new CredenciaisLogin(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapeadorJson.readTree(corpo).get("tokenAcesso").asText();
    }

    private UUID criarUnidade(String token, String nome) throws Exception {
        String corpo = mvc.perform(post("/api/v1/unidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeadorJson.writeValueAsString(new NomeUnidade(nome))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapeadorJson.readTree(corpo);
        UUID id = UUID.fromString(json.get("id").asText());
        assertThat(id).isNotNull();
        return id;
    }

    record CredenciaisCadastro(String nome, String email, String senha) {}
    record CredenciaisLogin(String email, String senha) {}
    record NomeUnidade(String nome) {}
    record UsuarioUnidade(String email, String perfil) {}
}
