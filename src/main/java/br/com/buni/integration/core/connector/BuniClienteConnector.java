package br.com.buni.integration.core.connector;

import br.com.buni.integration.core.enums.TipoConsultaClienteEnum;
import br.com.buni.integration.core.model.request.ClienteRequest;
import br.com.buni.integration.core.model.response.ConsultaClienteCpfResponse;
import br.com.buni.integration.core.model.response.InclusaoClienteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuniClienteConnector {

    private final RestTemplate restTemplate;
    private final BuniAuthConnector authConnector;

    @Value("${buni.cliente.inclusao.url}")
    private String inclusaoClienteUrl;

    @Value("${buni.cliente.consulta-cpf.url}")
    private String consultaClienteCpfUrl;

    public InclusaoClienteResponse incluirCliente(ClienteRequest request) {

        String token = authConnector.gerarToken();
        HttpEntity<ClienteRequest> entity = new HttpEntity<>(request, criarHeaders(token));

        try {

            ResponseEntity<InclusaoClienteResponse> response = restTemplate.exchange(
                    inclusaoClienteUrl, HttpMethod.POST, entity, InclusaoClienteResponse.class
            );

            InclusaoClienteResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("API retornou resposta vazia na inclusao do cliente.");
            }

            return body;

        } catch (HttpStatusCodeException ex) {
            log.error("Erro HTTP ao incluir cliente CPF {}: [{}] {}",
                    request.getCpf(), ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new RuntimeException(
                    String.format("[HTTP %d] %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()), ex
            );
        }
    }

    /**
     * Verifica se já existe um cliente com o CPF informado na base.
     *
     * Regras:
     * - HTTP 404 → não encontrado, segue para inclusão.
     * - Código de API 100000026 → "cliente não encontrado", segue para inclusão.
     * - Qualquer outro erro HTTP → propaga como exceção para o service tratar.
     */
    public boolean clienteExistePorCpf(String cpf) {

        String token = authConnector.gerarToken();
        HttpEntity<Void> entity = new HttpEntity<>(criarHeaders(token));

        String url = consultaClienteCpfUrl
                + "?TipoConsulta=" + TipoConsultaClienteEnum.CLIENTE.getCodigo()
                + "&CPF=" + cpf;

        try {

            log.info("Consultando CPF antes da inclusão: {}", cpf);

            ResponseEntity<ConsultaClienteCpfResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ConsultaClienteCpfResponse.class
            );

            ConsultaClienteCpfResponse body = response.getBody();

            if (body == null) return false;

            boolean exists = body.possuiCliente();
            log.info("CPF {} encontrado na base: {}", cpf, exists);
            return exists;

        } catch (HttpClientErrorException.NotFound ex) {
            log.info("CPF {} não encontrado na base (HTTP 404).", cpf);
            return false;

        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody.contains("100000026")
                    || responseBody.contains("Nao foi encontrado cliente")
                    || responseBody.contains("Não foi encontrado cliente")) {
                log.info("CPF {} não encontrado na base (código API 100000026) — seguindo para inclusão.", cpf);
                return false;
            }
            log.error("Erro ao consultar CPF {}: [{}] {}", cpf, ex.getStatusCode().value(), responseBody);
            throw new RuntimeException(
                    String.format("[HTTP %d] %s", ex.getStatusCode().value(), responseBody), ex
            );

        } catch (Exception ex) {
            log.error("Falha técnica ao consultar CPF {}", cpf, ex);
            throw new RuntimeException("Falha tecnica ao consultar CPF: " + ex.getMessage(), ex);
        }
    }

    private HttpHeaders criarHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
