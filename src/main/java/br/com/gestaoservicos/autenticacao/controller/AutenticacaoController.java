package br.com.gestaoservicos.autenticacao.controller;

import br.com.gestaoservicos.autenticacao.dto.CadastroRequestDTO;
import br.com.gestaoservicos.autenticacao.dto.LoginRequestDTO;
import br.com.gestaoservicos.autenticacao.dto.TokenResponseDTO;
import br.com.gestaoservicos.autenticacao.service.AutenticacaoService;
import br.com.gestaoservicos.usuario.dto.UsuarioResponseDTO;
import br.com.gestaoservicos.usuario.mapper.UsuarioMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {
    static final String COOKIE_RENOVACAO = "conta_comigo_renovacao";
    private final AutenticacaoService autenticacaoService;
    private final UsuarioMapper usuarioMapper;

    public AutenticacaoController(AutenticacaoService autenticacaoService, UsuarioMapper usuarioMapper) {
        this.autenticacaoService = autenticacaoService;
        this.usuarioMapper = usuarioMapper;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResponseDTO cadastrar(@Valid @RequestBody CadastroRequestDTO requisicao) {
        return usuarioMapper.paraResponseDTO(
                autenticacaoService.cadastrar(requisicao.nome(), requisicao.email(), requisicao.senha()));
    }

    @PostMapping("/login")
    TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO requisicao, HttpServletResponse resposta) {
        var resultado = autenticacaoService.autenticar(
                requisicao.email(), requisicao.senha(), Boolean.TRUE.equals(requisicao.manterConectado()));
        adicionarCookie(resposta, resultado.tokenRenovacao(), resultado.persistente());
        return resultado.tokenAcesso();
    }

    @PostMapping("/renovar")
    TokenResponseDTO renovar(HttpServletRequest requisicao, HttpServletResponse resposta) {
        var resultado = autenticacaoService.renovar(lerCookie(requisicao));
        adicionarCookie(resposta, resultado.tokenRenovacao(), resultado.persistente());
        return resultado.tokenAcesso();
    }

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(HttpServletRequest requisicao, HttpServletResponse resposta) {
        autenticacaoService.sair(lerCookie(requisicao));
        resposta.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_RENOVACAO, "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/api/v1/autenticacao")
                .maxAge(Duration.ZERO).build().toString());
    }

    private String lerCookie(HttpServletRequest requisicao) {
        if (requisicao.getCookies() == null) return null;
        for (Cookie cookie : requisicao.getCookies()) {
            if (COOKIE_RENOVACAO.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void adicionarCookie(HttpServletResponse resposta, String token, boolean persistente) {
        var construtor = ResponseCookie.from(COOKIE_RENOVACAO, token)
                .httpOnly(true).secure(true).sameSite("Lax").path("/api/v1/autenticacao");
        if (persistente) construtor.maxAge(Duration.ofDays(30));
        resposta.addHeader(HttpHeaders.SET_COOKIE, construtor.build().toString());
    }
}
