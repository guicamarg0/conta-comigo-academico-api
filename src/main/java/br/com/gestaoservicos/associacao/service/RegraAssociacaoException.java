package br.com.gestaoservicos.associacao.service;

public class RegraAssociacaoException extends RuntimeException {
    private final String codigo;

    public RegraAssociacaoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
