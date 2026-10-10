package br.com.gestaoservicos.convite.service;

import br.com.gestaoservicos.associacao.model.Associacao;
import br.com.gestaoservicos.associacao.model.Perfil;
import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import br.com.gestaoservicos.convite.dto.ConviteUnidadeResponseDTO;
import br.com.gestaoservicos.convite.mapper.ConviteUnidadeMapper;
import br.com.gestaoservicos.convite.model.ConviteUnidade;
import br.com.gestaoservicos.convite.repository.ConviteUnidadeRepository;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ConviteUnidadeService {
    private final ConviteUnidadeRepository convites;
    private final UnidadeRepository unidades;
    private final UsuarioRepository usuarios;
    private final AssociacaoRepository associacoes;
    private final ConviteUnidadeMapper mapper;
    private final org.springframework.security.crypto.password.PasswordEncoder codificadorSenha;
    private final SecureRandom aleatorio = new SecureRandom();

    public ConviteUnidadeService(ConviteUnidadeRepository convites, UnidadeRepository unidades,
                                 UsuarioRepository usuarios, AssociacaoRepository associacoes,
                                 ConviteUnidadeMapper mapper, org.springframework.security.crypto.password.PasswordEncoder codificadorSenha) {
        this.convites = convites;
        this.unidades = unidades;
        this.usuarios = usuarios;
        this.associacoes = associacoes;
        this.mapper = mapper;
        this.codificadorSenha = codificadorSenha;
    }
    @Transactional(readOnly = true)
    public br.com.gestaoservicos.convite.dto.ConsultaConviteResponseDTO consultar(String codigo) {
        var convite = convites.findByTokenHash(hash(codigo)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado. Confira o link recebido."));
        validarDisponivel(convite);
        return new br.com.gestaoservicos.convite.dto.ConsultaConviteResponseDTO(convite.getUnidade().getNome(), convite.getPerfil(), convite.getExpiraEm());
    }
    @Transactional
    public void cadastrar(br.com.gestaoservicos.convite.dto.CadastroConviteRequestDTO dto) {
        var convite = convites.buscarPorTokenHashComBloqueio(hash(dto.codigo())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado."));
        validarDisponivel(convite);
        var email = dto.email().strip().toLowerCase(java.util.Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já possui conta. Use a opção Entrar.");
        var usuario = usuarios.save(new br.com.gestaoservicos.usuario.model.Usuario(dto.nome().strip(), email, codificadorSenha.encode(dto.senha())));
        associacoes.save(new Associacao(usuario, convite.getUnidade(), convite.getPerfil()));
        convite.aceitar();
    }
    private void validarDisponivel(ConviteUnidade convite) {
        if (convite.getExpiraEm().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.GONE, "Este convite expirou. Peça um novo link ao gestor.");
        if (!convite.estaDisponivel()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este convite foi utilizado ou revogado. Peça um novo link ao gestor.");
    }
    @Transactional
    public ConviteUnidadeResponseDTO criar(UUID unidadeId, Perfil perfil) {
        if (perfil == Perfil.ADMIN && br.com.gestaoservicos.security.ContextoUnidade.atual()
            .map(c->c.perfil()==Perfil.GESTOR).orElse(false))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente administradores podem convidar administradores.");
        var unidade = unidades.findById(unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade nao encontrada"));
        String codigo = gerarCodigo();
        var convite = convites.save(new ConviteUnidade(unidade, perfil, hash(codigo), Instant.now().plus(7, ChronoUnit.DAYS)));
        return mapper.paraResponseDTO(convite, codigo);
    }
    @Transactional(readOnly = true)
    public List<ConviteUnidadeResponseDTO> listar(UUID unidadeId) {
        return convites.findAllByUnidadeIdOrderByExpiraEmDesc(unidadeId).stream()
                .map(convite -> mapper.paraResponseDTO(convite, null))
                .toList();
    }
    @Transactional
    public void revogar(UUID unidadeId, UUID conviteId) {
        var convite = convites.findByIdAndUnidadeId(conviteId, unidadeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link de acesso nao encontrado"));
        validarDisponivel(convite);
        convite.revogar();
    }
    @Transactional
    public void aceitar(UUID usuarioId, String codigo) {
        var convite = convites.buscarPorTokenHashComBloqueio(hash(codigo)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link de acesso invalido"));
        validarDisponivel(convite);
        var usuario = usuarios.findById(usuarioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
        if (associacoes.existsByUsuarioIdAndUnidadeIdAndAtivaTrue(usuarioId, convite.getUnidade().getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Voce ja possui acesso a esta unidade");
        var existente = associacoes.findByUsuarioIdAndUnidadeId(usuarioId, convite.getUnidade().getId());
        if (existente.isPresent()) existente.get().reativar(convite.getPerfil());
        else associacoes.save(new Associacao(usuario, convite.getUnidade(), convite.getPerfil()));
        convite.aceitar();
    }
    private String gerarCodigo() { byte[] bytes = new byte[24]; aleatorio.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String texto) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException excecao) { throw new IllegalStateException(excecao); } }
}
