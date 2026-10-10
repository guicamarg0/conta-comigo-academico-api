package br.com.gestaoservicos;

import br.com.gestaoservicos.compartilhado.DocumentoFiscal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentoFiscalTest {
    @Test void formataDocumentosLegadosSemAlterarAtributosOuNumerosMaiores() {
        String html = "<p data-id='52998224725'>CPF 52998224725; CNPJ 12345678000190; 1529982247250</p>";
        String formatado = DocumentoFiscal.formatarNoTexto(html, "52998224725", "12345678000190");
        assertTrue(formatado.contains("529.982.247-25"));
        assertTrue(formatado.contains("12.345.678/0001-90"));
        assertTrue(formatado.contains("data-id=\"52998224725\""));
        assertTrue(formatado.contains("1529982247250"));
        assertEquals("CPF 529.982.247-25", DocumentoFiscal.formatarNoTexto("CPF 52998224725", "52998224725"));
        assertEquals(formatado, DocumentoFiscal.formatarNoTexto(formatado, "52998224725"));
    }
}
