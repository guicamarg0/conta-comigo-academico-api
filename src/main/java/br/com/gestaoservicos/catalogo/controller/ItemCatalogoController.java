package br.com.gestaoservicos.catalogo.controller;

import br.com.gestaoservicos.catalogo.dto.*;
import br.com.gestaoservicos.catalogo.model.TipoItemCatalogo;
import br.com.gestaoservicos.catalogo.service.ItemCatalogoService;
import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalogo-itens")
public class ItemCatalogoController {
    private final ItemCatalogoService service;
    public ItemCatalogoController(ItemCatalogoService service) { this.service = service; }
    @GetMapping @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA')") public PaginaResponseDTO<ItemCatalogoResponseDTO> listar(@RequestParam(required = false) String busca, @RequestParam(required = false) TipoItemCatalogo tipo, @RequestParam(required = false) Boolean ativo, @PageableDefault(sort = "nome") Pageable paginacao) { return service.listar(unidadeIdAtual(), busca, tipo, ativo, paginacao); }
    @GetMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA')") public ItemCatalogoResponseDTO consultar(@PathVariable UUID id) { return service.consultar(unidadeIdAtual(), id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR')") public ItemCatalogoResponseDTO criar(@Valid @RequestBody ItemCatalogoRequestDTO dto) { return service.criar(unidadeIdAtual(), dto); }
    @PutMapping("/{id}") @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR')") public ItemCatalogoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ItemCatalogoRequestDTO dto) { return service.atualizar(unidadeIdAtual(), id, dto); }
    @PatchMapping("/{id}/inativar") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR')") public void inativar(@PathVariable UUID id) { service.inativar(unidadeIdAtual(), id); }
    @PatchMapping("/{id}/reativar") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR')") public void reativar(@PathVariable UUID id) { service.reativar(unidadeIdAtual(), id); }
    private UUID unidadeIdAtual() { return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade pelo cabeçalho X-Unidade-Id")); }
}
