package br.com.gestaoservicos.config;

import br.com.gestaoservicos.unidade.service.NomeUnidadeJaExistenteException;
import br.com.gestaoservicos.associacao.service.RegraAssociacaoException;
import br.com.gestaoservicos.configuracaounidade.service.DocumentoInvalidoException;
import br.com.gestaoservicos.contratante.service.DocumentoContratanteJaExisteException;
import br.com.gestaoservicos.compartilhado.erro.CodigosErroApi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratadorExcecoesApi {
    @ExceptionHandler(DocumentoContratanteJaExisteException.class)
    ProblemDetail tratarDocumentoContratanteJaExiste(DocumentoContratanteJaExisteException excecao, HttpServletRequest requisicao) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        detalhe.setProperty("code", "CONTRATANTE_DOCUMENTO_JA_EXISTE"); detalhe.setProperty("path", requisicao.getRequestURI()); return detalhe;
    }
    @ExceptionHandler(DocumentoInvalidoException.class)
    ProblemDetail tratarDocumentoInvalido(DocumentoInvalidoException excecao, HttpServletRequest requisicao) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        detalhe.setProperty("code", "DOCUMENTO_INVALIDO");
        detalhe.setProperty("path", requisicao.getRequestURI());
        return detalhe;
    }
    @ExceptionHandler(RegraAssociacaoException.class)
    ProblemDetail tratarRegraAssociacao(RegraAssociacaoException excecao, HttpServletRequest requisicao) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        detalhe.setProperty("code", excecao.getCodigo());
        detalhe.setProperty("path", requisicao.getRequestURI());
        return detalhe;
    }

    @ExceptionHandler(NomeUnidadeJaExistenteException.class)
    ProblemDetail tratarNomeUnidadeJaExistente(
            NomeUnidadeJaExistenteException excecao,
            HttpServletRequest requisicao) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        detalhe.setProperty("code", "UNIDADE_NOME_JA_EXISTENTE");
        detalhe.setProperty("path", requisicao.getRequestURI());
        return detalhe;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail tratarStatus(ResponseStatusException excecao, HttpServletRequest requisicao) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(excecao.getStatusCode(), excecao.getReason());
        detalhe.setProperty("code", CodigosErroApi.para(excecao.getStatusCode()));
        detalhe.setProperty("path", requisicao.getRequestURI());
        return detalhe;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail tratarValidacao(MethodArgumentNotValidException excecao, HttpServletRequest requisicao) {
        String mensagem = "Confira os campos destacados e tente novamente.";
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, mensagem);
        detalhe.setProperty("code", "VALIDACAO_INVALIDA");
        var campos = new java.util.LinkedHashMap<String, String>();
        excecao.getBindingResult().getFieldErrors().forEach(erro -> campos.putIfAbsent(erro.getField(), mensagemCampo(erro.getCode(), erro.getDefaultMessage())));
        detalhe.setProperty("campos", campos);
        detalhe.setProperty("path", requisicao.getRequestURI());
        return detalhe;
    }

    private String mensagemCampo(String codigo, String original) {
        return switch (codigo == null ? "" : codigo) {
            case "NotBlank", "NotNull", "NotEmpty" -> "Preencha este campo.";
            case "Email" -> "Informe um e-mail válido.";
            case "Size" -> "Confira o tamanho permitido para este campo.";
            case "DecimalMin", "Min", "Max", "Digits" -> "Informe um valor dentro dos limites permitidos.";
            default -> original;
        };
    }

}
