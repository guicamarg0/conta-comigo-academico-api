package br.com.gestaoservicos.autenticacao.service;

import br.com.gestaoservicos.autenticacao.dto.TokenResponseDTO;
import br.com.gestaoservicos.autenticacao.model.SessaoRenovacao;
import br.com.gestaoservicos.autenticacao.repository.SessaoRenovacaoRepository;
import br.com.gestaoservicos.security.PropriedadesJwt;
import br.com.gestaoservicos.usuario.model.Usuario;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AutenticacaoService {
    @org.springframework.beans.factory.annotation.Value("${app.acesso.cadastroPublico:true}")
    private boolean cadastroPublico;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificadorSenha;
    private final JwtEncoder codificadorJwt;
    private final PropriedadesJwt propriedadesJwt;
    private final SessaoRenovacaoRepository sessoes;
    private final SecureRandom aleatorio = new SecureRandom();

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder codificadorSenha,
                               JwtEncoder codificadorJwt, PropriedadesJwt propriedadesJwt,
                               SessaoRenovacaoRepository sessoes) {
        this.usuarios = usuarios;
        this.codificadorSenha = codificadorSenha;
        this.codificadorJwt = codificadorJwt;
        this.propriedadesJwt = propriedadesJwt;
        this.sessoes = sessoes;
    }

    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        if (!cadastroPublico) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O cadastro é realizado pelo administrador.");
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        return usuarios.save(new Usuario(nome.trim(), emailNormalizado, codificadorSenha.encode(senha)));
    }

    @Transactional
    public ResultadoAutenticacao autenticar(String email, String senha, boolean manterConectado) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim())
                .filter(candidato -> codificadorSenha.matches(senha, candidato.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        String tokenRenovacao = gerarTokenRenovacao();
        Instant agora = Instant.now();
        sessoes.save(new SessaoRenovacao(usuario, UUID.randomUUID(), hash(tokenRenovacao),
                usuario.getVersaoSessao(), manterConectado, agora.plus(30, ChronoUnit.DAYS)));
        return new ResultadoAutenticacao(emitirTokenAcesso(usuario), tokenRenovacao, manterConectado);
    }

    @Transactional(noRollbackFor = TokenRenovacaoInvalidoException.class)
    public ResultadoAutenticacao renovar(String tokenAtual) {
        if (tokenAtual == null || tokenAtual.isBlank()) throw tokenInvalido();
        var sessao = sessoes.buscarPorTokenHashComBloqueio(hash(tokenAtual)).orElseThrow(this::tokenInvalido);
        Instant agora = Instant.now();
        if (sessao.foiRotacionada()) {
            sessoes.revogarFamilia(sessao.getFamiliaId(), agora, "REUTILIZACAO_DETECTADA");
            throw tokenInvalido();
        }
        if (!sessao.podeRenovar(agora) || sessao.getUsuario().getVersaoSessao() != sessao.getVersaoSessao()) {
            sessoes.revogarFamilia(sessao.getFamiliaId(), agora, "SESSAO_INVALIDA");
            throw tokenInvalido();
        }
        String novoToken = gerarTokenRenovacao();
        var novaSessao = new SessaoRenovacao(sessao.getUsuario(), sessao.getFamiliaId(), hash(novoToken),
                sessao.getVersaoSessao(), sessao.isPersistente(), sessao.getExpiraEm());
        sessoes.save(novaSessao);
        sessao.marcarRotacionada(novaSessao.getId(), agora);
        return new ResultadoAutenticacao(emitirTokenAcesso(sessao.getUsuario()), novoToken, sessao.isPersistente());
    }

    @Transactional
    public void sair(String tokenAtual) {
        if (tokenAtual == null || tokenAtual.isBlank()) return;
        sessoes.buscarPorTokenHashComBloqueio(hash(tokenAtual))
                .ifPresent(sessao -> sessoes.revogarFamilia(sessao.getFamiliaId(), Instant.now(), "LOGOUT"));
    }

    private TokenResponseDTO emitirTokenAcesso(Usuario usuario) {
        Instant emitidoEm = Instant.now();
        Instant expiraEm = emitidoEm.plus(propriedadesJwt.minutosExpiracao(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gestao-servicos-api")
                .subject(usuario.getId().toString())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim("email", usuario.getEmail())
                .claim("versaoSessao", usuario.getVersaoSessao())
                .build();
        String tokenAcesso = codificadorJwt.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponseDTO(tokenAcesso, "Bearer", expiraEm);
    }

    private String gerarTokenRenovacao() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private TokenRenovacaoInvalidoException tokenInvalido() {
        return new TokenRenovacaoInvalidoException();
    }

    public record ResultadoAutenticacao(TokenResponseDTO tokenAcesso, String tokenRenovacao, boolean persistente) {}

    private static final class TokenRenovacaoInvalidoException extends ResponseStatusException {
        private TokenRenovacaoInvalidoException() {
            super(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente.");
        }
    }
}
