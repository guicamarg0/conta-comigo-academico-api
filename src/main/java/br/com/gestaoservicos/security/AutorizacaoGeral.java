package br.com.gestaoservicos.security;

import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("autorizacaoGeral")
public class AutorizacaoGeral {
    private final UsuarioRepository usuarios;
    private final boolean somenteGeralCriaUnidade;
    public AutorizacaoGeral(UsuarioRepository usuarios,
            @Value("${app.acesso.somenteGeralCriaUnidade:false}") boolean somenteGeralCriaUnidade) {
        this.usuarios = usuarios;
        this.somenteGeralCriaUnidade = somenteGeralCriaUnidade;
    }
    public boolean administrador() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) return false;
        try {
            return usuarios.findById(UUID.fromString(autenticacao.getName()))
                    .map(br.com.gestaoservicos.usuario.model.Usuario::isAdministradorGeral).orElse(false);
        } catch (IllegalArgumentException e) { return false; }
    }
    public boolean podeCriarUnidade() { return !somenteGeralCriaUnidade || administrador(); }
}
