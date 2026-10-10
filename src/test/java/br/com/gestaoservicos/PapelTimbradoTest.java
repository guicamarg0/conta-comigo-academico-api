package br.com.gestaoservicos;

import br.com.gestaoservicos.compartilhado.PapelTimbrado;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PapelTimbradoTest {
    @Test void destaca_cantos_opostos_em_todas_as_paginas_sem_ocupar_o_centro() throws Exception {
        try (var documento = new PDDocument()) {
            documento.addPage(new PDPage(PDRectangle.A4));
            documento.addPage(new PDPage(PDRectangle.A4));
            PapelTimbrado.aplicar(documento);
            assertEquals(2, documento.getNumberOfPages());
            var renderizador = new PDFRenderer(documento);
            for (int p = 0; p < documento.getNumberOfPages(); p++) {
                var imagem = renderizador.renderImageWithDPI(p, 144);
                int x = Math.round(14 * 144f / 25.4f);
                int y = Math.round(8.3f * 144f / 25.4f);
                var superior = new Color(imagem.getRGB(x, y));
                var inferior = new Color(imagem.getRGB(imagem.getWidth() - x, imagem.getHeight() - y));
                assertTrue(superior.getGreen() > superior.getRed() + 20);
                assertTrue(inferior.getGreen() > inferior.getRed() + 20);
                assertEquals(Color.WHITE.getRGB(), imagem.getRGB(imagem.getWidth() / 2, imagem.getHeight() / 2));
                assertEquals(Color.WHITE.getRGB(), imagem.getRGB(imagem.getWidth() - x, y));
                assertEquals(Color.WHITE.getRGB(), imagem.getRGB(x, imagem.getHeight() - y));
            }
            documento.removePage(1);
            Files.createDirectories(Path.of("target/pdf-verificacao"));
            documento.save("target/pdf-verificacao/papel-timbrado-conta-comigo.pdf");
            javax.imageio.ImageIO.write(renderizador.renderImageWithDPI(0, 120), "png",
                    Path.of("target/pdf-verificacao/papel-timbrado-conta-comigo.png").toFile());
        }
    }
}
