package br.com.gestaoservicos.modelocontrato.controller;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.modelocontrato.dto.*;
import br.com.gestaoservicos.modelocontrato.model.StatusModeloContrato;
import br.com.gestaoservicos.modelocontrato.service.ModeloContratoService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/v1/modelos-contrato")
public class ModeloContratoController {
    private final ModeloContratoService service;
    private final br.com.gestaoservicos.modelocontrato.service.ImportadorDocumentoContrato importador;
    public ModeloContratoController(ModeloContratoService service, br.com.gestaoservicos.modelocontrato.service.ImportadorDocumentoContrato importador) { this.service = service; this.importador = importador; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<ModeloContratoResponseDTO> listar(@RequestParam(required = false) StatusModeloContrato status, @PageableDefault(sort = "atualizadoEm", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pagina) { return service.listar(unidade(), status, pagina); }
    @GetMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public ModeloContratoResponseDTO consultar(@PathVariable UUID id) { return service.consultar(unidade(), id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO criar(@Valid @RequestBody ModeloContratoRequestDTO dto) { return service.criar(unidade(), dto); }
    @PostMapping(value = "/importar-documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public DocumentoImportadoResponseDTO importar(@RequestParam("arquivo") MultipartFile arquivo) { return importador.importar(arquivo); }
    @PutMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ModeloContratoRequestDTO dto) { return service.atualizar(unidade(), id, dto); }
    @PostMapping("/{id}/publicar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO publicar(@PathVariable UUID id) { return service.publicar(unidade(), id); }
    @PostMapping("/{id}/inativar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO inativar(@PathVariable UUID id) { return service.inativar(unidade(), id); }
    @PostMapping("/{id}/reativar") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO reativar(@PathVariable UUID id) { return service.reativar(unidade(), id); }
    @PostMapping("/{id}/duplicar") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ModeloContratoResponseDTO duplicar(@PathVariable UUID id) { return service.duplicar(unidade(), id); }
    @GetMapping("/{id}/versoes") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public List<VersaoModeloContratoResponseDTO> versoes(@PathVariable UUID id) { return service.versoes(unidade(), id); }
    private UUID unidade() { return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade")); }
}
