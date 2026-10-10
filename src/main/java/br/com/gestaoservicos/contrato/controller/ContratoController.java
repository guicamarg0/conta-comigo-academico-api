package br.com.gestaoservicos.contrato.controller;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.contrato.dto.*;
import br.com.gestaoservicos.contrato.model.StatusContrato;
import br.com.gestaoservicos.contrato.service.ContratoService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/contratos")
public class ContratoController {
    private final ContratoService service;
    public ContratoController(ContratoService service) { this.service = service; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<ContratoResponseDTO> listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusContrato status, @RequestParam(defaultValue = "ATIVO") CadastroContrato cadastro,
            @PageableDefault(sort = "inicioVigencia") Pageable pagina) {
        return service.listar(unidade(), busca, status, cadastro.filtroAtivo(), pagina);
    }
    @GetMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public ContratoResponseDTO consultar(@PathVariable UUID id) { return service.consultar(unidade(), id); }
    @GetMapping("/{id}/snapshots") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public java.util.List<ContratoSnapshotResponseDTO> snapshots(@PathVariable UUID id) { return service.snapshots(unidade(), id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO criar(@Valid @RequestBody ContratoRequestDTO dto) { return service.criar(unidade(), dto); }
    @PutMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ContratoRequestDTO dto) {
        return service.atualizar(unidade(), id, dto);
    }
    @PostMapping("/previa") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoPreviaResponseDTO previa(@Valid @RequestBody ContratoPreviaRequestDTO dto) { return service.previa(unidade(), dto); }
    @PostMapping("/{id}/encerrar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO encerrar(@PathVariable UUID id) { return service.encerrar(unidade(), id); }
    @PostMapping("/{id}/marcar-vigente") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO marcarVigente(@PathVariable UUID id) { return service.marcarVigente(unidade(), id); }
    @PostMapping("/{id}/inativar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO inativar(@PathVariable UUID id) { return service.definirAtivo(unidade(), id, false); }
    @PostMapping("/{id}/reativar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO reativar(@PathVariable UUID id) { return service.definirAtivo(unidade(), id, true); }
    @PostMapping("/{id}/versoes/{versaoId}/restaurar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ContratoResponseDTO restaurar(@PathVariable UUID id, @PathVariable UUID versaoId) { return service.restaurar(unidade(), id, versaoId); }
    @GetMapping("/{id}/versoes") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<ContratoSnapshotResponseDTO> versoes(@PathVariable UUID id, @PageableDefault(size = 10) Pageable pagina) { return service.versoes(unidade(), id, pagina); }
    @GetMapping("/{id}/atividades") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<AtividadeContratoResponseDTO> atividades(@PathVariable UUID id, @PageableDefault(size = 10) Pageable pagina) { return service.atividades(unidade(), id, pagina); }
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        var contrato=service.consultar(unidade(),id);
        String nome=br.com.gestaoservicos.compartilhado.NomeDocumento.pdf(contrato.numero(),contrato.cliente());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment().filename(nome).build().toString())
                .body(service.pdf(unidade(), id));
    }
    private UUID unidade() {
        return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade"));
    }
}
