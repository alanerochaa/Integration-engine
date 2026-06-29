package br.com.buni.integration.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IntegrationEngineApplication {

	private static final Logger log =
			LoggerFactory.getLogger(
					IntegrationEngineApplication.class
			);

	public static void main(String[] args) {

		log.info("Iniciando Integration Engine...");

		SpringApplication.run(
				IntegrationEngineApplication.class,
				args
		);

		log.info("Integration Engine iniciado com sucesso.");
	}
}