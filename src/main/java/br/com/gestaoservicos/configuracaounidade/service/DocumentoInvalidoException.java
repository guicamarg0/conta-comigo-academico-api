package br.com.gestaoservicos.configuracaounidade.service;

public class DocumentoInvalidoException extends RuntimeException {
    public DocumentoInvalidoException() { super("Informe um CPF ou CNPJ válido."); }
}
