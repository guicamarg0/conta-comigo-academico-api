package br.com.gestaoservicos.configuracaounidade.controller;

import br.com.gestaoservicos.configuracaounidade.dto.FormaRecebimentoResponseDTO;
import br.com.gestaoservicos.configuracaounidade.service.ConfiguracaoUnidadeService;
import br.com.gestaoservicos.security.ContextoUnidade;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/unidades/atual/formas-recebimento")
public class FormaRecebimentoController {
    private final ConfiguracaoUnidadeService service;
    public FormaRecebimentoController(ConfiguracaoUnidadeService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR','OPERADOR','CONSULTA')")
    public List<FormaRecebimentoResponseDTO> listar() {
        var unidadeId = ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma unidade"));
        return service.listarFormasAtivas(unidadeId);
    }
}
