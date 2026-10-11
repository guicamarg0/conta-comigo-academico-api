package br.com.gestaoservicos.compartilhado;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public abstract class GeradorPdf<T> {

    // Template method: a ordem das etapas é fixa (final).
    public final byte[] gerar(T documento) {
        try {
            byte[] pdf = renderizarConteudo(documento);
            return finalizar(pdf, documento);
        } catch (Exception e) {
            throw new IllegalStateException(mensagemErro(), e);
        }
    }

    // Etapas que cada subclasse implementa.
    protected abstract byte[] renderizarConteudo(T documento) throws Exception;

    protected abstract void desenharAssinaturas(PDPageContentStream tela, PDType0Font fonte, PDPage pagina, T documento) throws IOException;

    protected abstract String mensagemErro();

    // Gancho (hook): por padrão mostra as assinaturas.
    protected boolean exibirAssinaturas(T documento) {
        return true;
    }

    // Etapa comum: timbrado em todas as páginas e assinaturas na última.
    private byte[] finalizar(byte[] pdf, T documento) throws IOException {
        try (var arquivo = Loader.loadPDF(pdf);
             var fonteStream = GeradorPdf.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf");
             var resultado = new ByteArrayOutputStream()) {
            PapelTimbrado.aplicar(arquivo);
            var fonte = PDType0Font.load(arquivo, fonteStream);
            var pagina = arquivo.getPage(arquivo.getNumberOfPages() - 1);
            if (exibirAssinaturas(documento)) {
                try (var tela = new PDPageContentStream(arquivo, pagina, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    desenharAssinaturas(tela, fonte, pagina, documento);
                }
            }
            arquivo.save(resultado);
            return resultado.toByteArray();
        }
    }
}