package br.com.gestaoservicos.unidade.service;

import br.com.gestaoservicos.unidade.dto.*;
import br.com.gestaoservicos.unidade.service.UnidadeService;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import br.com.gestaoservicos.usuario.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;

@Service
@PreAuthorize("@autorizacaoGeral.administrador()")
public class AdministracaoGeralService {
    private final UnidadeService unidades;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    public AdministracaoGeralService(UnidadeService unidades, UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.unidades=unidades; this.usuarios=usuarios; this.senhas=senhas;
    }
    @Transactional(readOnly=true)
    public List<AssociacaoUnidadeResponseDTO> listar() { return unidades.listarParaAdministradorGeral(); }

    @Transactional
    public UnidadeResponseDTO criar(CriarUnidadeGeralRequestDTO dto) {
        String email=dto.emailAdministrador().trim().toLowerCase(Locale.ROOT);
        var existente=usuarios.findByEmailIgnoreCase(email);
        if (existente.isPresent() && dto.senhaAdministrador()!=null && !dto.senhaAdministrador().isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Este e-mail já está cadastrado. Deixe a senha vazia para vincular a conta existente.");
        var administrador=existente.orElseGet(() -> {
            if (dto.senhaAdministrador()==null || dto.senhaAdministrador().length()<8
                    || dto.senhaAdministrador().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe uma senha de 8 a 72 caracteres para o novo administrador (máximo 72 bytes).");
            return usuarios.save(new Usuario(dto.nomeAdministrador().trim(), email, senhas.encode(dto.senhaAdministrador())));
        });
        return unidades.criarComAdministrador(administrador.getId(),dto.nome());
    }
}
