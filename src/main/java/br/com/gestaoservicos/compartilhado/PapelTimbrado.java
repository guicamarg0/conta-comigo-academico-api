package br.com.gestaoservicos.compartilhado;

import java.awt.Color;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;

/** Detalhes vetoriais nas margens, sem interferir na paginação do documento. */
public final class PapelTimbrado {
    private static final float MM = 72f / 25.4f;

    private PapelTimbrado() { }

    public static void aplicar(PDDocument documento) throws IOException {
        for (var pagina : documento.getPages()) {
            try (var tela = new PDPageContentStream(documento, pagina,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                var folha = pagina.getMediaBox();
                tela.saveGraphicsState();
                tela.transform(new Matrix(1, 0, 0, -1, folha.getLowerLeftX(), folha.getUpperRightY()));
                desenharCanto(tela);
                tela.restoreGraphicsState();
                tela.saveGraphicsState();
                tela.transform(new Matrix(-1, 0, 0, 1, folha.getUpperRightX(), folha.getLowerLeftY()));
                desenharCanto(tela);
                tela.restoreGraphicsState();
            }
        }
    }

    private static void desenharCanto(PDPageContentStream tela) throws IOException {
        // A faixa mais clara forma um L; o pequeno traço verde dá o destaque.
        tela.setNonStrokingColor(new Color(221, 235, 229));
        tela.moveTo(8 * MM, 8 * MM);
        tela.lineTo(40 * MM, 8 * MM);
        tela.lineTo(38 * MM, 10 * MM);
        tela.lineTo(10 * MM, 10 * MM);
        tela.lineTo(10 * MM, 20 * MM);
        tela.lineTo(8 * MM, 22 * MM);
        tela.closePath();
        tela.fill();
        tela.setNonStrokingColor(new Color(139, 190, 174));
        tela.addRect(12 * MM, 8 * MM, 14 * MM, 0.7f * MM);
        tela.fill();
    }
}
