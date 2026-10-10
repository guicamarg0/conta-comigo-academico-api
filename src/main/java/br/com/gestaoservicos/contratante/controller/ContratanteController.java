package br.com.gestaoservicos.contratante.controller;
import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.contratante.dto.*;
import br.com.gestaoservicos.contratante.service.ContratanteService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/contratantes") public class ContratanteController {
    private final ContratanteService service; public ContratanteController(ContratanteService service) { this.service = service; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA')") public PaginaResponseDTO<ContratanteResponseDTO> listar(@RequestParam(required = false) String busca, @RequestParam(required = false) Boolean ativo, @PageableDefault(sort = "nomeRazaoSocial") Pageable paginacao) { return service.listar(unidadeIdAtual(), busca, ativo, paginacao); }
    @GetMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA')") public ContratanteResponseDTO consultar(@PathVariable UUID id) { return service.consultar(unidadeIdAtual(), id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR')") public ContratanteResponseDTO criar(@Valid @RequestBody ContratanteRequestDTO dto) { return service.criar(unidadeIdAtual(), dto); }
    @PutMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR')") public ContratanteResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ContratanteRequestDTO dto) { return service.atualizar(unidadeIdAtual(), id, dto); }
    @PatchMapping("/{id}/inativar") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR')") public void inativar(@PathVariable UUID id) { service.inativar(unidadeIdAtual(), id); }
    @PatchMapping("/{id}/reativar") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR')") public void reativar(@PathVariable UUID id) { service.reativar(unidadeIdAtual(), id); }
    private UUID unidadeIdAtual() { return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade pelo cabeçalho X-Unidade-Id")); }
}
