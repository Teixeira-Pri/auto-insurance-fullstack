package com.insurance.auto.client;

import com.insurance.auto.dto.viacep.ViaCepResponse;
import com.insurance.auto.exception.ViaCepException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ViaCepClient {

    private static final String VIACEP_URL = "https://viacep.com.br/ws/{cep}/json/";

    private final RestTemplate restTemplate;

    public ViaCepClient() {
        this.restTemplate = new RestTemplate();
    }

    @Cacheable(value = "viacep", key = "#zipCode")
    public ViaCepResponse getAddressByZipCode(String zipCode) {
        log.debug("Consultando ViaCEP para CEP: {}", zipCode);
        try {
            ViaCepResponse response = restTemplate.getForObject(
                    VIACEP_URL, ViaCepResponse.class, zipCode
            );

            if (response == null || Boolean.TRUE.equals(response.getError())) {
                throw new ViaCepException("CEP não encontrado: " + zipCode);
            }

            log.debug("ViaCEP retornou estado: {} para CEP: {}", response.getState(), zipCode);
            return response;

        } catch (RestClientException e) {
            throw new ViaCepException("Falha ao consultar ViaCEP para CEP " + zipCode, e);
        }
    }
}
