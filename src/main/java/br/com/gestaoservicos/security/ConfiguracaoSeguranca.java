package br.com.gestaoservicos.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(PropriedadesJwt.class)
public class ConfiguracaoSeguranca {
    @Bean
    SecurityFilterChain cadeiaFiltrosSeguranca(
            HttpSecurity http,
            FiltroContextoUnidade filtroContextoUnidade,
            FiltroOrigemSessao filtroOrigemSessao,
            CorsConfigurationSource fonteConfiguracaoCors)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(fonteConfiguracaoCors))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/autenticacao/cadastro", "/api/v1/autenticacao/login",
                                "/api/v1/autenticacao/renovar", "/api/v1/autenticacao/sair",
                                "/api/v1/autenticacao/recuperar-senha", "/api/v1/autenticacao/redefinir-senha",
                                "/api/v1/convites/consultar", "/api/v1/convites/cadastro").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .addFilterBefore(filtroOrigemSessao, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(filtroContextoUnidade, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder codificadorSenha() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean
    JwtEncoder codificadorJwt(PropriedadesJwt propriedades) {
        var chave = new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder decodificadorJwt(PropriedadesJwt propriedades) {
        var chave = new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource fonteConfiguracaoCors(
            @Value("${app.cors.origensPermitidas:http://localhost:3000}") List<String> origensPermitidas) {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origensPermitidas);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", FiltroContextoUnidade.CABECALHO_UNIDADE));
        configuracao.setExposedHeaders(List.of(FiltroContextoUnidade.CABECALHO_UNIDADE));
        configuracao.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
