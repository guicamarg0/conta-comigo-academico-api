package br.com.gestaoservicos.unidade.controller;

import br.com.gestaoservicos.security.ContextoUnidade;
import br.com.gestaoservicos.unidade.dto.CriarUnidadeRequestDTO;
import br.com.gestaoservicos.unidade.dto.RotaProtegidaResponseDTO;
import br.com.gestaoservicos.unidade.dto.UnidadeResponseDTO;
import br.com.gestaoservicos.unidade.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades")
public class UnidadeController {
    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) { this.unidadeService = unidadeService; }

    @PostMapping
    @PreAuthorize("@autorizacaoGeral.podeCriarUnidade()")
    @ResponseStatus(HttpStatus.CREATED)
    UnidadeResponseDTO criar(@Valid @RequestBody CriarUnidadeRequestDTO requisicao, Authentication autenticacao) {
        return unidadeService.criarComAdministrador(UUID.fromString(autenticacao.getName()), requisicao.nome());
    }

    @GetMapping("/atual/protegida")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN', 'GESTOR', 'OPERADOR')")
    RotaProtegidaResponseDTO consultarRotaProtegida() {
        var selecao = ContextoUnidade.atual()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Selecione uma unidade pelo cabeçalho X-Unidade-Id"));
        return new RotaProtegidaResponseDTO(
                selecao.unidadeId(), selecao.perfil(), "Acesso autorizado pela API");
    }
}
