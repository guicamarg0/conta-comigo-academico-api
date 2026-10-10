package br.com.gestaoservicos;

import br.com.gestaoservicos.orcamento.model.*;
import br.com.gestaoservicos.orcamento.service.GeradorPdfOrcamento;
import br.com.gestaoservicos.catalogo.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeradorPdfOrcamentoTest {
    private RevisaoOrcamento revisao(int quantidade) throws Exception {
        var orcamento=new Orcamento(new br.com.gestaoservicos.unidade.model.Unidade("Teste PDF", "teste pdf"),"ORC-2026-000051");
        var r=new RevisaoOrcamento(orcamento,1);
        var itens=new ArrayList<ItemRevisaoOrcamento>();
        for(int n=1;n<=quantidade;n++) itens.add(new ItemRevisaoOrcamento(r,n,null,TipoItemCatalogo.PECA,
            "Item "+n+" - Fuso de esferas laminado ETM (padrão flexo one) - descrição técnica com acentuação",UnidadeMedida.UNIDADE,null,new BigDecimal("16"),new BigDecimal("3092")));
        r.atualizar(null,LocalDate.of(2026,10,30),"28/58/88 dias", "Observação: orçamento sem mão de obra para instalação.\nFaturamento: conforme condições acordadas.",true,TipoAjuste.VALOR,new BigDecimal("100"),TipoAjuste.VALOR,new BigDecimal("25"),itens);
        var imagem=new java.awt.image.BufferedImage(180,60,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=imagem.createGraphics();g.setColor(java.awt.Color.BLUE);g.fillRect(0,0,180,60);g.setColor(java.awt.Color.WHITE);g.drawString("LOGO EMPRESA",20,35);g.dispose();
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(imagem,"png",bytes);
        var json=new ObjectMapper();
        r.registrarSnapshots(json.writeValueAsString(Map.of("nomeRazaoSocial","Cliente Industrial LTDA","documento","55834337000196","endereco","Sorocaba - SP")),
            json.writeValueAsString(Map.of("nomeRazaoSocial","Assistência Técnica LTDA","documento","63047223000124","enderecoCompleto","Curitiba - PR","logoUrl","data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray()))),"[]");
        return r;
    }
    @Test void renderiza_layout_logo_totais_e_acentos() throws Exception {
        var r=revisao(4);byte[] pdf=new GeradorPdfOrcamento(new ObjectMapper()).gerar(r.getOrcamento(),r);
        Files.createDirectories(Path.of("target/pdf-verificacao"));Files.write(Path.of("target/pdf-verificacao/orcamento-padrao.pdf"),pdf);
        try(var d=Loader.loadPDF(pdf)) {
            assertEquals(1,d.getNumberOfPages());var texto=new PDFTextStripper().getText(d);
            assertTrue(texto.contains("ORÇAMENTO"));assertTrue(texto.contains("ORC-2026-000051"));assertTrue(texto.contains("CONDIÇÕES COMERCIAIS"));
            assertTrue(texto.contains("197.813,00"));assertTrue(texto.contains("55.834.337/0001-96"));assertFalse(texto.contains("Rev."));
            assertTrue(texto.contains("Contratada / Emitente"));assertTrue(texto.contains("Contratante / Cliente"));
            int imagens=0;for(var nome:d.getPage(0).getResources().getXObjectNames())if(d.getPage(0).getResources().isImageXObject(nome))imagens++;
            assertTrue(imagens>0,"Logo incorporada no PDF");
            javax.imageio.ImageIO.write(new PDFRenderer(d).renderImageWithDPI(0,120),"png",Path.of("target/pdf-verificacao/orcamento-padrao.png").toFile());
        }
    }
    @Test void repete_cabecalho_tabela_e_assina_apenas_ultima_pagina() throws Exception {
        var r=revisao(80);byte[] pdf=new GeradorPdfOrcamento(new ObjectMapper()).gerar(r.getOrcamento(),r);
        Files.createDirectories(Path.of("target/pdf-verificacao"));Files.write(Path.of("target/pdf-verificacao/orcamento-longo.pdf"),pdf);
        try(var d=Loader.loadPDF(pdf)) {
            assertTrue(d.getNumberOfPages()>1);
            for(int p=1;p<=d.getNumberOfPages();p++) {
                var texto=new PDFTextStripper();texto.setStartPage(p);texto.setEndPage(p);String pagina=texto.getText(d);
                if(p<d.getNumberOfPages()){assertFalse(pagina.contains("Contratada / Emitente"));assertTrue(pagina.contains("Valor Unitário"));}
                else {assertTrue(pagina.contains("Contratada / Emitente"));assertTrue(pagina.contains("Item 80"));assertTrue(pagina.contains("TOTAL DO PEDIDO"));}
                var imagem = new PDFRenderer(d).renderImageWithDPI(p-1,96);
                int x = Math.round(14 * 96f / 25.4f), y = Math.round(8.3f * 96f / 25.4f);
                var canto = new java.awt.Color(imagem.getRGB(x, y));
                var oposto = new java.awt.Color(imagem.getRGB(imagem.getWidth()-x, imagem.getHeight()-y));
                assertTrue(canto.getGreen() > canto.getRed() + 20, "Timbrado superior na página " + p);
                assertTrue(oposto.getGreen() > oposto.getRed() + 20, "Timbrado inferior na página " + p);
                javax.imageio.ImageIO.write(imagem,"png",Path.of("target/pdf-verificacao/orcamento-longo-"+p+".png").toFile());
            }
        }
    }
    @Test void omite_assinaturas_pagamento_e_referencia_quando_desmarcados() throws Exception {
        var r=revisao(1);
        r.atualizar(null,r.getValidade(),"Pagamento exclusivo", "Observação visível",false,TipoAjuste.VALOR,BigDecimal.ZERO,TipoAjuste.VALOR,BigDecimal.ZERO,r.getItens());
        r.definirOpcoesPdf(false,"Nome interno do orçamento");
        try(var d=Loader.loadPDF(new GeradorPdfOrcamento(new ObjectMapper()).gerar(r.getOrcamento(),r))){
            var texto=new PDFTextStripper().getText(d);
            assertTrue(texto.contains("Observação visível"));
            assertFalse(texto.contains("Pagamento exclusivo"));
            assertFalse(texto.contains("Nome interno do orçamento"));
            assertFalse(texto.contains("Contratada / Emitente"));
            assertFalse(texto.contains("Contratante / Cliente"));
        }
    }
    @Test void mantem_ultimo_item_total_condicoes_e_assinaturas_juntos_nas_quebras() throws Exception {
        for(int quantidade : new int[]{12,16,20,24,28,32}) {
            var r=revisao(quantidade);
            var itens=new ArrayList<ItemRevisaoOrcamento>();
            for(int n=1;n<=quantidade;n++) itens.add(new ItemRevisaoOrcamento(r,n,null,TipoItemCatalogo.PECA,
                "Peça de teste "+n,UnidadeMedida.UNIDADE,null,BigDecimal.ONE,BigDecimal.TEN));
            r.atualizar(null,r.getValidade(),"Pix", "Materiais destinados à manutenção de equipamento.",true,TipoAjuste.VALOR,BigDecimal.ZERO,TipoAjuste.VALOR,BigDecimal.ZERO,itens);
            try(var d=Loader.loadPDF(new GeradorPdfOrcamento(new ObjectMapper()).gerar(r.getOrcamento(),r))){
                var extrator=new PDFTextStripper();extrator.setStartPage(d.getNumberOfPages());extrator.setEndPage(d.getNumberOfPages());
                String ultima=extrator.getText(d);
                assertTrue(ultima.contains("Peça de teste "+quantidade),"Último item junto do encerramento: "+quantidade);
                assertTrue(ultima.contains("TOTAL DO PEDIDO"));
                assertTrue(ultima.contains("Materiais destinados"));
                assertTrue(ultima.contains("Contratada / Emitente"));
                assertTrue(ultima.contains("Contratante / Cliente"));
            }
        }
    }
    @Test void observacoes_extensas_nao_deixam_assinatura_sem_conteudo() throws Exception {
        var r=revisao(16);
        r.atualizar(null,r.getValidade(),"Pix","Observação comercial. ".repeat(180),true,TipoAjuste.VALOR,BigDecimal.ZERO,TipoAjuste.VALOR,BigDecimal.ZERO,r.getItens());
        try(var d=Loader.loadPDF(new GeradorPdfOrcamento(new ObjectMapper()).gerar(r.getOrcamento(),r))){
            assertTrue(d.getNumberOfPages()>1);
            var extrator=new PDFTextStripper();extrator.setStartPage(d.getNumberOfPages());extrator.setEndPage(d.getNumberOfPages());
            String ultima=extrator.getText(d);
            assertTrue(ultima.contains("Observação comercial."));
            assertTrue(ultima.contains("Contratada / Emitente"));
        }
    }
}
