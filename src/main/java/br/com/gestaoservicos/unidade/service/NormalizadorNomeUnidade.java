package br.com.gestaoservicos.unidade.service;

import java.text.Normalizer;
import java.util.Locale;

public final class NormalizadorNomeUnidade {
    private NormalizadorNomeUnidade() {}

    public static String normalizar(String nome) {
        String semAcentos = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcentos.strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public static String limparParaExibicao(String nome) {
        return nome.strip().replaceAll("\\s+", " ");
    }
}
