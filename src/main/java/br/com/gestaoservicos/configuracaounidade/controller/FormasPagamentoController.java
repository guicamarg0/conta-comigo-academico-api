package br.com.gestaoservicos.configuracaounidade.controller;
import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.service.FormasPagamentoService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/unidades/atual/formas-pagamento")
public class FormasPagamentoController {
    private final FormasPagamentoService service;
    public FormasPagamentoController(FormasPagamentoService service) { this.service=service; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public FormasPagamentoResponseDTO consultar() { return service.consultar(unidade()); }
    @PutMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public FormasPagamentoResponseDTO salvar(@Valid @RequestBody FormasPagamentoRequestDTO dados) { return service.salvar(unidade(),dados); }
    private UUID unidade() { return ContextoUnidade.atual().orElseThrow().unidadeId(); }
}
