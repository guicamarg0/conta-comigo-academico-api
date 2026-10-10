package br.com.gestaoservicos.painel.controller;
import br.com.gestaoservicos.painel.service.PainelService;
import br.com.gestaoservicos.painel.dto.PainelResponseDTO;
import br.com.gestaoservicos.security.ContextoUnidade;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/painel")
public class PainelController {
    private final PainelService service;
    public PainelController(PainelService service) { this.service=service; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PainelResponseDTO consultar(@RequestParam LocalDate de,@RequestParam LocalDate ate) {
        return service.consultar(ContextoUnidade.atual().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione uma unidade")).unidadeId(),de,ate);
    }
}
