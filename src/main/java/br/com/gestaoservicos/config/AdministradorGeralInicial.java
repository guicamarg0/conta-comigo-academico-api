package br.com.gestaoservicos.config;

import br.com.gestaoservicos.usuario.model.Usuario;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Component
public class AdministradorGeralInicial implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final String email, senha;
    public AdministradorGeralInicial(UsuarioRepository usuarios, PasswordEncoder senhas,
            @Value("${ADMIN_GERAL_EMAIL:}") String email, @Value("${ADMIN_GERAL_SENHA:}") String senha) {
        this.usuarios = usuarios; this.senhas = senhas; this.email = email.trim().toLowerCase(Locale.ROOT); this.senha = senha;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (email.isBlank()) return;
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 254)
            throw new IllegalStateException("Configure um ADMIN_GERAL_EMAIL válido.");
        var existente = usuarios.findByEmailIgnoreCase(email);
        if (existente.isPresent()) {
            if (!existente.get().isAdministradorGeral())
                throw new IllegalStateException("A conta informada já existe sem acesso geral. Promoção automática bloqueada.");
            return;
        }
        if (senha.length() < 12 || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("ADMIN_GERAL_SENHA deve ter pelo menos 12 caracteres e no máximo 72 bytes.");
        var usuario = new Usuario("Administrador geral", email, senhas.encode(senha));
        usuario.habilitarAdministradorGeral();
        usuarios.save(usuario);
    }
}
