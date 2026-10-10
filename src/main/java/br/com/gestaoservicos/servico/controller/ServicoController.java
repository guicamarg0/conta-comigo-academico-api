package br.com.gestaoservicos.servico.controller;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.security.ContextoUnidade;
import br.com.gestaoservicos.servico.dto.*;
import br.com.gestaoservicos.servico.model.StatusServico;
import br.com.gestaoservicos.servico.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ServicoController {
    private final ServicoService service;
    public ServicoController(ServicoService service) { this.service = service; }
    @GetMapping("/servicos")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<ServicoResponseDTO> listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusServico status, @PageableDefault(sort = "criadoEm") Pageable pagina) {
        return service.listar(unidade(), busca, status, pagina);
    }
    @GetMapping("/agenda")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public PaginaResponseDTO<ServicoResponseDTO> agenda(@RequestParam Instant de, @RequestParam Instant ate,
            @RequestParam(required = false) String responsavel, @PageableDefault(sort = "dataProgramada") Pageable pagina) {
        return service.agenda(unidade(), de, ate, responsavel, pagina);
    }
    @GetMapping("/servicos/{id}")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public ServicoResponseDTO consultar(@PathVariable UUID id) { return service.consultar(unidade(), id); }
    @PostMapping("/servicos") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ServicoResponseDTO criar(@Valid @RequestBody ServicoRequestDTO dto) { return service.criar(unidade(), dto); }
    @PutMapping("/servicos/{id}")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ServicoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoRequestDTO dto) { return service.atualizar(unidade(), id, dto); }
    @PutMapping("/servicos/{id}/agendamento")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ServicoResponseDTO agendar(@PathVariable UUID id, @Valid @RequestBody AgendamentoRequestDTO dto) { return service.agendar(unidade(), id, dto); }
    @PostMapping("/servicos/{id}/iniciar")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ServicoResponseDTO iniciar(@PathVariable UUID id) { return service.iniciar(unidade(), id); }
    @PostMapping("/servicos/{id}/concluir")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR')")
    public ServicoResponseDTO concluir(@PathVariable UUID id, @Valid @RequestBody ConclusaoServicoRequestDTO dto) { return service.concluir(unidade(), id, dto); }
    @PostMapping("/servicos/{id}/cancelar")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    public ServicoResponseDTO cancelar(@PathVariable UUID id) { return service.cancelar(unidade(), id); }
    private UUID unidade() {
        return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade"));
    }
}
