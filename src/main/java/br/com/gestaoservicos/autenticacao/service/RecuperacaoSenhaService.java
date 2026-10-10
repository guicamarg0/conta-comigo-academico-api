package br.com.gestaoservicos.autenticacao.service;

import br.com.gestaoservicos.autenticacao.model.RecuperacaoSenha;
import br.com.gestaoservicos.autenticacao.repository.RecuperacaoSenhaRepository;
import br.com.gestaoservicos.usuario.repository.UsuarioRepository;
import br.com.gestaoservicos.associacao.repository.AssociacaoRepository;
import br.com.gestaoservicos.autenticacao.repository.SessaoRenovacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RecuperacaoSenhaService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entidades;
    private final UsuarioRepository usuarios;
    private final RecuperacaoSenhaRepository recuperacoes;
    private final AssociacaoRepository associacoes;
    private final PasswordEncoder senhas;
    private final EmailRecuperacaoService emails;
    private final SessaoRenovacaoRepository sessoes;
    public RecuperacaoSenhaService(UsuarioRepository usuarios, RecuperacaoSenhaRepository recuperacoes,
        AssociacaoRepository associacoes, PasswordEncoder senhas, EmailRecuperacaoService emails,
        SessaoRenovacaoRepository sessoes) {
        this.usuarios = usuarios; this.recuperacoes = recuperacoes; this.associacoes = associacoes;
        this.senhas = senhas; this.emails = emails; this.sessoes = sessoes;
    }
    @Transactional
    public void solicitar(String email) {
        emails.validarConfiguracao();
        usuarios.findByEmailIgnoreCase(email.trim()).ifPresent(u -> emitir(u.getId()));
    }
    @Transactional
    public void solicitarPeloGestor(UUID unidadeId, UUID associacaoId) {
        var associacao = associacoes.findByIdAndUnidadeIdAndAtivaTrue(associacaoId, unidadeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado nesta unidade"));
        emitir(associacao.getUsuario().getId());
    }
    private void emitir(UUID id) {
        var usuario = usuarios.buscarComBloqueio(id).orElseThrow();
        var agora = Instant.now();
        // Mesmo limite nos dois caminhos; o pedido nunca informa se uma conta existe.
        if (recuperacoes.countByUsuarioIdAndCriadoEmAfter(id, agora.minusSeconds(60)) > 0
            || recuperacoes.countByUsuarioIdAndCriadoEmAfter(id, agora.minusSeconds(3600)) >= 5) return;
        byte[] aleatorio = new byte[32]; new SecureRandom().nextBytes(aleatorio);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        recuperacoes.invalidarDoUsuario(id, agora);
        recuperacoes.saveAndFlush(new RecuperacaoSenha(usuario, hash(token), agora));
        emails.enviar(usuario.getEmail(), token);
    }
    @Transactional
    public void redefinir(String token, String senha) {
        String hash = hash(token);
        var pedido = recuperacoes.findByTokenHash(hash).orElseThrow(this::linkInvalido);
        var usuario = usuarios.buscarComBloqueio(pedido.getUsuario().getId()).orElseThrow(this::linkInvalido);
        // Verificação após obter o bloqueio serializa redefinições concorrentes do mesmo usuário.
        entidades.refresh(pedido);
        if (!pedido.valida(Instant.now())) throw linkInvalido();
        usuario.redefinirSenha(senhas.encode(senha));
        var agora = Instant.now();
        sessoes.revogarDoUsuario(usuario.getId(), agora, "SENHA_REDEFINIDA");
        recuperacoes.invalidarDoUsuario(usuario.getId(), agora);
    }
    private ResponseStatusException linkInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado. Solicite outro link.");
    }
    private String hash(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
