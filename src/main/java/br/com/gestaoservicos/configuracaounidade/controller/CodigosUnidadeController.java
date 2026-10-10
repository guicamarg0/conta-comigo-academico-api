package br.com.gestaoservicos.configuracaounidade.controller;
import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.service.CodigosUnidadeService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/unidades/atual/codigos")
public class CodigosUnidadeController {
    private final CodigosUnidadeService servico;
    public CodigosUnidadeController(CodigosUnidadeService servico) { this.servico=servico; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    CodigosUnidadeResponseDTO consultar() { return servico.consultar(unidade()); }
    @PutMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    CodigosUnidadeResponseDTO salvar(@Valid @RequestBody CodigosUnidadeRequestDTO dto) { return servico.salvar(unidade(),dto); }
    private UUID unidade() { return ContextoUnidade.atual().orElseThrow().unidadeId(); }
}
