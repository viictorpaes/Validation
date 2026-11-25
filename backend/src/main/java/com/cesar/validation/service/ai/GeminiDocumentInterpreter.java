package com.cesar.validation.service.ai;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.util.TextNormalizationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiDocumentInterpreter implements DocumentAiInterpreter {

    private static final Logger log = LoggerFactory.getLogger(GeminiDocumentInterpreter.class);
    private static final String API_REVISION = "2026-05-20";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HeuristicDocumentExtractor heuristicExtractor;
    private final HttpClient httpClient;

    @Autowired
    public GeminiDocumentInterpreter(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        HeuristicDocumentExtractor heuristicExtractor
    ) {
        this(
            appProperties,
            objectMapper,
            heuristicExtractor,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()
        );
    }

    public GeminiDocumentInterpreter(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        HeuristicDocumentExtractor heuristicExtractor,
        HttpClient httpClient
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.heuristicExtractor = heuristicExtractor;
        this.httpClient = httpClient;
    }

    @Override
    public AiInterpretationResult interpret(String ocrText, DocumentType informedDocumentType) {
        if (ocrText == null || ocrText.isBlank()) {
            ExtractedDocumentDto empty = new ExtractedDocumentDto(
                null, null, null, null, null, null, null, null,
                List.of("nomeTitular", "cpf", "endereco", "cidade", "estado", "dataEmissao", "tipoDocumento")
            );
            return new AiInterpretationResult(empty, "OCR_EMPTY", false);
        }

        String apiKey = appProperties.getAi().getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Variável GEMINI_API_KEY não configurada; acionando extrator determinístico de contingência.");
            return fallbackOrEmpty(ocrText, "HEURISTIC_NO_API_KEY");
        }

        try {
            String requestBody = buildInteractionsRequestPayload(ocrText, informedDocumentType);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appProperties.getAi().getGemini().getApiUrl()))
                .timeout(Duration.ofSeconds(appProperties.getAi().getGemini().getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .header("Api-Revision", API_REVISION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("API Google Gemini retornou status HTTP {}", response.statusCode());
                return fallbackOrEmpty(ocrText, "HEURISTIC_GEMINI_ERROR");
            }

            String jsonContent = extractModelJsonOutput(response.body());
            ExtractedDocumentDto structured = parseAndSanitizeStructuredJson(jsonContent);
            return new AiInterpretationResult(structured, "GEMINI_AI", true);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Falha ao interpretar texto via Google Gemini ({}); aplicando tratamento resiliente.",
                ex.getClass().getSimpleName());
            return fallbackOrEmpty(ocrText, "HEURISTIC_GEMINI_ERROR");
        }
    }

    private AiInterpretationResult fallbackOrEmpty(String ocrText, String fallbackSource) {
        if (appProperties.getAi().getGemini().isFallbackHeuristicEnabled()) {
            ExtractedDocumentDto extracted = heuristicExtractor.extractFromText(ocrText);
            return new AiInterpretationResult(extracted, fallbackSource, false);
        }
        ExtractedDocumentDto empty = new ExtractedDocumentDto(
            null, null, null, null, null, null, null, null,
            List.of("nomeTitular", "cpf", "endereco", "cidade", "estado", "dataEmissao", "tipoDocumento")
        );
        return new AiInterpretationResult(empty, "GEMINI_UNAVAILABLE", false);
    }

    private String buildInteractionsRequestPayload(String ocrText, DocumentType informedType) throws IOException {
        String systemInstruction = """
            Você é um extrator especializado em comprovantes de residência brasileiros.
            Sua única responsabilidade é extrair os campos solicitados a partir do texto OCR fornecido e retornar exclusivamente um JSON compatível com o schema definido.
            REGRAS ESTRITAS:
            1. NUNCA invente, complete ou deduza informações ausentes no texto.
            2. Qualquer campo não encontrado explicitamente no texto deve receber o valor null.
            3. Converta dataEmissao para o formato ISO YYYY-MM-DD caso encontrada; se não encontrada, retorne null.
            4. No campo cpf, retorne apenas os 11 dígitos numéricos se encontrado, ou null se ausente.
            5. Classifique tipoDocumento apenas entre: CONTA_DE_AGUA, CONTA_DE_ENERGIA, CONTA_DE_TELEFONE, FATURA_CARTAO, CONTRATO_ALUGUEL, OUTROS, ou null se impossível identificar.
            6. NUNCA emita decisão de aprovação ou reprovação.
            """;

        String prompt = "Extraia os dados estruturados do comprovante abaixo (categoria informada no envio: "
            + (informedType != null ? informedType.name() : "NAO_INFORMADO")
            + "). Retorne null para campos não encontrados.\n\nTEXTO OCR:\n"
            + ocrText;

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("nomeTitular", nullableStringSchema("Nome completo do titular do documento."));
        properties.put("cpf", nullableStringSchema("CPF do titular contendo apenas dígitos, ou null."));
        properties.put("endereco", nullableStringSchema("Logradouro, número e complemento da residência."));
        properties.put("cidade", nullableStringSchema("Nome do município/cidade."));
        properties.put("estado", nullableStringSchema("Sigla da UF com 2 letras (ex: PE, SP, BA)."));
        properties.put("cep", nullableStringSchema("Código postal CEP se presente."));
        properties.put("dataEmissao", nullableStringSchema("Data de emissão do comprovante no formato YYYY-MM-DD."));
        properties.put("tipoDocumento", nullableStringSchema(
            "Tipo identificado: CONTA_DE_AGUA, CONTA_DE_ENERGIA, CONTA_DE_TELEFONE, FATURA_CARTAO, CONTRATO_ALUGUEL ou OUTROS."
        ));

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("type", "object");
        jsonSchema.put("properties", properties);
        jsonSchema.put("required", List.of(
            "nomeTitular", "cpf", "endereco", "cidade", "estado", "cep", "dataEmissao", "tipoDocumento"
        ));

        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "text");
        responseFormat.put("mime_type", "application/json");
        responseFormat.put("schema", jsonSchema);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", appProperties.getAi().getGemini().getModel());
        payload.put("system_instruction", systemInstruction.trim());
        payload.put("input", prompt);
        payload.put("store", false);
        payload.put("response_format", responseFormat);

        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> nullableStringSchema(String description) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("anyOf", List.of(
            Map.of("type", "string"),
            Map.of("type", "null")
        ));
        prop.put("description", description);
        return prop;
    }

    String extractModelJsonOutput(String rawApiResponse) throws IOException {
        JsonNode root = objectMapper.readTree(rawApiResponse);

        JsonNode outputTextNode = root.get("output_text");
        if (outputTextNode != null && outputTextNode.isTextual() && !outputTextNode.asText().isBlank()) {
            return stripMarkdownFences(outputTextNode.asText());
        }

        JsonNode stepsNode = root.get("steps");
        if (stepsNode != null && stepsNode.isArray()) {
            StringBuilder combined = new StringBuilder();
            for (JsonNode step : stepsNode) {
                if ("model_output".equals(step.path("type").asText())) {
                    JsonNode contentArr = step.get("content");
                    if (contentArr != null && contentArr.isArray()) {
                        for (JsonNode part : contentArr) {
                            if ("text".equals(part.path("type").asText()) && part.hasNonNull("text")) {
                                combined.append(part.get("text").asText());
                            }
                        }
                    }
                }
            }
            if (!combined.isEmpty()) {
                return stripMarkdownFences(combined.toString());
            }
        }

        throw new IllegalStateException("Resposta da API Gemini não contém bloco model_output válido.");
    }

    ExtractedDocumentDto parseAndSanitizeStructuredJson(String jsonText) throws IOException {
        JsonNode node = objectMapper.readTree(jsonText);
        if (node == null || !node.isObject()) {
            throw new IllegalStateException("Conteúdo retornado pela IA não é um objeto JSON válido.");
        }

        String nome = normalizeNullableField(node.get("nomeTitular"));
        String rawCpf = normalizeNullableField(node.get("cpf"));
        String cpf = rawCpf != null ? rawCpf.replaceAll("\\D", "") : null;
        if (cpf != null && cpf.isEmpty()) {
            cpf = null;
        }

        String endereco = normalizeNullableField(node.get("endereco"));
        String cidade = normalizeNullableField(node.get("cidade"));
        String estado = normalizeNullableField(node.get("estado"));
        if (estado != null) {
            estado = estado.toUpperCase().trim();
        }
        String cep = normalizeNullableField(node.get("cep"));

        String rawDate = normalizeNullableField(node.get("dataEmissao"));
        String isoDate = null;
        if (rawDate != null) {
            Optional<LocalDate> parsed = TextNormalizationUtil.parseFlexibleDate(rawDate);
            isoDate = parsed.map(LocalDate::toString).orElse(rawDate);
        }

        String tipoDocumento = normalizeNullableField(node.get("tipoDocumento"));

        List<String> missing = HeuristicDocumentExtractor.computeMissingFields(
            nome, cpf, endereco, cidade, estado, isoDate, tipoDocumento
        );

        return new ExtractedDocumentDto(
            nome,
            cpf,
            endereco,
            cidade,
            estado,
            cep,
            isoDate,
            tipoDocumento,
            missing
        );
    }

    private String normalizeNullableField(JsonNode field) {
        if (field == null || field.isNull()) {
            return null;
        }
        String val = field.asText().trim();
        if (val.isEmpty() || "null".equalsIgnoreCase(val) || "n/a".equalsIgnoreCase(val)) {
            return null;
        }
        return val;
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
