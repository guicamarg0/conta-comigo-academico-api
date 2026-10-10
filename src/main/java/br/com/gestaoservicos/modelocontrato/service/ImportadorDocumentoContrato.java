package br.com.gestaoservicos.modelocontrato.service;

import br.com.gestaoservicos.modelocontrato.dto.DocumentoImportadoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

@Service
public class ImportadorDocumentoContrato {
    public DocumentoImportadoResponseDTO importar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um documento");
        if (arquivo.getSize() > 5_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O documento deve ter no máximo 5 MB");
        var nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase();
        try {
            if (nome.endsWith(".txt")) return new DocumentoImportadoResponseDTO(new String(arquivo.getBytes(), StandardCharsets.UTF_8).strip());
            if (!nome.endsWith(".docx")) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie um arquivo .docx ou .txt");
            try (var zip = new ZipInputStream(arquivo.getInputStream())) {
                for (var entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                    if (!"word/document.xml".equals(entrada.getName())) continue;
                    var xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    var texto = xml.replaceAll("</w:p>", "\n").replaceAll("<[^>]+>", "")
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replaceAll("\\n{3,}", "\n\n").strip();
                    if (texto.isBlank()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "O documento não contém texto importável");
                    return new DocumentoImportadoResponseDTO(texto);
                }
            }
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível ler o conteúdo do DOCX");
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o documento"); }
    }
}
