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

    // ─── Clientes ────────────────────────────────────────────────────────────────

    @Value("${buni.cliente.auth.url}")
    private String clienteAuthUrl;

    @Value("${buni.cliente.auth.client-id}")
    private String clienteClientId;

    @Value("${buni.cliente.auth.client-secret}")
    private String clienteClientSecret;

    @Value("${buni.cliente.auth.grant-type}")
    private String clienteGrantType;

    @Value("${buni.cliente.auth.username}")
    private String clienteUsername;

    @Value("${buni.cliente.auth.password}")
    private String clientePassword;

    // ─── Funcionários ────────────────────────────────────────────────────────────

    @Value("${buni.funcionario.auth.url}")
    private String funcionarioAuthUrl;

    @Value("${buni.funcionario.auth.client-id}")
    private String funcionarioClientId;

    @Value("${buni.funcionario.auth.client-secret}")
    private String funcionarioClientSecret;

    @Value("${buni.funcionario.auth.grant-type}")
    private String funcionarioGrantType;

    @Value("${buni.funcionario.auth.username}")
    private String funcionarioUsername;

    @Value("${buni.funcionario.auth.password}")
    private String funcionarioPassword;

    // ─── Cache por módulo ────────────────────────────────────────────────────────

    private volatile String cachedTokenCliente;
    private volatile LocalDateTime tokenClienteExpiresAt;

    private volatile String cachedTokenFuncionario;
    private volatile LocalDateTime tokenFuncionarioExpiresAt;

    // ─── API pública ─────────────────────────────────────────────────────────────

    /**
     * Token OAuth para o módulo de Clientes.
     * Sincronizado para evitar múltiplas renovações simultâneas.
     */
    public synchronized String gerarToken() {
        if (isTokenValido(cachedTokenCliente, tokenClienteExpiresAt)) {
            log.debug("Reutilizando token OAuth do módulo Clientes.");
            return cachedTokenCliente;
        }
        TokenResponse resp = buscarToken(
                clienteAuthUrl, clienteClientId, clienteClientSecret,
                clienteGrantType, clienteUsername, clientePassword,
                "Clientes"
        );
        long expiresIn = resp.getExpiresIn() != null ? resp.getExpiresIn() : 300L;
        cachedTokenCliente     = resp.resolverToken();
        tokenClienteExpiresAt  = LocalDateTime.now().plusSeconds(expiresIn - 60);
        log.info("Novo token OAuth (Clientes) gerado. Válido até: {}", tokenClienteExpiresAt);
        return cachedTokenCliente;
    }

    /**
     * Token OAuth para o módulo de Funcionários.
     * Sincronizado para evitar múltiplas renovações simultâneas.
     */
    public synchronized String gerarTokenFuncionario() {
        if (isTokenValido(cachedTokenFuncionario, tokenFuncionarioExpiresAt)) {
            log.debug("Reutilizando token OAuth do módulo Funcionários.");
            return cachedTokenFuncionario;
        }
        TokenResponse resp = buscarToken(
                funcionarioAuthUrl, funcionarioClientId, funcionarioClientSecret,
                funcionarioGrantType, funcionarioUsername, funcionarioPassword,
                "Funcionários"
        );
        long expiresIn = resp.getExpiresIn() != null ? resp.getExpiresIn() : 300L;
        cachedTokenFuncionario    = resp.resolverToken();
        tokenFuncionarioExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);
        log.info("Novo token OAuth (Funcionários) gerado. Válido até: {}", tokenFuncionarioExpiresAt);
        return cachedTokenFuncionario;
    }

    // ─── Lógica compartilhada de obtenção de token ───────────────────────────────

    private TokenResponse buscarToken(String url, String clientId, String clientSecret,
                                      String grantType, String username, String password,
                                      String modulo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id",     clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type",    grantType);
            body.add("username",      username);
            body.add("password",      password);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, TokenResponse.class
            );

            if (response.getBody() == null || response.getBody().resolverToken() == null) {
                throw new RuntimeException("Token não retornado pela API [" + modulo + "].");
            }

            return response.getBody();

        } catch (HttpStatusCodeException e) {
            log.error("Erro de autenticação [{}]: {}", modulo, e.getResponseBodyAsString());
            throw new RuntimeException(
                    "Falha ao gerar token [" + modulo + "]: " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao autenticar [" + modulo + "]: " + e.getMessage(), e
            );
        }
    }

    private boolean isTokenValido(String token, LocalDateTime expiresAt) {
        return token != null
                && expiresAt != null
                && LocalDateTime.now().isBefore(expiresAt);
    }
}
