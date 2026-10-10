package br.com.gestaoservicos.autenticacao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.time.Duration;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

@Service
public class EmailRecuperacaoService {
    private final String chave, remetente, urlFrontend;
    private final RestClient cliente;
    public EmailRecuperacaoService(@Value("${app.email.chaveResend:${RESEND_API_KEY:}}") String chave,
        @Value("${app.email.remetente:${EMAIL_REMETENTE:}}") String remetente,
        @Value("${app.email.urlFrontend:${FRONTEND_URL:http://localhost:3000}}") String urlFrontend) {
        this.chave = chave; this.remetente = remetente; this.urlFrontend = urlFrontend.replaceAll("/$", "");
        var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().baseUrl("https://api.resend.com").requestFactory(fabrica).build();
    }
    public void validarConfiguracao() {
        if (chave.isBlank() || remetente.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "O envio de e-mails ainda não foi configurado. Contate o administrador.");
    }
    public void enviar(String destinatario, String token) {
        validarConfiguracao();
        try {
            cliente.post().uri("/emails").header("Authorization", "Bearer " + chave)
                .body(Map.of("from", remetente, "to", List.of(destinatario), "subject", "Redefinir sua senha — Conta Comigo",
                    "text", "Use o link para definir uma nova senha: " + urlFrontend + "/redefinir-senha#token=" + token
                        + "\nO link é válido por 30 minutos e só pode ser utilizado uma vez. Se não solicitou, ignore este e-mail."))
                .retrieve().toBodilessEntity();
        } catch (org.springframework.web.client.RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível enviar o e-mail. Tente novamente mais tarde.");
        }
    }
}
