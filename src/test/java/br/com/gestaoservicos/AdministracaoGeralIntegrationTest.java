package br.com.gestaoservicos;

import br.com.gestaoservicos.config.AdministradorGeralInicial;
import br.com.gestaoservicos.usuario.model.Usuario;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"ADMIN_GERAL_EMAIL=geral.teste@example.com", "ADMIN_GERAL_SENHA=senha-teste-segura-123",
        "app.acesso.cadastroPublico=false", "app.acesso.somenteGeralCriaUnidade=true"})
@AutoConfigureMockMvc
class AdministracaoGeralIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;
    @Autowired AdministradorGeralInicial inicial;

    @Test
    void geralCriaUnidadesEDefineAdminSemDarAcessoGlobalAoAdminLocal() throws Exception {
        String geral=login("geral.teste@example.com", "senha-teste-segura-123");
        String sufixo=UUID.randomUUID().toString();
        String email="local."+sufixo+"@example.com";
        String unidade=criar(geral, "Unidade A "+sufixo, email, "senha-local-123");
        String outra=criar(geral, "Unidade B "+sufixo, "outro."+sufixo+"@example.com", "senha-local-123");
        String local=login(email,"senha-local-123");
        mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+geral))
                .andExpect(status().isOk()).andExpect(jsonPath("$.administradorGeral").value(true));
        mvc.perform(get("/api/v1/me").header("Authorization","Bearer "+local))
                .andExpect(status().isOk()).andExpect(jsonPath("$.administradorGeral").value(false))
                .andExpect(jsonPath("$.unidades.length()").value(1)).andExpect(jsonPath("$.unidades[0].perfil").value("ADMIN"));
        mvc.perform(get("/api/v1/administracao-geral/unidades").header("Authorization","Bearer "+local)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/administracao-geral/unidades").header("Authorization","Bearer "+local)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nome","Bloqueada","nomeAdministrador","Admin","emailAdministrador",email))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/unidades").header("Authorization","Bearer "+local)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Não autorizada\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/unidades/atual/protegida").header("Authorization","Bearer "+local).header("X-Unidade-Id",outra))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/unidades/atual/protegida").header("Authorization","Bearer "+geral).header("X-Unidade-Id",outra))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("ADMIN"));
        mvc.perform(post("/api/v1/me/unidades/"+unidade+"/selecionar").header("Authorization","Bearer "+geral))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(unidade));
        mvc.perform(get("/api/v1/unidades/atual/usuarios").header("Authorization","Bearer "+geral).header("X-Unidade-Id",unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/unidades/atual/protegida").header("Authorization","Bearer "+geral).header("X-Unidade-Id",UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void producaoFechaCadastroPublicoEInicializacaoNaoRedefineSenhaNemPromoveContaExistente() throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Intruso\",\"email\":\"intruso@example.com\",\"senha\":\"senha-local-123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/administracao-geral/unidades")).andExpect(status().isUnauthorized());
        var geral=usuarios.findByEmailIgnoreCase("geral.teste@example.com").orElseThrow();
        String hash=geral.getSenhaHash();
        inicial.run(new DefaultApplicationArguments(new String[0]));
        assertThat(usuarios.findById(geral.getId()).orElseThrow().getSenhaHash()).isEqualTo(hash);
        assertThat(senhas.matches("senha-teste-segura-123",hash)).isTrue();
        String email="sem.global."+UUID.randomUUID()+"@example.com";
        var normal=usuarios.saveAndFlush(new Usuario("Normal",email,senhas.encode("senha-local-123")));
        assertThatThrownBy(() -> new AdministradorGeralInicial(usuarios,senhas,email,"outra-senha-segura").run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.findById(normal.getId()).orElseThrow().isAdministradorGeral()).isFalse();
    }

    @Test
    void criacaoDuplicadaReverteUsuarioENaoAlteraSenhaDeContaExistente() throws Exception {
        String token=login("geral.teste@example.com","senha-teste-segura-123");
        String sufixo=UUID.randomUUID().toString();
        String nome="Transacional "+sufixo,email="transacao."+sufixo+"@example.com",emailRevertido="rollback."+sufixo+"@example.com";
        criar(token,nome,email,"senha-local-123");
        mvc.perform(post("/api/v1/administracao-geral/unidades").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nome",nome,"nomeAdministrador","Admin","emailAdministrador",emailRevertido,"senhaAdministrador","senha-local-123"))))
                .andExpect(status().isConflict());
        assertThat(usuarios.existsByEmailIgnoreCase(emailRevertido)).isFalse();
        mvc.perform(post("/api/v1/administracao-geral/unidades").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nome","Nova "+sufixo,"nomeAdministrador","Admin","emailAdministrador",email,"senhaAdministrador","outra-senha-123"))))
                .andExpect(status().isConflict());
        criar(token,"Reutiliza "+sufixo,email,null);
        login(email,"senha-local-123");
    }
    String login(String email,String senha) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email,"senha",senha)))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText();
    }
    String criar(String token,String nome,String email,String senha) throws Exception {
        var corpo=new java.util.HashMap<String,String>(); corpo.put("nome",nome); corpo.put("nomeAdministrador","Admin local"); corpo.put("emailAdministrador",email);
        if(senha!=null)corpo.put("senhaAdministrador",senha);
        return json.readTree(mvc.perform(post("/api/v1/administracao-geral/unidades").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }
}
