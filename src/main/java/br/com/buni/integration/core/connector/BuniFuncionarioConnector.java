package br.com.buni.integration.core.connector;

import br.com.buni.integration.core.model.request.FuncionarioRequest;
import br.com.buni.integration.core.model.response.CadastroFuncionarioResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuniFuncionarioConnector {

    private final RestTemplate restTemplate;
    private final BuniAuthConnector authConnector;
    private final ObjectMapper objectMapper;

    @Value("${buni.funcionario.cadastro.url}")
    private String cadastroFuncionarioUrl;

    public CadastroFuncionarioResponse cadastrarFuncionario(FuncionarioRequest request) {

        String token = authConnector.gerarTokenFuncionario();

        FuncionarioRequest.DadosPessoais dp     = request.getDadosPessoais();
        FuncionarioRequest.DadosEmpresa empresa  = request.getDadosEmpresa();
        FuncionarioRequest.DadosBancarios banco  = request.getDadosBancarios();

        try {
            log.info("==========================================================");
            log.info("CADASTRO DE FUNCIONÁRIO");
            log.info("CPF............: {}", dp.getCpf());
            log.info("Nome...........: {}", dp.getNomeFuncionario());
            log.info("Matrícula......: {}", empresa.getMatricula());
            log.info("Situação.......: {}", empresa.getSituacao());
            log.info("DataAdmissao...: {}", empresa.getDataAdmissao());
            log.info("DataNascimento.: {}", dp.getDataNascimento());
            log.info("Banco..........: {}", banco.getCodBanco());
            log.info("Agencia........: {}", banco.getNrAgencia());
            log.info("Conta..........: {}", banco.getNrConta());
            log.info("TipoConta......: {}", banco.getTipodeConta());
            try {
                log.info("JSON PAYLOAD...: {}", objectMapper.writeValueAsString(request));
            } catch (Exception ex) {
                log.warn("Nao foi possivel serializar o payload para log: {}", ex.getMessage());
            }
            log.info("==========================================================");

            HttpEntity<FuncionarioRequest> entity =
                    new HttpEntity<>(request, criarHeaders(token));

            ResponseEntity<CadastroFuncionarioResponse> response =
                    restTemplate.exchange(
                            cadastroFuncionarioUrl,
                            HttpMethod.POST,
                            entity,
                            CadastroFuncionarioResponse.class
                    );

            CadastroFuncionarioResponse body = response.getBody();

            if (body == null) {
                throw new RuntimeException(
                        "API retornou resposta vazia no cadastro do funcionário."
                );
            }

            log.info("Funcionário cadastrado com sucesso. CPF: {}", dp.getCpf());
            return body;

        } catch (HttpStatusCodeException ex) {

            log.error("==========================================================");
            log.error("ERRO AO CADASTRAR FUNCIONÁRIO");
            log.error("CPF............: {}", dp.getCpf());
            log.error("Nome...........: {}", dp.getNomeFuncionario());
            log.error("Matrícula......: {}", empresa.getMatricula());
            log.error("HTTP...........: {}", ex.getStatusCode().value());
            log.error("Resposta API...: {}", ex.getResponseBodyAsString());
            log.error("==========================================================");

            throw new RuntimeException(
                    String.format(
                            "[HTTP %d] %s",
                            ex.getStatusCode().value(),
                            ex.getResponseBodyAsString()
                    ),
                    ex
            );
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
