package br.com.gestaoservicos.onboarding.controller;

import br.com.gestaoservicos.onboarding.dto.*;
import br.com.gestaoservicos.onboarding.service.OnboardingService;
import br.com.gestaoservicos.security.ContextoUnidade;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades/atual/onboarding")
@PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
public class OnboardingController {
    private final OnboardingService service;
    public OnboardingController(OnboardingService service) { this.service=service; }
    @PostMapping("/atualizar")
    public OnboardingResponseDTO atualizar(Principal usuario) { return service.atualizar(unidade(), UUID.fromString(usuario.getName())); }
    @PutMapping
    public OnboardingResponseDTO salvar(Principal usuario, @RequestBody OnboardingRequestDTO dados) {
        return service.salvar(unidade(), UUID.fromString(usuario.getName()), dados);
    }
    private UUID unidade() {
        return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione uma unidade."));
    }
}
