package com.cesar.validation.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.service.ai.DocumentAiInterpreter;
import com.cesar.validation.service.ocr.OcrExtractionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentAndDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OcrExtractionService ocrExtractionService;

    @MockBean
    private DocumentAiInterpreter documentAiInterpreter;

    @Test
    @DisplayName("Deve executar fluxo REST completo: upload, persistência, consulta, dashboard e exclusão")
    void shouldExecuteFullDocumentLifecycleAndHandleErrors() throws Exception {
        when(ocrExtractionService.extractText(any()))
            .thenReturn("CONTA DE ENERGIA TITULAR LUCAS MENDES SINTETICO RUA ALFA 200 RECIFE PE");

        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Lucas Mendes Sintetico",
            "52998224725",
            "Rua Alfa, 200",
            "Recife",
            "PE",
            "50000000",
            LocalDate.now().minusDays(12).toString(),
            "CONTA_DE_ENERGIA",
            List.of()
        );
        when(documentAiInterpreter.interpret(any(), any(DocumentType.class)))
            .thenReturn(new DocumentAiInterpreter.AiInterpretationResult(extracted, "GEMINI_AI", true));

        MockMultipartFile validPng = new MockMultipartFile(
            "file",
            "fatura_energia.png",
            "image/png",
            createSyntheticPngBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents")
                .file(validPng)
                .param("documentType", "CONTA_DE_ENERGIA")
                .param("holderName", "Lucas Mendes Sintetico")
                .param("cpf", "529.982.247-25"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.validationStatus", is("APROVADO")))
            .andExpect(jsonPath("$.processingStatus", is("CONCLUIDO")))
            .andExpect(jsonPath("$.extractedData.cpf", is("529.***.***-25")))
            .andExpect(jsonPath("$.appliedRules", hasSize(5)))
            .andReturn();

        JsonNode createdJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String docId = createdJson.get("id").asText();

        mockMvc.perform(get("/api/documents/{id}/result", docId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(docId)))
            .andExpect(jsonPath("$.validationStatus", is("APROVADO")));

        mockMvc.perform(get("/api/documents").param("status", "APROVADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(docId)));

        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvedDocuments").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/api/documents/{id}", docId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/{id}", docId))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/documents/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound());

        MockMultipartFile invalidTxt = new MockMultipartFile(
            "file", "invalido.txt", "text/plain", "conteudo".getBytes()
        );
        mockMvc.perform(multipart("/api/documents")
                .file(invalidTxt)
                .param("documentType", "CONTA_DE_AGUA")
                .param("holderName", "Lucas Mendes Sintetico"))
            .andExpect(status().isBadRequest());
    }

    private byte[] createSyntheticPngBytes() throws Exception {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 100);
        g.setColor(Color.BLACK);
        g.drawString("TESTE", 20, 50);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
