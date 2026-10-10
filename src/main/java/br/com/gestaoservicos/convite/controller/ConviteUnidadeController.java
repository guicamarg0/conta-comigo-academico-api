package br.com.gestaoservicos.convite.controller;

import br.com.gestaoservicos.convite.dto.AceitarConviteRequestDTO;
import br.com.gestaoservicos.convite.dto.ConviteUnidadeResponseDTO;
import br.com.gestaoservicos.convite.dto.CriarConviteUnidadeRequestDTO;
import br.com.gestaoservicos.convite.service.ConviteUnidadeService;
import br.com.gestaoservicos.security.ContextoUnidade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ConviteUnidadeController {
    private final ConviteUnidadeService servico;

    public ConviteUnidadeController(ConviteUnidadeService servico) {
        this.servico = servico;
    }

    @PostMapping("/convites/consultar")
    br.com.gestaoservicos.convite.dto.ConsultaConviteResponseDTO consultar(@Valid @RequestBody AceitarConviteRequestDTO requisicao) {
        return servico.consultar(requisicao.codigo());
    }
    @PostMapping("/convites/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    void cadastrar(@Valid @RequestBody br.com.gestaoservicos.convite.dto.CadastroConviteRequestDTO requisicao) {
        servico.cadastrar(requisicao);
    }

    @GetMapping("/unidades/atual/convites")
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    List<ConviteUnidadeResponseDTO> listar() {
        return servico.listar(unidadeAtual());
    }

    @PostMapping("/unidades/atual/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    ConviteUnidadeResponseDTO criar(@Valid @RequestBody CriarConviteUnidadeRequestDTO requisicao) {
        return servico.criar(unidadeAtual(), requisicao.perfil());
    }

    @DeleteMapping("/unidades/atual/convites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@autorizacaoUnidade.possuiAlgumPerfil('ADMIN','GESTOR')")
    void revogar(@PathVariable UUID id) {
        servico.revogar(unidadeAtual(), id);
    }

    @PostMapping("/convites/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void aceitar(@Valid @RequestBody AceitarConviteRequestDTO requisicao, Authentication autenticacao) {
        servico.aceitar(UUID.fromString(autenticacao.getName()), requisicao.codigo());
    }

    private UUID unidadeAtual() {
        return ContextoUnidade.atual().map(ContextoUnidade.Selecao::unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selecione uma unidade pelo cabeçalho X-Unidade-Id"));
    }
}
