package br.com.gestaoservicos;

import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OperacoesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void modulosRetiradosNaoExpoemEndpoints() throws Exception {
        String token = login("legado.removido@test.com");
        UUID unidade = unidade(token, "Sem modulos antigos");
        for (String rota : new String[]{"/api/v1/despesas", "/api/v1/relatorios/resumo?de=2026-10-01&ate=2026-10-09",
                "/api/v1/relatorios/servicos?de=2026-10-01&ate=2026-10-09",
                "/api/v1/relatorios/resumo.csv?de=2026-10-01&ate=2026-10-09",
                "/api/v1/relatorios/resumo.pdf?de=2026-10-01&ate=2026-10-09"}) {
            mvc.perform(get(rota).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(post("/api/v1/despesas").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void periodoOcupaTodosOsDiasInclusiveEntreMesesERejeitaFimAnterior() throws Exception {
        String token = login("operacoes.periodo@test.com");
        UUID unidade = unidade(token, "Periodo");
        UUID cliente = cliente(token, unidade);
        UUID id = servico(token, unidade, cliente, "Servico longo");
        mvc.perform(put("/api/v1/servicos/{id}/agendamento", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataInicio\":\"2026-09-29\",\"dataFim\":\"2026-10-02\",\"responsavel\":\"Carlos\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataInicio").value("2026-09-29"))
                .andExpect(jsonPath("$.dataFim").value("2026-10-02"));
        for (String dia : new String[]{"2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02"}) {
            String ate = java.time.LocalDate.parse(dia).plusDays(1).toString();
            mvc.perform(get("/api/v1/agenda?de=" + dia + "T03:00:00Z&ate=" + ate + "T03:00:00Z")
                    .header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        }
        mvc.perform(get("/api/v1/agenda?de=2026-10-03T03:00:00Z&ate=2026-10-04T03:00:00Z")
                .header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(put("/api/v1/servicos/{id}/agendamento", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataInicio\":\"2026-10-05\",\"dataFim\":\"2026-10-02\",\"responsavel\":\"Carlos\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/servicos/{id}", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(jsonPath("$.dataFim").value("2026-10-02"));
    }

    @Test
    void agendaPermiteVariosServicosNoDiaSemVazarUnidade() throws Exception {
        String token = login("operacoes.fluxo@test.com");
        UUID unidade = unidade(token, "Operacoes Fluxo");
        UUID cliente = cliente(token, unidade);
        UUID primeiro = servico(token, unidade, cliente, "Primeiro");
        UUID segundo = servico(token, unidade, cliente, "Segundo");
        mvc.perform(get("/api/v1/servicos/{id}", primeiro).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(jsonPath("$.codigo").value("SV-1"));
        mvc.perform(get("/api/v1/servicos/{id}", segundo).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(jsonPath("$.codigo").value("SV-2"));
        String horario = "{\"dataProgramada\":\"2026-09-29\",\"responsavel\":\"Carlos\"}";
        mvc.perform(put("/api/v1/servicos/{id}/agendamento", primeiro).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(horario))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGENDADO"));
        mvc.perform(put("/api/v1/servicos/{id}/agendamento", segundo).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(horario))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataProgramada").value("2026-09-29"));
        mvc.perform(get("/api/v1/agenda?de=2026-09-29T03:00:00Z&ate=2026-09-30T03:00:00Z").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(post("/api/v1/servicos/{id}/iniciar", primeiro).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        mvc.perform(post("/api/v1/servicos/{id}/concluir", primeiro).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content("{\"resumoConclusao\":\"Finalizado e testado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDO"));
        String outroToken = login("operacoes.outra@test.com");
        UUID outra = unidade(outroToken, "Operacoes Outra");
        mvc.perform(get("/api/v1/servicos/{id}", primeiro).header("Authorization", bearer(outroToken)).header("X-Unidade-Id", outra)).andExpect(status().isNotFound());
    }

    @Test
    void contratoNasceDeOrcamentoAprovadoEEmitePdfComTodasAsPaginas() throws Exception {
        String token = login("operacoes.contrato@test.com");
        UUID unidade = unidade(token, "Operacoes Contrato");
        UUID cliente = cliente(token, unidade);
        mvc.perform(put("/api/v1/unidades/atual/configuracao").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeRazaoSocial\":\"Prestadora\",\"documento\":\"52998224725\",\"enderecoCompleto\":\"Rua A\",\"formasRecebimento\":[{\"tipo\":\"PIX\",\"nomeExibicao\":\"Pix\",\"instrucoes\":\"chave\",\"ativa\":true,\"padrao\":true}]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/unidades/atual/formas-recebimento").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nomeExibicao").value("Pix"));
        String orcamento = mvc.perform(post("/api/v1/orcamentos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contratanteId\":\"" + cliente + "\",\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"tipo\":\"MAO_DE_OBRA\",\"descricao\":\"Visita\",\"unidadeMedida\":\"HORA\",\"quantidade\":1,\"valorUnitario\":100}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID orcamentoId = UUID.fromString(json.readTree(orcamento).get("id").asText());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir", orcamentoId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/orcamentos/{id}/aprovar", orcamentoId).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade)).andExpect(status().isOk());
        String clausulas = "Condição especial. ".repeat(300);
        String corpo = json.writeValueAsString(new Contrato(orcamentoId, "Prestação de serviços", "Serviço industrial", clausulas, "2026-09-29", "2027-03-29"));
        String criado = mvc.perform(post("/api/v1/contratos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURA")).andExpect(jsonPath("$.numero").value("CT-1")).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(criado).get("id").asText());
        mvc.perform(post("/api/v1/contratos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
        byte[] pdf = mvc.perform(get("/api/v1/contratos/{id}/pdf", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        try (var documento = Loader.loadPDF(pdf)) { assertThat(documento.getNumberOfPages()).isGreaterThan(1); }
        mvc.perform(post("/api/v1/contratos/{id}/enviar-assinatura", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/contratos/{id}/registrar-assinatura", id).header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assinadoPor\":\"Carlos\",\"assinadoEm\":\"2026-09-29\",\"canalAssinatura\":\"Eletrônica\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void painelConsideraRevisaoAtualPeriodoEUnidade() throws Exception {
        String token=login("painel.real@test.com");UUID unidade=unidade(token,"Painel real");
        String corpo="{\"exibirAssinatura\":false,\"descontoTipo\":\"VALOR\",\"descontoValor\":0,\"acrescimoTipo\":\"VALOR\",\"acrescimoValor\":0,\"itens\":[{\"tipo\":\"MAO_DE_OBRA\",\"descricao\":\"Visita\",\"unidadeMedida\":\"HORA\",\"quantidade\":1,\"valorUnitario\":100}]}";
        var criado=mvc.perform(post("/api/v1/orcamentos").header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String hoje=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        String rota="/api/v1/painel?de="+hoje+"&ate="+hoje;
        mvc.perform(get(rota).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.orcamentos").value(1)).andExpect(jsonPath("$.valorOrcamentos").value(100)).andExpect(jsonPath("$.aprovados").value(0)).andExpect(jsonPath("$.evolucao.length()").value(1));
        UUID id=UUID.fromString(json.readTree(criado).get("id").asText());
        mvc.perform(post("/api/v1/orcamentos/{id}/emitir",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/orcamentos/{id}",id).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade).contentType(MediaType.APPLICATION_JSON).content(corpo.replace("100","200"))).andExpect(status().isOk());
        mvc.perform(get(rota).header("Authorization",bearer(token)).header("X-Unidade-Id",unidade)).andExpect(status().isOk()).andExpect(jsonPath("$.orcamentos").value(1)).andExpect(jsonPath("$.valorOrcamentos").value(200));
        String outro=login("painel.outra@test.com");UUID outra=unidade(outro,"Painel vazio");
        mvc.perform(get(rota).header("Authorization",bearer(outro)).header("X-Unidade-Id",outra)).andExpect(status().isOk()).andExpect(jsonPath("$.orcamentos").value(0)).andExpect(jsonPath("$.recentes.length()").value(0));
    }

    private UUID cliente(String token, UUID unidade) throws Exception {
        String corpo = "{\"tipo\":\"PF\",\"nomeRazaoSocial\":\"Cliente Operacional\",\"documento\":\"12345678901\",\"contatos\":[{\"nome\":\"Principal\",\"telefone\":\"11999999999\"}]}";
        return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/contratantes").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
    }
    private UUID servico(String token, UUID unidade, UUID cliente, String titulo) throws Exception {
        return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/servicos").header("Authorization", bearer(token)).header("X-Unidade-Id", unidade).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contratanteId\":\"" + cliente + "\",\"titulo\":\"" + titulo + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
    }
    private String login(String email) throws Exception {
        mvc.perform(post("/api/v1/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Cadastro("Ana", email, "password123")))).andExpect(status().isCreated());
        return json.readTree(mvc.perform(post("/api/v1/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Login(email, "password123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("tokenAcesso").asText();
    }
    private UUID unidade(String token, String nome) throws Exception {
        return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/unidades").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Nome(nome))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
    }
    private String bearer(String token) { return "Bearer " + token; }
    record Cadastro(String nome, String email, String senha) {}
    record Login(String email, String senha) {}
    record Nome(String nome) {}
    record Contrato(UUID orcamentoId, String modelo, String objeto, String clausulasAdicionais, String inicioVigencia, String fimVigencia) {}
}
