package br.com.gestaoservicos.arquivo.service;

public interface ArmazenamentoArquivos {
    boolean configurado();
    void salvar(String caminho, byte[] conteudo, String tipo);
    byte[] ler(String caminho);
}