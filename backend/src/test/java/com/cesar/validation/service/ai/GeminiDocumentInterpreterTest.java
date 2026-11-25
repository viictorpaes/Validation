package com.cesar.validation.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.enums.DocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeminiDocumentInterpreterTest {

    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private HeuristicDocumentExtractor heuristicExtractor;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getAi().getGemini().setApiKey("fake-test-api-key");
        appProperties.getAi().getGemini().setModel("gemini-4-hydrogen");
        appProperties.getAi().getGemini().setApiUrl("https://generativelanguage.googleapis.com/v1/interactions");
        appProperties.getAi().getGemini().setFallbackHeuristicEnabled(true);

        objectMapper = new ObjectMapper();
        heuristicExtractor = new HeuristicDocumentExtractor();
        mockHttpClient = mock(HttpClient.class);
    }

    @Test
    @DisplayName("Deve interpretar resposta estruturada JSON da Interactions API do Google Gemini")
    @SuppressWarnings("unchecked")
    void shouldParseStructuredResponseFromGeminiInteractionsApi() throws Exception {
        String apiResponse = """
            {
              "id": "inter_test_001",
              "status": "completed",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "{\\"nomeTitular\\":\\"João Teste Sintético\\",\\"cpf\\":\\"529.982.247-25\\",\\"endereco\\":\\"Rua das Acácias, 42\\",\\"cidade\\":\\"Recife\\",\\"estado\\":\\"PE\\",\\"cep\\":\\"50000-000\\",\\"dataEmissao\\":\\"2026-11-10\\",\\"tipoDocumento\\":\\"CONTA_DE_AGUA\\"}"
                    }
                  ]
                }
              ]
            }
            """;

        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(apiResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        GeminiDocumentInterpreter interpreter = new GeminiDocumentInterpreter(
            appProperties, objectMapper, heuristicExtractor, mockHttpClient
        );

        DocumentAiInterpreter.AiInterpretationResult result = interpreter.interpret(
            "CONTA DE AGUA TEXTO EXEMPLO",
            DocumentType.CONTA_DE_AGUA
        );

        assertTrue(result.aiSucceeded());
        assertEquals("GEMINI_AI", result.source());
        assertEquals("João Teste Sintético", result.data().nomeTitular());
        assertEquals("52998224725", result.data().cpf());
        assertEquals("Rua das Acácias, 42", result.data().endereco());
        assertEquals("Recife", result.data().cidade());
        assertEquals("PE", result.data().estado());
        assertEquals("2026-11-10", result.data().dataEmissao());
        assertTrue(result.data().camposAusentes().isEmpty());
    }

    @Test
    @DisplayName("Deve acionar contingência heurística caso a API do Gemini retorne erro HTTP 500")
    @SuppressWarnings("unchecked")
    void shouldFallbackGracefullyOnGeminiHttpError() throws Exception {
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("{\"error\":\"Internal Error\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        GeminiDocumentInterpreter interpreter = new GeminiDocumentInterpreter(
            appProperties, objectMapper, heuristicExtractor, mockHttpClient
        );

        String sampleOcr = """
            COMPESA SANEAMENTO
            Nome: Ana Paula Ficticia
            CPF: 529.982.247-25
            Endereço: Rua Exemplo Norte, 300
            Recife - PE
            Data de emissão: 15/11/2026
            """;

        DocumentAiInterpreter.AiInterpretationResult result = interpreter.interpret(
            sampleOcr,
            DocumentType.CONTA_DE_AGUA
        );

        assertFalse(result.aiSucceeded());
        assertEquals("HEURISTIC_GEMINI_ERROR", result.source());
        assertEquals("Ana Paula Ficticia", result.data().nomeTitular());
        assertEquals("52998224725", result.data().cpf());
        assertEquals("Rua Exemplo Norte, 300", result.data().endereco());
        assertEquals("Recife", result.data().cidade());
        assertEquals("PE", result.data().estado());
        assertEquals("2026-11-15", result.data().dataEmissao());
    }

    @Test
    @DisplayName("Deve retornar campos nulos sem inventar dados quando API key está ausente e texto não tem campos")
    void shouldReturnNullFieldsWithoutInventingInformation() {
        appProperties.getAi().getGemini().setApiKey("");
        GeminiDocumentInterpreter interpreter = new GeminiDocumentInterpreter(
            appProperties, objectMapper, heuristicExtractor, mockHttpClient
        );

        DocumentAiInterpreter.AiInterpretationResult result = interpreter.interpret(
            "Texto genérico sem nenhum dado cadastral identificável",
            DocumentType.OUTROS
        );

        assertFalse(result.aiSucceeded());
        assertNull(result.data().nomeTitular());
        assertNull(result.data().cpf());
        assertNull(result.data().endereco());
        assertNull(result.data().dataEmissao());
        assertEquals(7, result.data().camposAusentes().size());
    }
}
