package br.com.gestaoservicos.usuario.mapper;

import br.com.gestaoservicos.usuario.dto.UsuarioResponseDTO;
import br.com.gestaoservicos.usuario.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public UsuarioResponseDTO paraResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
