package br.com.gestaoservicos;

import br.com.gestaoservicos.usuario.model.Usuario;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import br.com.gestaoservicos.unidade.model.Unidade;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import br.com.gestaoservicos.associacao.model.*;
import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class OnboardingIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios; @Autowired UnidadeRepository unidades;
    @Autowired AssociacaoRepository associacoes; @Autowired PasswordEncoder senhas; @Autowired JdbcTemplate banco;
    record Contexto(String token, UUID unidade) {}
    Contexto contexto() throws Exception {
        String sufixo=UUID.randomUUID().toString();
        var unidade=unidades.save(new Unidade("Onboarding "+sufixo,"onboarding "+sufixo));
        return new Contexto(usuario(unidade, Perfil.ADMIN), unidade.getId());
    }
    String usuario(Unidade unidade, Perfil perfil) throws Exception {
        String email="onboarding."+UUID.randomUUID()+"@example.com";
        var usuario=usuarios.save(new Usuario("Usuário",email,senhas.encode("senha-teste-123")));
        associacoes.save(new Associacao(usuario,unidade,perfil));
        return json.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",email,"senha","senha-teste-123"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText();
    }
    org.springframework.test.web.servlet.ResultActions atualizar(Contexto c) throws Exception {
        return mvc.perform(post("/api/v1/unidades/atual/onboarding/atualizar").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()));
    }
    org.springframework.test.web.servlet.ResultActions preferencias(Contexto c, String body) throws Exception {
        return mvc.perform(put("/api/v1/unidades/atual/onboarding").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    @Test void progressoUsaCadastrosSalvosEPreservaMarcosAposInativacao() throws Exception {
        var c=contexto();
        atualizar(c).andExpect(status().isOk()).andExpect(jsonPath("$.etapasConcluidas[0]").value(false)).andExpect(jsonPath("$.apresentacaoVista").value(false));
        banco.update("insert into empresa(id,unidade_id,tipo,razao_social,cnpj,ativo) values (?,?,'MATRIZ','Empresa teste','12345678000195',true)",UUID.randomUUID(),c.unidade());
        atualizar(c).andExpect(jsonPath("$.etapasConcluidas[0]").value(true)).andExpect(jsonPath("$.etapasConcluidas[2]").value(false));
        banco.update("update empresa set ativo=false where unidade_id=?",c.unidade());
        atualizar(c).andExpect(jsonPath("$.etapasConcluidas[0]").value(true));
        mvc.perform(put("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())
            .contentType(MediaType.APPLICATION_JSON).content("{\"prefixoOrcamento\":\"ORC\",\"digitosOrcamento\":1,\"prefixoPeca\":\"PC\",\"separadorPeca\":\"-\",\"digitosPeca\":4}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())
            .contentType(MediaType.APPLICATION_JSON).content("{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[]}"))
            .andExpect(status().isCreated());
        atualizar(c).andExpect(jsonPath("$.etapasConcluidas[2]").value(true)).andExpect(jsonPath("$.etapasConcluidas[5]").value(true));
    }
    @Test void compartilhaProgressoMasMantemApresentacaoPorUsuarioEUnidade() throws Exception {
        var c=contexto(); atualizar(c);
        banco.update("update onboarding_unidade set etapas_concluidas=255 where unidade_id=?",c.unidade());
        preferencias(c,"{\"apresentacaoVista\":true,\"conclusaoDispensada\":true}").andExpect(status().isOk()).andExpect(jsonPath("$.conclusaoDispensada").value(true));
        var convidado=new Contexto(usuario(unidades.findById(c.unidade()).orElseThrow(),Perfil.CONSULTA),c.unidade());
        atualizar(convidado).andExpect(status().isOk()).andExpect(jsonPath("$.etapasConcluidas[7]").value(true))
            .andExpect(jsonPath("$.apresentacaoVista").value(false)).andExpect(jsonPath("$.conclusaoDispensada").value(false));
        atualizar(c).andExpect(jsonPath("$.apresentacaoVista").value(true));
    }
    @Test void naoPermiteDispensarConclusaoAntesDeCompletarNemForjarEtapas() throws Exception {
        var c=contexto();
        preferencias(c,"{\"apresentacaoVista\":true,\"conclusaoDispensada\":true}").andExpect(status().isConflict());
        preferencias(c,"{\"apresentacaoVista\":true,\"conclusaoDispensada\":false,\"etapasConcluidas\":[true,true,true,true,true,true,true,true]}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.etapasConcluidas[0]").value(false));
        atualizar(c).andExpect(jsonPath("$.etapasConcluidas[0]").value(false));
    }
    @Test void bloqueiaUnidadeDeOutroUsuarioEExigeSelecao() throws Exception {
        var a=contexto(); var b=contexto();
        atualizar(new Contexto(a.token(),b.unidade())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/unidades/atual/onboarding/atualizar").header("Authorization","Bearer "+a.token())).andExpect(status().isForbidden());
        atualizar(b).andExpect(status().isOk()).andExpect(jsonPath("$.etapasConcluidas[0]").value(false));
    }
    @Test void cadastraPagamentoSemConfiguracaoEmpresaEAtualizaOnboarding() throws Exception {
        var c=contexto();
        String rota="/api/v1/unidades/atual/formas-pagamento";
        mvc.perform(get(rota).header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.formasRecebimento.length()").value(0));
        String dados="{\"formasRecebimento\":[{\"tipo\":\"PIX\",\"nomeExibicao\":\"Pix\",\"instrucoes\":\"À vista\",\"ativa\":true,\"padrao\":true}]}";
        mvc.perform(put(rota).header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(dados))
            .andExpect(status().isOk()).andExpect(jsonPath("$.formasRecebimento[0].nomeExibicao").value("Pix"));
        org.assertj.core.api.Assertions.assertThat(banco.queryForObject("select count(*) from configuracao_unidade where unidade_id=?",Integer.class,c.unidade())).isZero();
        mvc.perform(get("/api/v1/unidades/atual/formas-recebimento").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].nomeExibicao").value("Pix"));
        atualizar(c).andExpect(jsonPath("$.etapasConcluidas[1]").value(true));
        mvc.perform(put(rota).header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(dados.replace("\"padrao\":true","\"padrao\":false")))
            .andExpect(status().isBadRequest());
        var consulta=new Contexto(usuario(unidades.findById(c.unidade()).orElseThrow(),Perfil.CONSULTA),c.unidade());
        mvc.perform(get(rota).header("Authorization","Bearer "+consulta.token()).header("X-Unidade-Id",c.unidade())).andExpect(status().isOk());
        mvc.perform(put(rota).header("Authorization","Bearer "+consulta.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(dados)).andExpect(status().isForbidden());
        var outra=contexto();
        mvc.perform(get(rota).header("Authorization","Bearer "+outra.token()).header("X-Unidade-Id",c.unidade())).andExpect(status().isForbidden());
    }
}
