package com.cesar.validation.service.image;

import com.cesar.validation.exception.DocumentProcessingException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

@Component
public class PdfPageRendererService {

    private static final float RENDER_DPI = 300f;
    private static final int MAX_PAGES_TO_PROCESS = 3;

    public List<BufferedImage> renderPdfPages(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                throw new DocumentProcessingException("O arquivo PDF enviado não contém páginas.");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToRender = Math.min(totalPages, MAX_PAGES_TO_PROCESS);
            List<BufferedImage> images = new ArrayList<>(pagesToRender);

            for (int page = 0; page < pagesToRender; page++) {
                BufferedImage rendered = renderer.renderImageWithDPI(page, RENDER_DPI, ImageType.RGB);
                images.add(rendered);
            }
            return images;
        } catch (IOException ex) {
            throw new DocumentProcessingException("Falha ao converter páginas do PDF em imagem para OCR.", ex);
        }
    }
}
