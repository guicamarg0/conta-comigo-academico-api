package br.com.gestaoservicos.unidade.service;

public class NomeUnidadeJaExistenteException extends RuntimeException {
    public NomeUnidadeJaExistenteException() {
        super("Já existe uma unidade com esse nome.");
    }
}
