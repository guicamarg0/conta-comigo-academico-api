package br.com.gestaoservicos.security;

import br.com.gestaoservicos.associacao.model.Perfil;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("autorizacaoUnidade")
public class AutorizacaoUnidade {
    public boolean possuiAlgumPerfil(String... perfisPermitidos) {
        return ContextoUnidade.atual()
                .map(selecao -> Arrays.stream(perfisPermitidos)
                        .map(Perfil::valueOf)
                        .anyMatch(selecao.perfil()::equals))
                .orElse(false);
    }
}
