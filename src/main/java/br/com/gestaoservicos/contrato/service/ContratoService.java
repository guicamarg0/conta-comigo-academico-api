package br.com.gestaoservicos.contrato.service;

import br.com.gestaoservicos.compartilhado.paginacao.PaginaResponseDTO;
import br.com.gestaoservicos.contrato.dto.*;
import br.com.gestaoservicos.contrato.model.*;
import br.com.gestaoservicos.contrato.repository.ContratoRepository;
import br.com.gestaoservicos.contrato.repository.ContratoSnapshotRepository;
import br.com.gestaoservicos.empresa.repository.EmpresaRepository;
import br.com.gestaoservicos.modelocontrato.repository.ModeloContratoRepository;
import br.com.gestaoservicos.orcamento.model.*;
import br.com.gestaoservicos.orcamento.repository.*;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.Map;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class ContratoService {
    private final br.com.gestaoservicos.compartilhado.SequenciaOperacional sequencia;
    private final ContratoRepository contratos;
    private final ContratoSnapshotRepository snapshots;
    private final OrcamentoRepository orcamentos;
    private final RevisaoOrcamentoRepository revisoes;
    private final UnidadeRepository unidades;
    private final GeradorPdfContrato gerador;
    private final EmpresaRepository empresas;
    private final ModeloContratoRepository modelos;
    private final br.com.gestaoservicos.contrato.repository.AtividadeContratoRepository atividades;
    private final br.com.gestaoservicos.usuario.repository.UsuarioRepository usuarios;
    private final org.springframework.data.domain.AuditorAware<UUID> auditor;
    public ContratoService(ContratoRepository contratos, ContratoSnapshotRepository snapshots, OrcamentoRepository orcamentos,
                           RevisaoOrcamentoRepository revisoes, UnidadeRepository unidades, GeradorPdfContrato gerador, EmpresaRepository empresas, ModeloContratoRepository modelos, br.com.gestaoservicos.compartilhado.SequenciaOperacional sequencia,
                           br.com.gestaoservicos.contrato.repository.AtividadeContratoRepository atividades,
                           br.com.gestaoservicos.usuario.repository.UsuarioRepository usuarios, org.springframework.data.domain.AuditorAware<UUID> auditor) {
        this.sequencia = sequencia;
        this.contratos = contratos; this.snapshots = snapshots; this.orcamentos = orcamentos; this.revisoes = revisoes;
        this.unidades = unidades; this.gerador = gerador; this.empresas = empresas; this.modelos = modelos;
        this.atividades = atividades; this.usuarios = usuarios; this.auditor = auditor;
    }
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ContratoResponseDTO> listar(UUID unidadeId, String busca, StatusContrato status, Boolean ativo, Pageable pagina) {
        return PaginaResponseDTO.de(contratos.buscar(unidadeId, busca == null ? "" : busca.strip(), status, ativo, pagina).map(this::resposta));
    }
    @Transactional(readOnly = true)
    public ContratoResponseDTO consultar(UUID unidadeId, UUID id) { return resposta(obter(unidadeId, id)); }
    @Transactional
    public ContratoResponseDTO criar(UUID unidadeId, ContratoRequestDTO dto) {
        validarVigencia(dto);
        var orcamento = orcamentos.findByIdAndUnidade_Id(dto.orcamentoId(), unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orçamento não pertence à unidade"));
        if (orcamento.getStatus() != StatusOrcamento.APROVADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente orçamento aprovado pode gerar contrato");
        if (contratos.existsByOrcamento_Id(orcamento.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este orçamento já possui contrato");
        var revisao = revisoes.findByOrcamentoIdAndNumeroRevisao(orcamento.getId(), orcamento.getRevisaoAtual())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Revisão do orçamento não encontrada"));
        if (revisao.getContratante() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Orçamento sem cliente");
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var empresa = dto.empresaId() == null ? null : empresas.findByIdAndUnidadeId(dto.empresaId(), unidadeId).filter(e -> e.isAtivo()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa contratada não encontrada ou inativa"));
        var contrato = new Contrato(unidade, revisao.getContratante(), orcamento, revisao.getTotalFinal(),
                revisao.getCondicoesPagamento(), dto.modelo(), dto.objeto(), dto.clausulasAdicionais(),
                dto.inicioVigencia(), dto.fimVigencia(), empresa);
        contrato.definirNumero(sequencia.proximo(unidadeId, "CT"));
        contrato.atualizarRascunho(dto.modeloContratoId(), resolverConteudo(unidadeId, contrato, dto.modeloContratoId(), dto.conteudoRascunho()));
        contrato = contratos.save(contrato); snapshots.save(new ContratoSnapshot(contrato));
        atividade(contrato, TipoAtividadeContrato.CRIADO, "Contrato criado");
        return resposta(contrato);
    }
    @Transactional
    public ContratoResponseDTO atualizar(UUID unidadeId, UUID id, ContratoRequestDTO dto) {
        validarVigencia(dto);
        var contrato = obter(unidadeId, id);
        var anterior = estadoConteudo(contrato);
        if (!contrato.getOrcamento().getId().equals(dto.orcamentoId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A origem do contrato não pode mudar");
        if (contrato.getStatus() == StatusContrato.ENCERRADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contrato encerrado não pode ser editado");
        if (dto.empresaId() != null) {
            var empresa = empresas.findByIdAndUnidadeId(dto.empresaId(), unidadeId).filter(e -> e.isAtivo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa contratada não encontrada ou inativa"));
            contrato.atualizarEmpresa(empresa);
        }
        contrato.atualizar(dto.modelo(), dto.objeto(), dto.clausulasAdicionais(), dto.inicioVigencia(), dto.fimVigencia());
        contrato.atualizarRascunho(dto.modeloContratoId(), resolverConteudo(unidadeId, contrato, dto.modeloContratoId(), dto.conteudoRascunho()));
        if (!anterior.equals(estadoConteudo(contrato))) {
            contrato.avancarVersao();
            snapshots.save(new ContratoSnapshot(contrato));
            atividade(contrato, TipoAtividadeContrato.EDITADO, "Conteúdo salvo na versão " + contrato.getNumeroVersao());
        }
        return resposta(contrato);
    }
    @Transactional
    public ContratoResponseDTO encerrar(UUID unidadeId, UUID id) {
        var contrato = obter(unidadeId, id);
        if (contrato.getStatus() != StatusContrato.VIGENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente contrato vigente pode ser encerrado");
        contrato.encerrar();
        atividade(contrato, TipoAtividadeContrato.ENCERRADO, "Contrato encerrado");
        return resposta(contrato);
    }
    @Transactional
    public ContratoResponseDTO marcarVigente(UUID unidadeId, UUID id) {
        var contrato = obter(unidadeId, id);
        if (contrato.getStatus() != StatusContrato.AGUARDANDO_ASSINATURA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente contrato aguardando assinatura pode ser marcado como vigente");
        contrato.marcarVigente();
        atividade(contrato, TipoAtividadeContrato.VIGENTE, "Contrato marcado como vigente");
        return resposta(contrato);
    }
    @Transactional
    public ContratoResponseDTO definirAtivo(UUID unidadeId, UUID id, boolean ativo) {
        var contrato = obter(unidadeId, id);
        if (contrato.isAtivo() != ativo) {
            contrato.definirAtivo(ativo);
            atividade(contrato, ativo ? TipoAtividadeContrato.REATIVADO : TipoAtividadeContrato.INATIVADO,
                    ativo ? "Cadastro reativado" : "Cadastro inativado");
        }
        return resposta(contrato);
    }
    @Transactional
    public ContratoResponseDTO restaurar(UUID unidadeId, UUID id, UUID versaoId) {
        var contrato = obter(unidadeId, id);
        if (contrato.getStatus() == StatusContrato.ENCERRADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contrato encerrado não pode ser restaurado");
        var versao = snapshots.findByIdAndContrato_Id(versaoId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão não encontrada neste contrato"));
        contrato.atualizarRascunho(contrato.getModeloContratoId(), versao.getConteudo());
        contrato.avancarVersao();
        snapshots.save(new ContratoSnapshot(contrato));
        atividade(contrato, TipoAtividadeContrato.RESTAURADO, "Conteúdo da versão " + versao.getNumeroVersao() + " restaurado na versão " + contrato.getNumeroVersao());
        return resposta(contrato);
    }
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ContratoSnapshotResponseDTO> versoes(UUID unidadeId, UUID id, Pageable pagina) {
        obter(unidadeId, id);
        return PaginaResponseDTO.de(snapshots.findByContrato_IdOrderByNumeroVersaoDesc(id, pagina).map(this::respostaSnapshot));
    }
    @Transactional(readOnly = true)
    public PaginaResponseDTO<AtividadeContratoResponseDTO> atividades(UUID unidadeId, UUID id, Pageable pagina) {
        obter(unidadeId, id);
        return PaginaResponseDTO.de(atividades.findByContrato_IdOrderByOcorridoEmDescIdDesc(id, pagina).map(a ->
                new AtividadeContratoResponseDTO(a.getId(), a.getTipo(), a.getDescricao(), a.getUsuarioId(), autor(a.getUsuarioId()), a.getOcorridoEm())));
    }
    private void atividade(Contrato contrato, TipoAtividadeContrato tipo, String descricao) {
        atividades.save(new AtividadeContrato(contrato, tipo, descricao, auditor.getCurrentAuditor().orElse(null)));
    }
    private String autor(UUID usuarioId) {
        return usuarioId == null ? "Sistema" : usuarios.findById(usuarioId).map(u -> u.getNome()).orElse("Usuário indisponível");
    }
    private ContratoSnapshotResponseDTO respostaSnapshot(ContratoSnapshot s) {
        return new ContratoSnapshotResponseDTO(s.getId(), s.getNumeroVersao(), s.getConteudo(), s.getCriadoEm(), s.getCriadoPor(), autor(s.getCriadoPor()));
    }
    private java.util.List<Object> estadoConteudo(Contrato c) {
        return java.util.Arrays.asList(c.getModelo(), c.getObjeto(), c.getClausulasAdicionais(), c.getInicioVigencia(),
                c.getFimVigencia(), c.getModeloContratoId(), c.getConteudoRascunho(), c.getEmpresa() == null ? null : c.getEmpresa().getId());
    }
    @Transactional(readOnly = true)
    public byte[] pdf(UUID unidadeId, UUID id) { return gerador.gerar(obter(unidadeId, id)); }
    @Transactional(readOnly = true)
    public ContratoPreviaResponseDTO previa(UUID unidadeId, ContratoPreviaRequestDTO dto) {
        var orcamento = orcamentos.findByIdAndUnidade_Id(dto.orcamentoId(), unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orçamento não pertence à unidade"));
        var revisao = revisoes.findByOrcamentoIdAndNumeroRevisao(orcamento.getId(), orcamento.getRevisaoAtual())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Revisão do orçamento não encontrada"));
        var empresa = empresas.findByIdAndUnidadeId(dto.empresaId(), unidadeId).filter(e -> e.isAtivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa contratada não encontrada ou inativa"));
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var contrato = new Contrato(unidade, revisao.getContratante(), orcamento, revisao.getTotalFinal(), revisao.getCondicoesPagamento(), "Prévia", "Conforme modelo", null, java.time.LocalDate.now(), java.time.LocalDate.now(), empresa);
        return new ContratoPreviaResponseDTO(resolverConteudo(unidadeId, contrato, dto.modeloContratoId(), dto.conteudoRascunho()), gerador.camposAssinatura(contrato));
    }
    @Transactional(readOnly = true)
    public java.util.List<ContratoSnapshotResponseDTO> snapshots(UUID unidadeId, UUID id) {
        obter(unidadeId, id);
        return snapshots.findByContrato_IdOrderByNumeroVersaoDesc(id).stream().map(this::respostaSnapshot).toList();
    }
    private void validarVigencia(ContratoRequestDTO dto) {
        if (dto.inicioVigencia().isAfter(dto.fimVigencia()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A vigência final deve ser posterior ao início");
    }
    private String resolverConteudo(UUID unidadeId, Contrato contrato, UUID modeloId, String conteudoInformado) {
        String base = conteudoInformado;
        if (base == null || base.isBlank()) {
            base = modeloId == null ? null : modelos.findByIdAndUnidade_Id(modeloId, unidadeId)
                    .map(m -> m.getConteudo()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modelo de contrato não encontrado"));
            if (base == null || base.isBlank()) base = contrato.getObjeto() + "\n\n" + (contrato.getClausulasAdicionais() == null ? "" : contrato.getClausulasAdicionais());
        }
        var cliente = contrato.getContratante();
        var contato = cliente.getContatos().isEmpty() ? null : cliente.getContatos().get(0);
        var empresa = contrato.getEmpresa();
        var revisao = revisoes.findByOrcamentoIdAndNumeroRevisao(contrato.getOrcamento().getId(), contrato.getOrcamento().getRevisaoAtual()).orElse(null);
        String enderecoEmpresa = empresa == null ? "" : String.join(", ", java.util.stream.Stream.of(empresa.getLogradouro(), empresa.getNumero(), empresa.getComplemento(), empresa.getBairro()).filter(v -> v != null && !v.isBlank()).toList());
        String cidadeEmpresa = empresa == null ? "" : String.join("/", java.util.stream.Stream.of(empresa.getCidade(), empresa.getEstado()).filter(v -> v != null && !v.isBlank()).toList());
        String valor = "R$ " + contrato.getValorTotal().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        Map<String, String> variaveis = Map.ofEntries(
                Map.entry("@RazaoSocialCliente", texto(cliente.getNomeRazaoSocial())), Map.entry("@NomeFantasiaCliente", texto(cliente.getNomeFantasia(), cliente.getNomeRazaoSocial())), Map.entry("@DocumentoCliente", br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatar(cliente.getDocumento())), Map.entry("@EnderecoCliente", texto(cliente.getEndereco())), Map.entry("@CidadeCliente", ""), Map.entry("@TelefoneCliente", contato == null ? "" : texto(contato.getTelefone())), Map.entry("@EmailCliente", contato == null ? "" : texto(contato.getEmail())), Map.entry("@RepresentanteCliente", contato == null ? "" : texto(contato.getNome())),
                Map.entry("@razaoSocialEmpresa", texto(cliente.getNomeRazaoSocial())), Map.entry("@nomeEmpresaContratada", empresa == null ? "" : texto(empresa.getRazaoSocial())),
                Map.entry("@RazaoSocialContratada", empresa == null ? "" : texto(empresa.getRazaoSocial())), Map.entry("@NomeFantasiaContratada", empresa == null ? "" : texto(empresa.getNomeFantasia(), empresa.getRazaoSocial())), Map.entry("@CNPJContratada", empresa == null ? "" : br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatar(empresa.getCnpj())), Map.entry("@DocumentoContratada", empresa == null ? "" : br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatar(empresa.getCnpj())), Map.entry("@TipoDocumentoContratada", empresa != null && empresa.getCnpj().length()==11 ? "CPF" : "CNPJ"), Map.entry("@TipoDocumentoCliente", cliente.getDocumento().length()==11 ? "CPF" : "CNPJ"), Map.entry("@EnderecoContratada", enderecoEmpresa), Map.entry("@CidadeContratada", cidadeEmpresa), Map.entry("@TelefoneContratada", empresa == null ? "" : texto(empresa.getTelefone())), Map.entry("@EmailContratada", empresa == null ? "" : texto(empresa.getEmail())), Map.entry("@RepresentanteContratada", ""),
                Map.entry("@Orcamento", "Orçamento #" + contrato.getOrcamento().getNumero()), Map.entry("@DataOrcamento", ""), Map.entry("@DescricaoServico", revisao == null || revisao.getItens().isEmpty() ? texto(contrato.getObjeto()) : texto(revisao.getItens().get(0).getDescricao())), Map.entry("@ValorOrcamento", valor), Map.entry("@NumeroContrato", contrato.getNumero()),
                Map.entry("@CondicoesPagamento", texto(contrato.getCondicoesPagamento())), Map.entry("@FormaPagamento", texto(contrato.getCondicoesPagamento())), Map.entry("@ValorEntrada", ""), Map.entry("@ValorSaldo", ""), Map.entry("@DadosBancarios", ""),
                Map.entry("@DataInicio", contrato.getInicioVigencia().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))), Map.entry("@DataFim", contrato.getFimVigencia().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))), Map.entry("@PrazoExecucao", ""), Map.entry("@DataAssinatura", contrato.getAssinadoEm() == null ? "" : contrato.getAssinadoEm().format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))
        );
        for (var entrada : variaveis.entrySet()) base = base.replace(entrada.getKey(), org.jsoup.nodes.Entities.escape(entrada.getValue()));
        return br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatarNoTexto(base, cliente.getDocumento(), empresa == null ? null : empresa.getCnpj());
    }
    private String texto(String valor) { return valor == null ? "" : valor; }
    private String texto(String preferido, String alternativo) { return preferido == null || preferido.isBlank() ? texto(alternativo) : preferido; }
    private Contrato obter(UUID unidadeId, UUID id) {
        return contratos.findByIdAndUnidade_Id(id, unidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
    }
    private ContratoResponseDTO resposta(Contrato c) {
        return new ContratoResponseDTO(c.getId(), c.getNumero(), c.getContratante().getId(),
                c.getContratante().getNomeFantasia() == null ? c.getContratante().getNomeRazaoSocial() : c.getContratante().getNomeFantasia(),
                c.getOrcamento().getId(), c.getOrcamento().getNumero(), c.getModelo(), c.getObjeto(),
                c.getClausulasAdicionais(), c.getInicioVigencia(), c.getFimVigencia(), c.getValorTotal(),
                c.getCondicoesPagamento(), c.getStatus(), c.getNumeroVersao(), c.getAssinadoPor(),
                c.getAssinadoEm(), c.getCanalAssinatura(), c.getEvidenciaUrl(), c.getModeloContratoId(), br.com.gestaoservicos.compartilhado.DocumentoFiscal.formatarNoTexto(c.getConteudoRascunho(), c.getContratante().getDocumento(), c.getEmpresa() == null ? null : c.getEmpresa().getCnpj()),
                c.getEmpresa() == null ? null : c.getEmpresa().getId(), c.getEmpresa() == null ? null : (c.getEmpresa().getNomeFantasia() == null ? c.getEmpresa().getRazaoSocial() : c.getEmpresa().getNomeFantasia()), c.isAtivo());
    }
}
