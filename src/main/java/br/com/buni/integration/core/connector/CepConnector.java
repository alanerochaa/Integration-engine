package br.com.buni.integration.core.connector;

import br.com.buni.integration.core.model.response.CepResponse;
import br.com.buni.integration.core.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CepConnector {

    private final RestTemplate restTemplate;

    public CepResponse buscarCep(String cep) {

        String cepLimpo = StringUtils.limparNumero(cep);

        if (cepLimpo.length() != 8) {
            return null;
        }

        String url = "https://viacep.com.br/ws/" + cepLimpo + "/json/";

        try {
            CepResponse response =
                    restTemplate.getForObject(url, CepResponse.class);

            if (response == null || Boolean.TRUE.equals(response.getErro())) {
                return null;
            }

            return response;

        } catch (Exception e) {
            log.warn("Failed to query ViaCEP for CEP {} — address fallback will be applied. Reason: {}",
                    cep, e.getMessage());
            return null;
        }
    }
}