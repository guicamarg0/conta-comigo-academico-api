package br.com.gestaoservicos.autenticacao.controller;

import br.com.gestaoservicos.autenticacao.dto.*;
import br.com.gestaoservicos.autenticacao.service.RecuperacaoSenhaService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class RecuperacaoSenhaController {
    private final RecuperacaoSenhaService service;
    public RecuperacaoSenhaController(RecuperacaoSenhaService service) { this.service = service; }
    @PostMapping("/autenticacao/recuperar-senha")
    Map<String, String> solicitar(@Valid @RequestBody RecuperarSenhaRequestDTO requisicao) {
        service.solicitar(requisicao.email());
        return Map.of("mensagem", "Se houver uma conta com este e-mail, você receberá um link para redefinir a senha.");
    }
    @PostMapping("/autenticacao/redefinir-senha") @ResponseStatus(HttpStatus.NO_CONTENT)
    void redefinir(@Valid @RequestBody RedefinirSenhaRequestDTO requisicao) { service.redefinir(requisicao.token(), requisicao.senha()); }
    @PostMapping("/unidades/atual/usuarios/{id}/recuperar-senha") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    void solicitarPeloGestor(@PathVariable UUID id) {
        var unidade = ContextoUnidade.atual().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade"));
        service.solicitarPeloGestor(unidade.unidadeId(), id);
    }
}
