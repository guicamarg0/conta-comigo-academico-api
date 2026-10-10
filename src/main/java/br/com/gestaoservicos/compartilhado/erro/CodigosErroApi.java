package br.com.gestaoservicos.compartilhado.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class CodigosErroApi {
    private CodigosErroApi() {}

    public static String para(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.BAD_REQUEST)) return "REQUISICAO_INVALIDA";
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED)) return "NAO_AUTENTICADO";
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN)) return "ACESSO_NEGADO";
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) return "RECURSO_NAO_ENCONTRADO";
        if (status.isSameCodeAs(HttpStatus.CONFLICT)) return "CONFLITO_DE_REGRA";
        return "ERRO_DE_NEGOCIO";
    }
}
