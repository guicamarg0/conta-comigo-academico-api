package br.com.gestaoservicos.security;

import br.com.gestaoservicos.associacao.model.Perfil;

import java.util.Optional;
import java.util.UUID;

public final class ContextoUnidade {
    private static final ThreadLocal<Selecao> ATUAL = new ThreadLocal<>();

    private ContextoUnidade() {}

    public static void definir(UUID unidadeId, Perfil perfil) { ATUAL.set(new Selecao(unidadeId, perfil)); }
    public static Optional<Selecao> atual() { return Optional.ofNullable(ATUAL.get()); }
    public static void limpar() { ATUAL.remove(); }

    public record Selecao(UUID unidadeId, Perfil perfil) {}
}
