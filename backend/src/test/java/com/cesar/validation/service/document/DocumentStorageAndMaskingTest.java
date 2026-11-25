package com.cesar.validation.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.exception.InvalidDocumentFileException;
import com.cesar.validation.util.PiiMaskingUtil;
import com.cesar.validation.util.TextNormalizationUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class DocumentStorageAndMaskingTest {

    @TempDir
    Path tempDir;

    private DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getStorage().setUploadDir(tempDir.toString());
        props.getStorage().setMaxFileSizeBytes(1024 * 1024); // 1 MB para teste
        storageService = new DocumentStorageService(props);
    }

    @Test
    @DisplayName("Deve validar magic bytes PNG, armazenar e excluir arquivo físico com segurança")
    void shouldStoreAndDeleteValidPng() throws Exception {
        BufferedImage img = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 80, 40);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        MockMultipartFile file = new MockMultipartFile(
            "file", "conta_agua.png", "image/png", baos.toByteArray()
        );

        UUID id = UUID.randomUUID();
        DocumentStorageService.StoredFileMetadata stored = storageService.validateAndStore(file, id);

        assertNotNull(stored);
        assertTrue(Files.exists(stored.storedPath()));

        storageService.deleteStoredFile(stored.storedPath().toString());
        assertFalse(Files.exists(stored.storedPath()));
    }

    @Test
    @DisplayName("Deve rejeitar arquivo com extensão não permitida ou assinatura falsa")
    void shouldRejectInvalidExtensionOrSpoofedPdf() {
        MockMultipartFile exeFile = new MockMultipartFile(
            "file", "malicioso.exe", "application/octet-stream", new byte[] {1, 2, 3, 4, 5}
        );
        assertThrows(InvalidDocumentFileException.class,
            () -> storageService.validateAndStore(exeFile, UUID.randomUUID()));

        MockMultipartFile fakePdf = new MockMultipartFile(
            "file", "falso.pdf", "application/pdf", "NAO_EH_PDF_REAL".getBytes()
        );
        assertThrows(InvalidDocumentFileException.class,
            () -> storageService.validateAndStore(fakePdf, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Deve mascarar CPF corretamente para conformidade com LGPD")
    void shouldMaskPiiInCpfAndOcrPreview() {
        assertEquals("529.***.***-25", PiiMaskingUtil.maskCpf("529.982.247-25"));
        String preview = PiiMaskingUtil.sanitizeOcrPreview(
            "Titular Teste CPF 529.982.247-25 Rua Projetada 10", 200
        );
        assertFalse(preview.contains("982.247"));
        assertTrue(preview.contains("529.***.***-25"));
        assertTrue(TextNormalizationUtil.isValidCpf("52998224725"));
        assertFalse(TextNormalizationUtil.isValidCpf("11111111111"));
    }
}
