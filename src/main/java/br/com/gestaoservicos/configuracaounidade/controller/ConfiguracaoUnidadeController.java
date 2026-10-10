package br.com.gestaoservicos.configuracaounidade.controller;

import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.service.ConfiguracaoUnidadeService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades/atual/configuracao")
@PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN')")
public class ConfiguracaoUnidadeController {
    private final ConfiguracaoUnidadeService service;
    public ConfiguracaoUnidadeController(ConfiguracaoUnidadeService service) { this.service = service; }
    @GetMapping ConfiguracaoUnidadeResponseDTO consultar() { return service.consultar(unidadeIdAtual()); }
    @PutMapping ConfiguracaoUnidadeResponseDTO salvar(@Valid @RequestBody ConfiguracaoUnidadeRequestDTO requisicao) { return service.salvar(unidadeIdAtual(), requisicao); }
    private UUID unidadeIdAtual() { return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade pelo cabeçalho X-Unidade-Id")); }
}
