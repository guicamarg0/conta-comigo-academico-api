package br.com.gestaoservicos.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record PropriedadesJwt(String segredo, long minutosExpiracao) {}
