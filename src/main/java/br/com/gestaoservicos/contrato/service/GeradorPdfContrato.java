package br.com.gestaoservicos.contrato.service;

import br.com.gestaoservicos.contrato.model.Contrato;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class GeradorPdfContrato {
    public byte[] gerar(Contrato contrato) {
        try (var saida = new ByteArrayOutputStream()) {
            String html = """
                <!DOCTYPE html><html><head><meta charset="UTF-8" />
                <style>
                  @page { size: A4; margin: 22mm 18mm 48mm; }
                  body { font-family: 'Noto Sans'; color: #203453; font-size: 12pt; line-height: 1.5; }
                  .cabecalho { border-bottom: 1px solid #CAD1D8; padding-bottom: 9px; margin-bottom: 22px; }
                  .cabecalho h1 { font-size: 16pt; margin: 0; color: #0a2350; }
                  .cabecalho p { color: #506990; font-size: 9pt; margin: 3px 0 0; }
                  h1 { font-size: 18pt; } h2 { font-size: 15pt; } h3 { font-size: 12pt; }
                  p { margin: 0 0 10px; } ul, ol { margin: 0 0 12px 20px; padding: 0; }
                  li { margin: 0 0 4px; } strong, b { font-weight: bold; } em, i { font-style: italic; }
                  table { border-collapse: collapse; width: 100%%; } td, th { border: 1px solid #cad5e5; padding: 6px; }
                </style></head><body>
                  <header class="cabecalho"><h1>Contrato %s</h1><p>Orçamento #%s</p></header>
                  <main>%s</main>
                </body></html>""".formatted(escapar(contrato.getNumero()), escapar(contrato.getOrcamento().getNumero()), normalizarHtml(br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatarNoTexto(contrato.getConteudoRascunho(), contrato.getContratante().getDocumento(), contrato.getEmpresa() == null ? null : contrato.getEmpresa().getCnpj())));
            var builder = new PdfRendererBuilder();
            for (String familia : List.of("NotoSans", "NotoSerif", "NotoSansMono")) {
                for (String estilo : List.of("Regular", "Bold", "Italic", "BoldItalic")) {
                    if (familia.equals("NotoSansMono") && estilo.contains("Italic")) continue;
                    String recurso = "/fonts/" + familia + "-" + estilo + ".ttf";
                    String nome = familia.equals("NotoSerif") ? "Noto Serif" : familia.equals("NotoSansMono") ? "Noto Sans Mono" : "Noto Sans";
                    builder.useFont(() -> getClass().getResourceAsStream(recurso), nome,
                            estilo.contains("Bold") ? 700 : 400, estilo.contains("Italic") ? FontStyle.ITALIC : FontStyle.NORMAL, true);
                }
            }
            builder.withHtmlContent(html, null).toStream(saida).run();
            // Reserva espaço no layout e desenha apenas na última página, sem repetir assinaturas.
            try (var documento = Loader.loadPDF(saida.toByteArray());
                 var fonteStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf");
                 var finalizado = new ByteArrayOutputStream()) {
                br.com.gestaoservicos.compartilhado.PapelTimbrado.aplicar(documento);
                var pagina = documento.getPage(documento.getNumberOfPages() - 1);
                var fonte = PDType0Font.load(documento, fonteStream);
                try (var stream = new PDPageContentStream(documento, pagina, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    float margem = 51, largura = (pagina.getMediaBox().getWidth() - margem * 2 - 32) / 2;
                    desenharAssinatura(stream, fonte, margem, largura, nomeCliente(contrato), "Contratante");
                    desenharAssinatura(stream, fonte, margem + largura + 32, largura, nomeEmpresa(contrato), "Contratada");
                }
                documento.save(finalizado);
                return finalizado.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do contrato", e);
        }
    }

    private void desenharAssinatura(PDPageContentStream stream, PDType0Font fonte, float x, float largura, String nome, String papel) throws java.io.IOException {
        stream.setStrokingColor(new java.awt.Color(70, 70, 70)); stream.setNonStrokingColor(new java.awt.Color(40, 40, 40));
        stream.moveTo(x, 106); stream.lineTo(x + largura, 106); stream.stroke();
        float tamanho = 9;
        // Nomes extensos são distribuídos em linhas abaixo do campo, sem invadir o conteúdo.
        var linhas = new java.util.ArrayList<String>(); String linha = "";
        for (String palavra : nome.replaceAll("[\\r\\n\\t]", " ").split(" ")) {
            String tentativa = linha.isEmpty() ? palavra : linha + " " + palavra;
            if (!linha.isEmpty() && fonte.getStringWidth(tentativa) / 1000 * tamanho > largura) { linhas.add(linha); linha = palavra; }
            else linha = tentativa;
        }
        if (!linha.isEmpty()) linhas.add(linha);
        linhas.add(papel);
        float y = 90;
        for (String texto : linhas) {
            float escala = Math.min(tamanho, largura * 1000 / Math.max(1, fonte.getStringWidth(texto)));
            stream.beginText(); stream.setFont(fonte, escala);
            stream.newLineAtOffset(x + (largura - fonte.getStringWidth(texto) / 1000 * escala) / 2, y);
            stream.showText(texto); stream.endText(); y -= 13;
        }
    }

    public String camposAssinatura(Contrato contrato) {
        return "<table class=\"contract-signatures\" style=\"width:100%;margin-top:48px;text-align:center\"><tr><td style=\"width:50%;padding:24px 12px\"><div style=\"border-top:1px solid #444;padding-top:12px\">"
                + escapar(nomeCliente(contrato)) + "<br/>Contratante</div></td><td style=\"width:50%;padding:24px 12px\"><div style=\"border-top:1px solid #444;padding-top:12px\">"
                + escapar(nomeEmpresa(contrato)) + "<br/>Contratada</div></td></tr></table>";
    }
    private String nomeCliente(Contrato c) { return c.getContratante() == null ? "Contratante" : c.getContratante().getNomeRazaoSocial(); }
    private String nomeEmpresa(Contrato c) { return c.getEmpresa() == null ? "Empresa contratada não informada" : c.getEmpresa().getRazaoSocial(); }

    private String normalizarHtml(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) return "<p>Contrato sem conteúdo.</p>";
        if (!conteudo.matches("(?s).*<[/a-zA-Z][^>]*>.*")) conteudo = "<p>" + escapar(conteudo).replace("\n", "<br/>") + "</p>";
        var documento = Jsoup.parseBodyFragment(conteudo);
        documento.select(".contract-signatures").remove();
        // Converte atributos gerados pelo editor antigo em CSS preservado pelo renderizador.
        for (var elemento : documento.select("font")) {
            String estilo = elemento.attr("style");
            if (elemento.hasAttr("color")) estilo += ";color:" + elemento.attr("color");
            if (elemento.hasAttr("face")) estilo += ";font-family:" + elemento.attr("face");
            if (elemento.hasAttr("size")) {
                String[] tamanhos = {"8pt", "10pt", "12pt", "14pt", "18pt", "24pt", "36pt"};
                try { estilo += ";font-size:" + tamanhos[Math.max(0, Math.min(6, Integer.parseInt(elemento.attr("size")) - 1))]; } catch (NumberFormatException ignorado) { }
            }
            elemento.tagName("span").attr("style", estilo.replaceFirst("^;", ""));
        }
        for (var elemento : documento.select("[style]")) {
            elemento.attr("style", elemento.attr("style")
                    .replaceAll("(?i)Arial|Helvetica|sans-serif", "Noto Sans")
                    .replaceAll("(?i)Times New Roman|(?<!Noto )\\bserif\\b", "Noto Serif")
                    .replaceAll("(?i)Courier New|monospace", "Noto Sans Mono"));
        }
        documento.outputSettings(new OutputSettings().syntax(OutputSettings.Syntax.xml));
        return documento.body().html();
    }
    private String escapar(String texto) { return texto == null ? "" : org.jsoup.nodes.Entities.escape(texto); }
}
