package br.com.gestaoservicos.security;

import br.com.gestaoservicos.compartilhado.erro.CodigosErroApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
public class FiltroOrigemSessao extends OncePerRequestFilter {
    private final List<String> origensPermitidas;
    private final ObjectMapper json;

    public FiltroOrigemSessao(
            @Value("${app.cors.origensPermitidas:http://localhost:3000}") List<String> origensPermitidas,
            ObjectMapper json) {
        this.origensPermitidas = origensPermitidas.stream().map(FiltroOrigemSessao::normalizar).toList();
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        if (!"POST".equalsIgnoreCase(requisicao.getMethod())) return true;
        String caminho = requisicao.getRequestURI();
        return !caminho.equals("/api/v1/autenticacao/renovar")
                && !caminho.equals("/api/v1/autenticacao/sair");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String origem = requisicao.getHeader("Origin");
        if (origem == null || !origensPermitidas.contains(normalizar(origem))) {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "Origem não autorizada para esta operação.");
            problema.setProperty("code", CodigosErroApi.para(HttpStatus.FORBIDDEN));
            problema.setProperty("path", requisicao.getRequestURI());
            resposta.setStatus(HttpStatus.FORBIDDEN.value());
            resposta.setContentType("application/problem+json");
            json.writeValue(resposta.getOutputStream(), problema);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static String normalizar(String origem) {
        String valor = origem == null ? "" : origem.strip();
        while (valor.endsWith("/")) valor = valor.substring(0, valor.length() - 1);
        return valor;
    }
}
