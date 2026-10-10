package br.com.gestaoservicos.configuracaounidade.service;

import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.mapper.ConfiguracaoUnidadeMapper;
import br.com.gestaoservicos.configuracaounidade.model.*;
import br.com.gestaoservicos.configuracaounidade.repository.*;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class ConfiguracaoUnidadeService {
    private final ConfiguracaoUnidadeRepository configuracoes;
    private final FormaRecebimentoRepository formas;
    private final UnidadeRepository unidades;
    private final ConfiguracaoUnidadeMapper mapper;

    public ConfiguracaoUnidadeService(ConfiguracaoUnidadeRepository configuracoes, FormaRecebimentoRepository formas, UnidadeRepository unidades, ConfiguracaoUnidadeMapper mapper) {
        this.configuracoes = configuracoes; this.formas = formas; this.unidades = unidades; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ConfiguracaoUnidadeResponseDTO consultar(UUID unidadeId) {
        var configuracao = configuracoes.findByUnidadeId(unidadeId);
        var pagamentos = formas.findAllByUnidadeIdOrderByNomeExibicao(unidadeId);
        if (configuracao.isPresent()) return mapper.paraResponseDTO(configuracao.get(), pagamentos);
        if (!unidades.existsById(unidadeId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada");
        return new ConfiguracaoUnidadeResponseDTO("", "", "", "", "", null, "", "",
                pagamentos.stream().map(mapper::paraResponseDTO).toList());
    }

    @Transactional(readOnly = true)
    public List<FormaRecebimentoResponseDTO> listarFormasAtivas(UUID unidadeId) {
        return formas.findAllByUnidadeIdOrderByNomeExibicao(unidadeId).stream().filter(FormaRecebimento::isAtiva).map(mapper::paraResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public DadosContratadoSnapshotDTO obterSnapshot(UUID unidadeId) {
        ConfiguracaoUnidade configuracao = configuracoes.findByUnidadeId(unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração da unidade não encontrada"));
        return mapper.paraSnapshot(configuracao, formas.findAllByUnidadeIdOrderByNomeExibicao(unidadeId));
    }

    @Transactional
    public ConfiguracaoUnidadeResponseDTO salvar(UUID unidadeId, ConfiguracaoUnidadeRequestDTO requisicao) {
        validarDocumento(requisicao.documento());
        validarFormas(requisicao.formasRecebimento());
        ConfiguracaoUnidade configuracao = configuracoes.findComBloqueioByUnidadeId(unidadeId).orElseGet(() -> {
            var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
            return configuracoes.save(new ConfiguracaoUnidade(unidade, limpar(requisicao.nomeRazaoSocial()), requisicao.documento(), limpar(requisicao.enderecoCompleto()), limparOpcional(requisicao.nomeFantasia()), limparOpcional(requisicao.email()), limparOpcional(requisicao.logoUrl()), limparOpcional(requisicao.responsavel()), limparOpcional(requisicao.condicoesPagamentoPadrao())));
        });
        configuracao.atualizar(limpar(requisicao.nomeRazaoSocial()), requisicao.documento(), limpar(requisicao.enderecoCompleto()), limparOpcional(requisicao.nomeFantasia()), limparOpcional(requisicao.email()), limparOpcional(requisicao.logoUrl()), limparOpcional(requisicao.responsavel()), limparOpcional(requisicao.condicoesPagamentoPadrao()));
        formas.deleteAllByUnidadeId(unidadeId);
        List<FormaRecebimento> novasFormas = requisicao.formasRecebimento().stream()
                .map(forma -> new FormaRecebimento(configuracao, forma.tipo(), limpar(forma.nomeExibicao()), limpar(forma.instrucoes()), forma.ativa(), forma.padrao()))
                .toList();
        formas.saveAll(novasFormas);
        return mapper.paraResponseDTO(configuracao, novasFormas.stream().sorted(java.util.Comparator.comparing(FormaRecebimento::getNomeExibicao)).toList());
    }

    private void validarFormas(List<FormaRecebimentoRequestDTO> formasRecebimento) {
        long padroesAtivos = formasRecebimento.stream().filter(forma -> forma.ativa() && forma.padrao()).count();
        if (padroesAtivos != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe exatamente uma forma de recebimento padrão ativa");
        if (formasRecebimento.stream().anyMatch(forma -> !forma.ativa() && forma.padrao())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma forma de recebimento inativa não pode ser padrão");
    }
    private void validarDocumento(String documento) {
        if (!documentoValido(documento)) throw new DocumentoInvalidoException();
    }
    private boolean documentoValido(String documento) {
        if (documento.chars().distinct().count() == 1) return false;
        if (documento.length() == 11) return validarDigitos(documento, new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2}) && validarDigitos(documento, new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        if (documento.length() == 14) return validarDigitos(documento, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}) && validarDigitos(documento, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return false;
    }
    private boolean validarDigitos(String documento, int[] pesos) {
        int soma = 0; for (int i = 0; i < pesos.length; i++) soma += (documento.charAt(i) - '0') * pesos[i];
        int digito = soma % 11; digito = digito < 2 ? 0 : 11 - digito;
        return documento.charAt(pesos.length) - '0' == digito;
    }
    private String limpar(String valor) { return valor.strip().replaceAll("\\s+", " "); }
    private String limparOpcional(String valor) { return valor == null || valor.isBlank() ? null : limpar(valor); }
}
