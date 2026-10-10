package br.com.gestaoservicos.usuario.controller;

import br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO;
import br.com.gestaoservicos.usuario.dto.MeResponseDTO;
import br.com.gestaoservicos.usuario.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final UsuarioService usuarioService;

    public MeController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @GetMapping
    MeResponseDTO consultar(Authentication autenticacao) {
        return usuarioService.consultarMe(UUID.fromString(autenticacao.getName()));
    }

    @PostMapping("/unidades/{unidadeId}/selecionar")
    AssociacaoUnidadeResponseDTO selecionarUnidade(@PathVariable UUID unidadeId, Authentication autenticacao) {
        return usuarioService.selecionarUnidade(UUID.fromString(autenticacao.getName()), unidadeId);
    }
}
