package br.com.gestaoservicos;

import br.com.gestaoservicos.arquivo.service.SupabaseArmazenamentoAdapter;
import br.com.gestaoservicos.autenticacao.service.EmailRecuperacaoService;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessaoRenovacaoIntegrationTest {
    private static final String ORIGEM = "http://localhost:3000";
    private static final String COOKIE = "conta_comigo_renovacao";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate banco;
    @Autowired UsuarioRepository usuarios;
    @MockitoBean EmailRecuperacaoService emails;
    @MockitoBean
    SupabaseArmazenamentoAdapter storage;

    @Test
    void loginAntigoContinuaCompativelEUsaCookieDaSessaoDoNavegador() throws Exception {
        String email = "sessao.legada@test.com";
        cadastrar(email);

        var resposta = mvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "senha", "senha12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcesso").isString())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isString())
                .andReturn().getResponse();

        String setCookie = resposta.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains(COOKIE + "=").contains("HttpOnly").contains("Secure")
                .contains("SameSite=Lax").contains("Path=/api/v1/autenticacao")
                .doesNotContain("Max-Age").doesNotContain("Domain");
    }

    @Test
    void manterConectadoGiraTokenEReplayRevogaTodaFamilia() throws Exception {
        String email = "sessao.rotacao@test.com";
        cadastrar(email);
        var login = login(email, true);
        assertThat(login.cabecalho()).contains("Max-Age=2592000");
        assertThat(banco.queryForObject("select token_hash from sessao_renovacao where usuario_id = ?",
                String.class, usuarios.findByEmailIgnoreCase(email).orElseThrow().getId()))
                .hasSize(64).isNotEqualTo(login.token());

        var renovada = renovar(login.token(), ORIGEM, 200);
        assertThat(renovada.token()).isNotEqualTo(login.token());
        assertThat(banco.queryForObject("select count(*) from sessao_renovacao where usuario_id = ?",
                Integer.class, usuarios.findByEmailIgnoreCase(email).orElseThrow().getId())).isEqualTo(2);

        renovar(login.token(), ORIGEM, 401);
        renovar(renovada.token(), ORIGEM, 401);
        assertThat(banco.queryForObject("select count(*) from sessao_renovacao where usuario_id = ? and revogada_em is null",
                Integer.class, usuarios.findByEmailIgnoreCase(email).orElseThrow().getId())).isZero();
    }

    @Test
    void expiracaoLogoutEOrigemSaoValidados() throws Exception {
        String email = "sessao.saida@test.com";
        cadastrar(email);
        var login = login(email, true);

        renovar(login.token(), "https://origem-invalida.example", 403);
        mvc.perform(post("/api/v1/autenticacao/renovar").cookie(new Cookie(COOKIE, login.token())))
                .andExpect(status().isForbidden());

        banco.update("update sessao_renovacao set expira_em = criada_em where usuario_id = ?",
                usuarios.findByEmailIgnoreCase(email).orElseThrow().getId());
        renovar(login.token(), ORIGEM, 401);

        var outra = login(email, false);
        mvc.perform(post("/api/v1/autenticacao/sair").header("Origin", ORIGEM)
                        .cookie(new Cookie(COOKIE, outra.token())))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        renovar(outra.token(), ORIGEM, 401);
        mvc.perform(post("/api/v1/autenticacao/sair").header("Origin", ORIGEM))
                .andExpect(status().isNoContent());
    }

    @Test
    void redefinicaoDeSenhaRevogaRenovacao() throws Exception {
        String email = "sessao.reset@test.com";
        cadastrar(email);
        var login = login(email, true);

        mvc.perform(post("/api/v1/autenticacao/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email))))
                .andExpect(status().isOk());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(emails).enviar(eq(email), captor.capture());
        mvc.perform(post("/api/v1/autenticacao/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of(
                                "token", captor.getValue(), "senha", "novaSenha123"))))
                .andExpect(status().isNoContent());

        renovar(login.token(), ORIGEM, 401);
    }

    private void cadastrar(String email) throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of(
                                "nome", "Teste", "email", email, "senha", "senha12345"))))
                .andExpect(status().isCreated());
    }

    private TokenCookie login(String email, boolean manterConectado) throws Exception {
        var resposta = mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of(
                                "email", email, "senha", "senha12345", "manterConectado", manterConectado))))
                .andExpect(status().isOk()).andReturn().getResponse();
        String cabecalho = resposta.getHeader(HttpHeaders.SET_COOKIE);
        return new TokenCookie(valorCookie(cabecalho), cabecalho);
    }

    private TokenCookie renovar(String token, String origem, int status) throws Exception {
        var resposta = mvc.perform(post("/api/v1/autenticacao/renovar").header("Origin", origem)
                        .cookie(new Cookie(COOKIE, token)))
                .andExpect(status().is(status)).andReturn().getResponse();
        String cabecalho = resposta.getHeader(HttpHeaders.SET_COOKIE);
        return new TokenCookie(cabecalho == null ? "" : valorCookie(cabecalho), cabecalho);
    }

    private String valorCookie(String cabecalho) {
        return cabecalho.substring(cabecalho.indexOf('=') + 1, cabecalho.indexOf(';'));
    }

    private record TokenCookie(String token, String cabecalho) {}
}
