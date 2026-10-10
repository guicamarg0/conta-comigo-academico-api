package br.com.gestaoservicos;

import br.com.gestaoservicos.contrato.model.Contrato;
import br.com.gestaoservicos.contratante.model.Contratante;
import br.com.gestaoservicos.empresa.model.Empresa;
import br.com.gestaoservicos.contrato.service.GeradorPdfContrato;
import br.com.gestaoservicos.orcamento.model.Orcamento;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeradorPdfContratoTest {
    @Test void formataCpfECnpjDeContratoJaSalvoNoPdf() throws Exception {
        var contrato = contrato("<p>Prestador 52998224725 e cliente 12345678000190</p>");
        when(contrato.getEmpresa().getCnpj()).thenReturn("52998224725");
        when(contrato.getContratante().getDocumento()).thenReturn("12345678000190");
        try (var pdf = Loader.loadPDF(new GeradorPdfContrato().gerar(contrato))) {
            String texto = new PDFTextStripper().getText(pdf);
            assertTrue(texto.contains("529.982.247-25"));
            assertTrue(texto.contains("12.345.678/0001-90"));
        }
    }
    private Contrato contrato(String conteudo) {
        var contrato = mock(Contrato.class); var orcamento = mock(Orcamento.class);
        var cliente = mock(Contratante.class); var empresa = mock(Empresa.class);
        when(contrato.getNumero()).thenReturn("CT-001"); when(contrato.getNumeroVersao()).thenReturn(1);
        when(contrato.getOrcamento()).thenReturn(orcamento); when(orcamento.getNumero()).thenReturn("52");
        when(contrato.getContratante()).thenReturn(cliente); when(cliente.getNomeRazaoSocial()).thenReturn("Cliente Industrial LTDA");
        when(contrato.getEmpresa()).thenReturn(empresa); when(empresa.getRazaoSocial()).thenReturn("Prestadora de Serviços LTDA");
        when(contrato.getConteudoRascunho()).thenReturn(conteudo); return contrato;
    }
    @Test void preserva_cor_fonte_tamanho_e_formatacao_legada() throws Exception {
        var contrato = contrato("<p><span style='font-family:Noto Serif;font-size:24pt;color:#ff0000'>TEXTO VERMELHO</span></p>"
                + "<p><font face='Courier New' color='#0000ff' size='5'>TEXTO AZUL LEGADO</font></p>");
        var pdf = new GeradorPdfContrato().gerar(contrato);
        Files.createDirectories(Path.of("target/pdf-verificacao")); Files.write(Path.of("target/pdf-verificacao/contrato-formatado.pdf"), pdf);
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("TEXTO VERMELHO")); assertTrue(texto.contains("TEXTO AZUL LEGADO"));
            var nomes = new java.util.ArrayList<String>();
            for (var nome : documento.getPage(0).getResources().getFontNames()) nomes.add(documento.getPage(0).getResources().getFont(nome).getName());
            assertTrue(nomes.stream().anyMatch(n -> n.contains("NotoSerif")), nomes.toString());
            assertTrue(nomes.stream().anyMatch(n -> n.contains("NotoSansMono")), nomes.toString());
            var tamanhos = new java.util.ArrayList<Float>();
            var leitor = new PDFTextStripper() {
                @Override protected void processTextPosition(org.apache.pdfbox.text.TextPosition posicao) {
                    tamanhos.add(posicao.getFontSizeInPt()); super.processTextPosition(posicao);
                }
            };
            leitor.getText(documento);
            assertTrue(tamanhos.stream().anyMatch(t -> Math.abs(t - 24) < 1), tamanhos.toString());
            assertTrue(tamanhos.stream().anyMatch(t -> Math.abs(t - 18) < 1), tamanhos.toString());
            var imagem = new PDFRenderer(documento).renderImageWithDPI(0, 96);
            javax.imageio.ImageIO.write(imagem, "png", Path.of("target/pdf-verificacao/contrato-formatado.png").toFile());
            int vermelho = 0, azul = 0;
            for (int y = 0; y < imagem.getHeight(); y++) for (int x = 0; x < imagem.getWidth(); x++) {
                var cor = new java.awt.Color(imagem.getRGB(x,y));
                if (cor.getRed() > 180 && cor.getGreen() < 80 && cor.getBlue() < 80) vermelho++;
                if (cor.getBlue() > 180 && cor.getRed() < 80 && cor.getGreen() < 80) azul++;
            }
            assertTrue(vermelho > 100); assertTrue(azul > 100);
        }
    }
    @Test void assinaturas_aparecem_somente_no_rodape_da_ultima_pagina() throws Exception {
        var contrato = contrato("<p>Cláusula de serviço e condições da execução.</p>".repeat(100));
        var pdf = new GeradorPdfContrato().gerar(contrato);
        Files.createDirectories(Path.of("target/pdf-verificacao")); Files.write(Path.of("target/pdf-verificacao/contrato-multipagina.pdf"), pdf);
        try (var documento = Loader.loadPDF(pdf)) {
            assertTrue(documento.getNumberOfPages() > 1);
            var stripper = new PDFTextStripper();
            stripper.setEndPage(documento.getNumberOfPages() - 1);
            assertFalse(stripper.getText(documento).contains("Cliente Industrial LTDA"));
            stripper.setStartPage(documento.getNumberOfPages()); stripper.setEndPage(documento.getNumberOfPages());
            String ultima = stripper.getText(documento);
            assertTrue(ultima.contains("Cliente Industrial LTDA")); assertTrue(ultima.contains("Prestadora de Serviços LTDA"));
            var imagem = new PDFRenderer(documento).renderImageWithDPI(documento.getNumberOfPages()-1, 96);
            javax.imageio.ImageIO.write(imagem, "png", Path.of("target/pdf-verificacao/ultima-pagina.png").toFile());
        }
    }
}
