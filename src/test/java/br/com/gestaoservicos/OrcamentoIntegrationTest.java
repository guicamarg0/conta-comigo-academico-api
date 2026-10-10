package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import br.com.gestaoservicos.orcamento.repository.RevisaoOrcamentoRepository;
import tools.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrcamentoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RevisaoOrcamentoRepository revisoes;

    @Test
    void baixaPdfDoRascunhoSemEmitirERefleteEdicoes() throws Exception {
        String token=login("orcamento.rascunho.pdf@test.com");
        UUID unidade=unidade(token,"Unidade PDF Rascunho");
        UUID id=criarRascunho(token,unidade,10);
        String rota="/api/v1/orcamentos/{id}/revisoes/1/pdf";
        byte[] primeiro=mvc.perform(get(rota,id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        try(var doc=Loader.loadPDF(primeiro)){assertThat(new PDFTextStripper().getText(doc)).contains("Visita", "10,00");}
        mvc.perform(put("/api/v1/orcamentos/{id}",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)
            .contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(20))).andExpect(status().isOk());
        byte[] segundo=mvc.perform(get(rota,id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var doc=Loader.loadPDF(segundo)){assertThat(new PDFTextStripper().getText(doc)).contains("20,00");}
        mvc.perform(get("/api/v1/orcamentos/{id}",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RASCUNHO"))
            .andExpect(jsonPath("$.revisaoAtual").value(1)).andExpect(jsonPath("$.historico.length()").value(2));
        assertThat(revisoes.findByOrcamentoIdAndNumeroRevisao(id,1).orElseThrow().getContratadoSnapshot()).isNull();
        String outro=login("orcamento.rascunho.pdf.outra@test.com");
        UUID outra=unidade(outro,"Outra unidade PDF Rascunho");
        mvc.perform(get(rota,id).header("Authorization",bearer(outro)).header("X-Unidade-Id",outra)).andExpect(status().isNotFound());
    }

    @Test
    void emiteEAbreNovaRevisao() throws Exception {
        String token = login("orcamento.fluxo@test.com");
        UUID unidade = unidade(token, "Unidade Fluxo Orcamento");
        UUID id = criarRascunho(token, unidade, 100);

        mvc.perform(post("/api/v1/orcamentos/{id}/emitir", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EMITIDO"));
        mvc.perform(post("/api/v1/orcamentos/{id}/nova-revisao", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.revisaoAtual").value(2));
    }

    @Test
    void editarEmitidoCriaNovaRevisaoAutomaticamente() throws Exception {
        String token = login("orcamento.edicao@test.com");
        UUID unidade = unidade(token, "Unidade Edicao Orcamento");
        UUID id = criarRascunho(token, unidade, 10);
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/orcamentos/{id}", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(20)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.revisaoAtual").value(2));
    }

    @Test
    void operadorNaoPodeEditarOrcamentoEmitido() throws Exception {
        String tokenAdmin = login("orcamento.admin.perfil@test.com");
        UUID unidade = unidade(tokenAdmin, "Unidade Perfil Orcamento");
        UUID id = criarRascunho(tokenAdmin, unidade, 10);
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir", id)
                        .header("Authorization", bearer(tokenAdmin)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk());

        String emailOperador = "orcamento.operador.perfil@test.com";
        String tokenOperador = login(emailOperador);
        mvc.perform(post("/api/v1/unidades/atual/usuarios")
                        .header("Authorization", bearer(tokenAdmin)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailOperador + "\",\"perfil\":\"OPERADOR\"}"))
                .andExpect(status().isCreated());

        mvc.perform(put("/api/v1/orcamentos/{id}", id)
                        .header("Authorization", bearer(tokenOperador)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(20)))
                .andExpect(status().isForbidden());
    }

    @Test
    void geraNumerosDistintosEmCriacoesConcorrentes() throws Exception {
        String token = login("orcamento.concorrencia@test.com");
        UUID unidade = unidade(token, "Unidade Concorrencia Orcamento");
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = List.of(1, 2).stream().map(ignorado -> executor.submit(() -> {
                inicio.await();
                return mvc.perform(post("/api/v1/orcamentos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                                .contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(10)))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            })).toList();
            inicio.countDown();
            Set<String> numeros = new HashSet<>();
            for (var tarefa : tarefas) numeros.add(json.readTree(tarefa.get()).get("numero").asText());
            assertThat(numeros).hasSize(2);
            assertThat(numeros).allMatch(n->n.matches("ORC-[1-9][0-9]*"));
        }
    }

    @Test
    void previaEConfirmacaoAtualizamSomenteItemVinculadoAoCatalogo() throws Exception {
        String token = login("orcamento.catalogo@test.com"); UUID unidade = unidade(token, "Unidade Catalogo Orcamento");
        String item = mvc.perform(post("/api/v1/catalogo-itens").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MATERIAL\",\"nome\":\"Cabo\",\"unidadeMedida\":\"METRO\",\"valorPadrao\":10}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID catalogoId=UUID.fromString(json.readTree(item).get("id").asText());
        String corpo="{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"itemCatalogoId\":\""+catalogoId+"\",\"quantidade\":2},{\"tipo\":\"MATERIAL\",\"descricao\":\"Manual\",\"unidadeMedida\":\"UNIDADE\",\"quantidade\":1,\"valorUnitario\":5}]}";
        String criado=mvc.perform(post("/api/v1/orcamentos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(json.readTree(criado).get("id").asText());
        mvc.perform(put("/api/v1/catalogo-itens/{id}",catalogoId).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"MATERIAL\",\"nome\":\"Cabo\",\"unidadeMedida\":\"METRO\",\"valorPadrao\":12}")) .andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos/{id}/atualizacao-catalogo/previa",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.mudancas.length()").value(1)).andExpect(jsonPath("$.mudancas[0].valorUnitarioNovo").value(12));
        mvc.perform(post("/api/v1/orcamentos/{id}/atualizacao-catalogo/confirmar",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.revisaoAtualDetalhe.itens[0].valorUnitario").value(12)).andExpect(jsonPath("$.revisaoAtualDetalhe.itens[1].valorUnitario").value(5));
    }

    @Test
    void rejeitaTotalNegativoETransicaoInvalida() throws Exception {
        String token=login("orcamento.regras@test.com"); UUID unidade=unidade(token,"Unidade Regras Orcamento");
        mvc.perform(post("/api/v1/orcamentos").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content(corpoRascunhoComDesconto(10,11))).andExpect(status().isConflict());
        UUID id=criarRascunho(token,unidade,10);
        mvc.perform(post("/api/v1/orcamentos/{id}/aprovar",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isConflict());
    }

    @Test
    void impedeAcessoDeOutraUnidadeEPreparaSnapshotsNaEmissao() throws Exception {
        String token=login("orcamento.snapshot@test.com"); UUID unidade=unidade(token,"Unidade Snapshot Orcamento"); UUID id=criarRascunho(token,unidade,10);
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content("{\"nomeRazaoSocial\":\"Prestadora\",\"documento\":\"52998224725\",\"enderecoCompleto\":\"Rua A\",\"formasRecebimento\":[{\"tipo\":\"PIX\",\"nomeExibicao\":\"PIX\",\"instrucoes\":\"chave\",\"ativa\":true,\"padrao\":true}]}")) .andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        assertThat(revisoes.findByOrcamentoIdAndNumeroRevisao(id,1).orElseThrow().getContratadoSnapshot()).contains("Prestadora");
        String outro=login("orcamento.outra.unidade@test.com"); UUID outra=unidade(outro,"Outra Unidade Orcamento");
        mvc.perform(get("/api/v1/orcamentos/{id}",id).header("Authorization",bearer(outro)).header("X-Unidade-Id",outra)).andExpect(status().isNotFound());
    }

    @Test
    void preservaPdfGenericoPorRevisaoEBloqueiaOutraUnidade() throws Exception {
        String token=login("orcamento.pdf@test.com"); UUID unidade=unidade(token,"Unidade PDF"); UUID id=criarRascunho(token,unidade,10);
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        byte[] primeiro=mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(primeiro,java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF");
        try (var documento=Loader.loadPDF(primeiro)) { assertThat(new PDFTextStripper().getText(documento)).contains("ORÇAMENTO", "Proposta sem destinatário", "CONDIÇÕES COMERCIAIS", "TOTAL DO PEDIDO"); }
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content("{\"nomeRazaoSocial\":\"Nova Prestadora\",\"documento\":\"52998224725\",\"enderecoCompleto\":\"Rua B\",\"formasRecebimento\":[{\"tipo\":\"PIX\",\"nomeExibicao\":\"PIX\",\"instrucoes\":\"chave\",\"ativa\":true,\"padrao\":true}]}")) .andExpect(status().isOk());
        byte[] novamente=mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(novamente).isEqualTo(primeiro);
        String outro=login("orcamento.pdf.outra@test.com"); UUID outra=unidade(outro,"Outra PDF");
        mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(outro)).header("X-Unidade-Id",outra)).andExpect(status().isNotFound());
    }

    @Test
    void preservaPdfIndependenteParaCadaRevisaoEmitida() throws Exception {
        String token=login("orcamento.pdf.revisoes@test.com"); UUID unidade=unidade(token,"Unidade PDFs Revisoes"); UUID id=criarRascunho(token,unidade,10);
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        byte[] primeira=mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        mvc.perform(post("/api/v1/orcamentos/{id}/nova-revisao",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/orcamentos/{id}",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(20))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        byte[] segunda=mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/2/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(segunda).isNotEqualTo(primeira);
        assertThat(mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andReturn().getResponse().getContentAsByteArray()).isEqualTo(primeira);
    }

    @Test
    void criaNovasPaginasParaConteudoExtenso() throws Exception {
        String token=login("orcamento.pdf.paginas@test.com"); UUID unidade=unidade(token,"Unidade PDF Paginas");
        String observacoes="texto ".repeat(650); String corpo=corpoRascunho(10).replace("\"exibirAssinatura\":false","\"observacoesComerciais\":\""+observacoes+"\",\"exibirAssinatura\":false");
        String criado=mvc.perform(post("/api/v1/orcamentos").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(); UUID id=UUID.fromString(json.readTree(criado).get("id").asText());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()); byte[] pdf=mvc.perform(get("/api/v1/orcamentos/{id}/revisoes/1/pdf",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var documento=Loader.loadPDF(pdf)){assertThat(documento.getNumberOfPages()).isGreaterThan(1);}
    }

    @Test
    void persisteEmpresaEmitenteCopiaRevisaoEBloqueiaEmpresaDeOutraUnidade() throws Exception {
        String token=login("orcamento.empresa@test.com"); UUID unidade=unidade(token,"Unidade Empresa PDF");
        var logoBytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12,12,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",logoBytes);
        String logo="data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(logoBytes.toByteArray());
        String empresa=mvc.perform(post("/api/v1/empresas").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("tipo","MATRIZ","razaoSocial","Emitente Escolhida LTDA","cnpj","63047223000124","cidade","Curitiba","estado","PR","logoUrl",logo))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String empresaId=json.readTree(empresa).get("id").asText();
        String corpo=corpoRascunho(10).replace("{\"exibirAssinatura\"", "{\"empresaId\":\""+empresaId+"\",\"exibirAssinatura\"");
        String criado=mvc.perform(post("/api/v1/orcamentos").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)
            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.revisaoAtualDetalhe.empresaId").value(empresaId)).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(json.readTree(criado).get("id").asText());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        String snapshot=revisoes.findByOrcamentoIdAndNumeroRevisao(id,1).orElseThrow().getContratadoSnapshot();
        assertThat(snapshot).contains("Emitente Escolhida LTDA", "Curitiba", logo);
        mvc.perform(put("/api/v1/empresas/{id}",empresaId).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("tipo","MATRIZ","razaoSocial","Emitente Escolhida LTDA","cnpj","63047223000124","logoUrl","data:image/png;base64,AAAA"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/orcamentos/{id}/nova-revisao",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revisaoAtualDetalhe.empresaId").value(empresaId));
        mvc.perform(post("/api/v1/orcamentos/{id}/duplicar",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revisaoAtualDetalhe.empresaId").value(empresaId));
        UUID outra=unidade(token,"Outra Unidade Empresa PDF");
        mvc.perform(post("/api/v1/orcamentos").header("Authorization",bearer(token)).header("X-Unidade-Id",outra)
            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isNotFound());
    }

    private UUID criarRascunho(String token, UUID unidade, int valor) throws Exception {
        String resposta = mvc.perform(post("/api/v1/orcamentos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoRascunho(valor)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(resposta).get("id").asText());
    }

    private String corpoRascunho(int valor) {
        return "{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"tipo\":\"MAO_DE_OBRA\",\"descricao\":\"Visita\",\"unidadeMedida\":\"HORA\",\"quantidade\":1,\"valorUnitario\":" + valor + "}]}";
    }
    private String corpoRascunhoComDesconto(int valor, int desconto) { return "{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":"+desconto+",\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"tipo\":\"MAO_DE_OBRA\",\"descricao\":\"Visita\",\"unidadeMedida\":\"HORA\",\"quantidade\":1,\"valorUnitario\":"+valor+"}]}"; }

    private String login(String email) throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Cadastro("Ana", email, "password123"))))
                .andExpect(status().isCreated());
        String resposta = mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(email, "password123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("tokenAcesso").asText();
    }

    private UUID unidade(String token, String nome) throws Exception {
        String resposta = mvc.perform(post("/api/v1/unidades").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Nome(nome))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(resposta).get("id").asText());
    }

    private String bearer(String token) { return "Bearer " + token; }
    record Cadastro(String nome, String email, String senha) {}
    record Login(String email, String senha) {}
    record Nome(String nome) {}
}
