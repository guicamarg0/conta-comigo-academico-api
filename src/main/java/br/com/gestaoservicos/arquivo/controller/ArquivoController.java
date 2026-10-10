package br.com.gestaoservicos.arquivo.controller;
import br.com.gestaoservicos.arquivo.service.ArquivoService;
import br.com.gestaoservicos.arquivo.dto.ArquivoResponseDTO;
import br.com.gestaoservicos.security.ContextoUnidade;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/v1/arquivos")
public class ArquivoController {
    private final ArquivoService service;
    public ArquivoController(ArquivoService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ArquivoResponseDTO enviar(@RequestParam MultipartFile arquivo){return service.enviar(unidade(),arquivo);}
    @GetMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID id){var arquivo=service.consultar(unidade(),id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(arquivo.getTipo()))
        .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(arquivo.getNome(),StandardCharsets.UTF_8).build().toString()).body(service.conteudo(arquivo));}
    private UUID unidade(){return ContextoUnidade.atual().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione uma unidade")).unidadeId();}
}
