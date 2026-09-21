package br.com.osb.web_scrapper_servidores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WebScrapperServidoresApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebScrapperServidoresApplication.class, args);
	}

}
