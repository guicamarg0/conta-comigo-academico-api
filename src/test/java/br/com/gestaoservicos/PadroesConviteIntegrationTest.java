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
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.acesso.cadastroPublico=false") @AutoConfigureMockMvc
class PadroesConviteIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios; @Autowired UnidadeRepository unidades;
    @Autowired AssociacaoRepository associacoes; @Autowired PasswordEncoder senhas;
    record Contexto(String token, UUID unidade) {}
    Contexto contexto() throws Exception {
        String sufixo=UUID.randomUUID().toString(),email="padroes."+sufixo+"@example.com";
        var usuario=usuarios.save(new Usuario("Gestor",email,senhas.encode("senha-teste-123")));
        var unidade=unidades.save(new Unidade("Unidade "+sufixo,"unidade "+sufixo));
        associacoes.save(new Associacao(usuario,unidade,Perfil.ADMIN));
        return new Contexto(login(email),unidade.getId());
    }
    String login(String email) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email,"senha","senha-teste-123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText();
    }
    String convite(Contexto c) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/unidades/atual/convites").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"CONSULTA\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("codigoConvite").asText();
    }
    @Test void conviteCadastraMesmoComCadastroPublicoFechadoEUsaPerfilDaUnidadeUmaVez() throws Exception {
        var c=contexto();String codigo=convite(c),email="convidado."+UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/v1/convites/consultar").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo",codigo))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("CONSULTA"));
        String corpo=json.writeValueAsString(Map.of("codigo",codigo,"nome","Convidado","email",email,"senha","senha-teste-123","perfil","ADMIN"));
        mvc.perform(post("/api/v1/convites/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
        var usuario=usuarios.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senha-teste-123");assertThat(senhas.matches("senha-teste-123",usuario.getSenhaHash())).isTrue();
        assertThat(associacoes.findByUsuarioIdAndUnidadeId(usuario.getId(),c.unidade()).orElseThrow().getPerfil()).isEqualTo(Perfil.CONSULTA);
        mvc.perform(post("/api/v1/convites/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        String token=login(email);
        mvc.perform(get("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+token).header("X-Unidade-Id",c.unidade())).andExpect(status().isOk());
        mvc.perform(put("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+token).header("X-Unidade-Id",c.unidade())
                .contentType(MediaType.APPLICATION_JSON).content(codigos("PROP"))).andExpect(status().isForbidden());
    }
    @Test void conviteInvalidoNaoCriaContaEEmailExistenteNaoConsomeConvite() throws Exception {
        var c=contexto();String codigo=convite(c),email="existente."+UUID.randomUUID()+"@example.com";
        usuarios.save(new Usuario("Existente",email,senhas.encode("senha-teste-123")));
        long antes=usuarios.count();
        var dados=Map.of("codigo",codigo,"nome","Teste","email",email,"senha","senha-teste-123");
        mvc.perform(post("/api/v1/convites/cadastro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/convites/consultar").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo",codigo)))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/convites/cadastro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo","invalido","nome","Teste","email","novo@example.com","senha","senha-teste-123")))).andExpect(status().isNotFound());
        assertThat(usuarios.count()).isEqualTo(antes);
        mvc.perform(post("/api/v1/convites/aceitar").header("Authorization","Bearer "+login(email)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo",codigo)))).andExpect(status().isNoContent());
    }
    String codigos(String prefixo) throws Exception {return json.writeValueAsString(Map.of("prefixoOrcamento",prefixo,"digitosOrcamento",3,"prefixoPeca","IT","separadorPeca","_","digitosPeca",2));}
    @Test void codigosSaoPersistidosPorUnidadeAplicadosSemRenumerarHistorico() throws Exception {
        var c=contexto();var outra=contexto();
        String orcamento="{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[]}";
        String id=json.readTree(mvc.perform(post("/api/v1/orcamentos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(orcamento))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.numero").value("ORC-1")).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(put("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(codigos("PROP"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())).andExpect(jsonPath("$.prefixoOrcamento").value("PROP"));
        mvc.perform(put("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(codigos("PROP"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+outra.token()).header("X-Unidade-Id",outra.unidade())).andExpect(jsonPath("$.prefixoOrcamento").value("ORC"));
        mvc.perform(post("/api/v1/orcamentos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(orcamento)).andExpect(jsonPath("$.numero").value("PROP-002"));
        mvc.perform(get("/api/v1/orcamentos/"+id).header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())).andExpect(jsonPath("$.numero").value("ORC-1"));
        mvc.perform(post("/api/v1/catalogo-itens").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"PECA\",\"nome\":\"Peça\",\"unidadeMedida\":\"UNIDADE\",\"valorPadrao\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigoReferencia").value("IT_01"));
        String item="{\"tipo\":\"PECA\",\"nome\":\"Peça importada\",\"unidadeMedida\":\"UNIDADE\",\"valorPadrao\":10,\"codigoReferencia\":\"IT_02\"}";
        mvc.perform(post("/api/v1/catalogo-itens").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(item)).andExpect(status().isCreated());
        String automatico=item.replace("IT_02", "");
        String itemId=json.readTree(mvc.perform(post("/api/v1/catalogo-itens").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(automatico))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigoReferencia").value("IT_03")).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(put("/api/v1/catalogo-itens/"+itemId).header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(automatico))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoReferencia").value("IT_03"));

    }
    @Test void validacaoRetornaTodosOsCamposEPeriodoInvalidoNaoGravaServico() throws Exception {
        var c=contexto();
        mvc.perform(put("/api/v1/unidades/atual/codigos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"prefixoOrcamento\":\"\",\"digitosOrcamento\":0,\"prefixoPeca\":\"\",\"separadorPeca\":\"/\",\"digitosPeca\":9}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.prefixoOrcamento").exists()).andExpect(jsonPath("$.campos.digitosOrcamento").exists());
        String cliente=json.readTree(mvc.perform(post("/api/v1/contratantes").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"PJ\",\"nomeRazaoSocial\":\"Cliente\",\"documento\":\"11222333000181\",\"contatos\":[{\"nome\":\"Contato\",\"email\":\"contato@example.com\"}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        String invalido=json.writeValueAsString(Map.of("contratanteId",cliente,"titulo","Visita","responsavel","Ana","dataInicio","2026-10-03","dataFim","2026-10-01"));
        mvc.perform(post("/api/v1/servicos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/servicos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade())).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(post("/api/v1/servicos").header("Authorization","Bearer "+c.token()).header("X-Unidade-Id",c.unidade()).contentType(MediaType.APPLICATION_JSON).content(invalido.replace("2026-10-01","2026-10-05")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("SV-1")).andExpect(jsonPath("$.status").value("AGENDADO")).andExpect(jsonPath("$.dataFim").value("2026-10-05"));
    }
}
