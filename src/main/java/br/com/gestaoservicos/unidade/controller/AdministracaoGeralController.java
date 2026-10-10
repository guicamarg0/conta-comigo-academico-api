package br.com.gestaoservicos.unidade.controller;
import br.com.gestaoservicos.unidade.dto.*;
import br.com.gestaoservicos.unidade.service.AdministracaoGeralService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.List;
@RestController
@RequestMapping("/api/v1/administracao-geral/unidades")
@PreAuthorize("@autorizacaoGeral.administrador()")
public class AdministracaoGeralController {
    private final AdministracaoGeralService administracao;
    public AdministracaoGeralController(AdministracaoGeralService administracao) { this.administracao=administracao; }
    @GetMapping
    List<AssociacaoUnidadeResponseDTO> listar() { return administracao.listar(); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UnidadeResponseDTO criar(@Valid @RequestBody CriarUnidadeGeralRequestDTO dto) { return administracao.criar(dto); }
}
