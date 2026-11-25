package com.cesar.validation.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageAndPdfPreprocessingTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve pré-processar imagem com OpenCV e converter PDF sintético em imagens prontas para OCR")
    void shouldPreprocessImageAndRenderPdf() throws Exception {
        PdfPageRendererService pdfRenderer = new PdfPageRendererService();
        OpenCvImagePreprocessingService imageService = new OpenCvImagePreprocessingService(pdfRenderer);

        BufferedImage source = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 300);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("COMPROVANTE SINTETICO TESTE", 40, 120);
        g.dispose();

        BufferedImage processed = imageService.preprocessSingleImage(source);
        assertNotNull(processed);
        assertTrue(processed.getWidth() >= 600);

        Path pdfFile = tempDir.resolve("comprovante_sintetico.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.newLineAtOffset(60, 700);
                content.showText("COMPROVANTE DE RESIDENCIA SINTETICO");
                content.endText();
            }
            doc.save(pdfFile.toFile());
        }

        List<BufferedImage> pdfPages = imageService.prepareDocumentImages(pdfFile, "application/pdf");
        assertEquals(1, pdfPages.size());
        assertTrue(pdfPages.get(0).getWidth() > 1000);
    }
}
