package com.cesar.validation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI residenceValidationOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API — Sistema Inteligente de Validação de Comprovantes de Residência")
                .description("Processamento de imagens e PDFs com OpenCV, extração OCR com Tesseract, "
                    + "interpretação semântica com Google Gemini e validação determinística de regras de negócio.")
                .version("1.0.0")
                .contact(new Contact().name("Equipe de Engenharia").email("arquitetura@validacao.local")));
    }
}
