package br.com.gestaoservicos.orcamento.service;

import br.com.gestaoservicos.compartilhado.GeradorPdf;
import br.com.gestaoservicos.orcamento.model.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GeradorPdfOrcamento extends GeradorPdf<RevisaoOrcamento> {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final ObjectMapper json;
    public GeradorPdfOrcamento(ObjectMapper json) { this.json = json; }

    // Mantém a assinatura usada pelo OrcamentoService e pelos testes.
    public byte[] gerar(Orcamento orcamento, RevisaoOrcamento revisao) {
        return gerar(revisao);
    }

    @Override
    protected byte[] renderizarConteudo(RevisaoOrcamento revisao) throws Exception {
        var orcamento = revisao.getOrcamento();
        var emitente = ler(revisao.getContratadoSnapshot());
        var cliente = ler(revisao.getContratanteSnapshot());
        StringBuilder itens = new StringBuilder();
        int numero = 1;
        String ultimoItem = "";
        for (var item : revisao.getItens()) {
            var linha = new StringBuilder("<tr><td class='centro'>").append(numero++)
                    .append("</td><td>").append(texto(item.getDescricao()))
                    .append("</td><td class='centro'>").append(decimal(item.getQuantidade()))
                    .append("</td><td class='valor'>").append(moeda(item.getValorUnitario()))
                    .append("</td><td class='valor'>").append(moeda(item.getTotal())).append("</td></tr>");
            if (numero > revisao.getItens().size()) ultimoItem = linha.toString();
            else itens.append(linha);
        }
        String ajustes = "";
        if (revisao.getDescontoTotal().signum() != 0 || revisao.getAcrescimoTotal().signum() != 0) {
            ajustes = total("Subtotal", revisao.getSubtotal(), "")
                    + total("Desconto", revisao.getDescontoTotal().negate(), "")
                    + total("Acréscimo", revisao.getAcrescimoTotal(), "");
        }
        String logo = campo(emitente, "logoUrl", "");
        // Logos enviadas pelo cadastro são incorporadas, sem acesso a URLs externas.
        String marca = logo.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+")
                ? "<img class='logo' src='" + logo + "' alt='' />" : "";
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8" />
            <style>
            @page { size:A4; margin:15mm 15mm 43mm; @bottom-center { content:'Página ' counter(page) ' de ' counter(pages); font-family:'Noto Sans'; font-size:8pt; color:#303A44; } }
            body { font-family:'Noto Sans'; font-size:9pt; color:#303A44; line-height:1.4; }
            .marca { height:24mm; border-bottom:1px solid #CAD1D8; margin-bottom:5px; }
            .logo { max-width:54mm; max-height:22mm; }
            h1 { color:#303A44; font-size:20pt; margin:0 0 6px; }
            .numero { border-bottom:1px solid #CAD1D8; padding-bottom:6px; margin-bottom:8px; font-size:11pt; }
            .codigo { color:#303A44; font-weight:bold; }
            table { width:100%%; border-collapse:collapse; table-layout:fixed; }
            td,th { border:1px solid #CAD1D8; padding:5px 7px; vertical-align:top; word-wrap:break-word; }
            .partes { margin-bottom:12px; page-break-inside:avoid; }
            .partes td { background:#FAFBFC; width:50%%; }
            .rotulo { color:#303A44; font-weight:bold; font-size:8pt; margin-bottom:5px; }
            .nome { font-weight:bold; margin-bottom:2px; }
            .itens { -fs-table-paginate:paginate; }
            .itens th { text-align:center; background:#F2F4F6; font-size:9pt; vertical-align:middle; }
            .itens tr { page-break-inside:avoid; }
            .itens td { font-size:8pt; }
            .centro { text-align:center; } .valor { text-align:right; }
            .total td { font-weight:bold; background:#EEF1F4; font-size:10pt; }
            .total .valor { color:#155B57; }
            h2 { color:#303A44; font-size:10pt; margin:8px 0 5px; page-break-after:avoid; }
            .encerramento { page-break-inside:avoid; }
            .condicoes { border:1px solid #CAD1D8; padding:8px 10px; page-break-inside:avoid; }
            .condicoes p { margin:0 0 6px; }
            </style></head><body>
            <div class="marca">%s</div><h1>ORÇAMENTO</h1>
            <div class="numero"><span class="codigo">Nº %s</span> &#160; | &#160; Data: %s</div>
            <table class="partes"><tr><td><div class="rotulo">EMITENTE</div>%s</td><td><div class="rotulo">CLIENTE</div>%s</td></tr></table>
            %s
            <div class="encerramento">
            <table class="itens"><colgroup><col style="width:7%%"/><col style="width:45%%"/><col style="width:9%%"/><col style="width:19%%"/><col style="width:20%%"/></colgroup>
            <!--CABECALHO_FINAL-->
            <tbody>%s%s%s</tbody></table>
            <h2>CONDIÇÕES COMERCIAIS</h2><div class="condicoes">%s</div>
            </div>
            </body></html>
            """.formatted(marca, texto(orcamento.getNumero()), DATA.format(LocalDate.now()),
                parte(emitente, "enderecoCompleto", "Emitente não informado"),
                parte(cliente, "endereco", "Proposta sem destinatário"), tabelaItens(itens.toString()), ultimoItem, ajustes,
                total("TOTAL DO PEDIDO", revisao.getTotalFinal(), "total"), condicoes(revisao));
        String cabecalho = "<thead><tr><th>Item</th><th>Descrição</th><th>Qtde.</th><th>Valor Unitário</th><th>Valor Total</th></tr></thead>";
        byte[] bytes = renderizar(html.replace("<!--CABECALHO_FINAL-->", itens.isEmpty() ? cabecalho : ""));
        try (var previa = Loader.loadPDF(bytes)) {
            var extrator = new PDFTextStripper();
            extrator.setStartPage(previa.getNumberOfPages());
            extrator.setEndPage(previa.getNumberOfPages());
            String ultimaPagina = extrator.getText(previa);
            if (previa.getNumberOfPages() > 1 && ultimaPagina.contains("TOTAL DO PEDIDO")
                    && !ultimaPagina.contains("Valor Unitário"))
                bytes = renderizar(html.replace("<!--CABECALHO_FINAL-->", cabecalho));
        }
        return bytes;
    }

    @Override
    protected boolean exibirAssinaturas(RevisaoOrcamento revisao) {
        return revisao.isExibirAssinatura();
    }

    @Override
    protected void desenharAssinaturas(PDPageContentStream tela, PDType0Font fonte, PDPage pagina, RevisaoOrcamento revisao) throws java.io.IOException {
        var emitente = ler(revisao.getContratadoSnapshot());
        var cliente = ler(revisao.getContratanteSnapshot());
        assinatura(tela, fonte, 43, campo(emitente, "nomeRazaoSocial", "Emitente"), "Contratada / Emitente");
        assinatura(tela, fonte, 312, campo(cliente, "nomeRazaoSocial", "Contratante"), "Contratante / Cliente");
    }

    @Override
    protected String mensagemErro() {
        return "Não foi possível gerar o PDF do orçamento";
    }

    private byte[] renderizar(String html) throws Exception {
        try (var saida = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf"), "Noto Sans", 400,
                    com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/NotoSans-Bold.ttf"), "Noto Sans", 700,
                    com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(html, null).toStream(saida).run();
            return saida.toByteArray();
        }
    }
    private String tabelaItens(String linhas) {
        if (linhas.isEmpty()) return "";
        return "<table class='itens'><colgroup><col style='width:7%'/><col style='width:45%'/><col style='width:9%'/><col style='width:19%'/><col style='width:20%'/></colgroup>"
                + "<thead><tr><th>Item</th><th>Descrição</th><th>Qtde.</th><th>Valor Unitário</th><th>Valor Total</th></tr></thead><tbody>"
                + linhas + "</tbody></table>";
    }
    private String parte(JsonNode parte, String endereco, String vazio) {
        if (parte == null || parte.isEmpty()) return texto(vazio);
        StringBuilder html = new StringBuilder("<div class='nome'>").append(texto(campo(parte,"nomeRazaoSocial",vazio))).append("</div>");
        String documento = campo(parte,"documento", "");
        if (!documento.isBlank()) html.append("<div>").append(documento.replaceAll("\\D", "").length()==11?"CPF: ":"CNPJ: ").append(texto(documentoFormatado(documento))).append("</div>");
        html.append("<div>").append(texto(campo(parte,endereco,""))).append("</div>");
        String contato = String.join(" · ", java.util.stream.Stream.of(campo(parte,"telefone",""),campo(parte,"email","")).filter(s->!s.isBlank()).toList());
        if(!contato.isBlank()) html.append("<div>").append(texto(contato)).append("</div>");
        for(var c : parte.path("contatos")) html.append("<div>").append(texto(String.join(" · ",java.util.stream.Stream.of(campo(c,"nome",""),campo(c,"telefone",""),campo(c,"email","")).filter(s->!s.isBlank()).toList()))).append("</div>");
        return html.toString();
    }
    private String condicoes(RevisaoOrcamento revisao) {
        StringBuilder html = new StringBuilder();
        if(revisao.getValidade()!=null) html.append("<p><strong>Validade da proposta:</strong> ").append(DATA.format(revisao.getValidade())).append("</p>");
        if(revisao.isExibirPagamento()&&revisao.getCondicoesPagamento()!=null&&!revisao.getCondicoesPagamento().isBlank()) html.append("<p><strong>Condição de pagamento:</strong> ").append(texto(revisao.getCondicoesPagamento())).append("</p>");
        if(revisao.getObservacoesComerciais()!=null&&!revisao.getObservacoesComerciais().isBlank()) html.append("<p>").append(texto(revisao.getObservacoesComerciais())).append("</p>");
        return html.isEmpty()?"<p>Sem observações comerciais.</p>":html.toString();
    }
    private String total(String rotulo,BigDecimal valor,String classe) {return "<tr class='"+classe+"'><td colspan='4' class='valor'>"+rotulo+"</td><td class='valor'>"+moeda(valor)+"</td></tr>";}
    private String documentoFormatado(String valor) {
        String d=valor.replaceAll("\\D", "");
        if(d.length()==14)return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        if(d.length()==11)return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        return valor;
    }
    private void assinatura(PDPageContentStream tela,PDType0Font fonte,float x,String nome,String papel) throws java.io.IOException {
        float largura=240; tela.setStrokingColor(new java.awt.Color(202,209,216));tela.setNonStrokingColor(new java.awt.Color(48,58,68));
        tela.moveTo(x,95);tela.lineTo(x+largura,95);tela.stroke();
        var linhas=new ArrayList<String>();String linha="";
        for(String palavra:nome.split("\\s+")){String nova=linha.isEmpty()?palavra:linha+" "+palavra;if(fonte.getStringWidth(nova)/1000*8>largura&&!linha.isEmpty()){linhas.add(linha);linha=palavra;}else linha=nova;}if(!linha.isEmpty())linhas.add(linha);
        float y=82;for(String l:linhas){tela.beginText();tela.setFont(fonte,8);tela.newLineAtOffset(x,y);tela.showText(l);tela.endText();y-=11;}
        tela.beginText();tela.setFont(fonte,7);tela.newLineAtOffset(x,y);tela.showText(papel);tela.endText();
    }
    private JsonNode ler(String valor) {if(valor==null)return null;try{return json.readTree(valor);}catch(Exception e){throw new IllegalStateException("Snapshot inválido",e);}}
    private String campo(JsonNode n,String chave,String padrao){return n==null?padrao:n.path(chave).asText(padrao);}
    private String texto(String valor){return Entities.escape(Objects.toString(valor, ""), new org.jsoup.nodes.Document.OutputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml)).replace("\r", "").replace("\n", "<br />");}
    private String moeda(BigDecimal valor){return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);}
    private String decimal(BigDecimal valor){var formato=NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));formato.setMaximumFractionDigits(2);return formato.format(valor);}
}