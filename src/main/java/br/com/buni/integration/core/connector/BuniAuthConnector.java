package br.com.buni.integration.core.connector;

import br.com.buni.integration.core.model.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuniAuthConnector {

    private final RestTemplate restTemplate;

    @Value("${buni.auth.url}")
    private String authUrl;

    @Value("${buni.auth.client-id}")
    private String clientId;

    @Value("${buni.auth.client-secret}")
    private String clientSecret;

    @Value("${buni.auth.grant-type}")
    private String grantType;

    @Value("${buni.auth.username}")
    private String username;

    @Value("${buni.auth.password}")
    private String password;

    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpiresAt;

    /**
     * Retorna um token OAuth válido, reutilizando o cache quando ainda estiver vigente.
     * Sincronizado para evitar que threads concorrentes gerem tokens duplicados sob carga.
     */
    public synchronized String gerarToken() {

        if (isTokenValid()) {
            log.debug("Reutilizando token OAuth em cache.");
            return cachedToken;
        }

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", grantType);
            body.add("username", username);
            body.add("password", password);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    authUrl, HttpMethod.POST, request, TokenResponse.class
            );

            if (response.getBody() == null || response.getBody().resolverToken() == null) {
                throw new RuntimeException("Token nao retornado pela API.");
            }

            TokenResponse tokenResponse = response.getBody();
            long expiresIn = tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 300L;

            cachedToken = tokenResponse.resolverToken();
            // Renova 60 segundos antes do vencimento real para evitar usar token expirado no meio de uma requisição
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);

            log.info("Novo token OAuth gerado. Válido até: {}", tokenExpiresAt);
            return cachedToken;

        } catch (HttpStatusCodeException e) {
            log.error("Authentication error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao gerar token: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao autenticar: " + e.getMessage(), e);
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null
                && tokenExpiresAt != null
                && LocalDateTime.now().isBefore(tokenExpiresAt);
    }
}
