package com.cesar.validation.service.ocr;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.exception.DocumentProcessingException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TesseractOcrService implements OcrExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    private static final List<String> CANDIDATE_DATAPATHS = List.of(
        "/usr/share/tessdata",
        "/usr/share/tesseract-ocr/5/tessdata",
        "/usr/share/tesseract-ocr/4.00/tessdata"
    );

    private final AppProperties appProperties;

    public TesseractOcrService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String extractText(List<BufferedImage> preprocessedPages) {
        if (preprocessedPages == null || preprocessedPages.isEmpty()) {
            return "";
        }

        String datapath = resolveTessdataPath();
        String language = resolveAvailableLanguage(datapath);

        StringBuilder fullText = new StringBuilder();
        for (int i = 0; i < preprocessedPages.size(); i++) {
            BufferedImage pageImage = preprocessedPages.get(i);
            String pageText = extractSinglePage(pageImage, datapath, language);
            if (pageText != null && !pageText.isBlank()) {
                if (!fullText.isEmpty()) {
                    fullText.append("\n\n--- PÁGINA ").append(i + 1).append(" ---\n\n");
                }
                fullText.append(pageText.trim());
            }
        }
        return fullText.toString().trim();
    }

    private synchronized String extractSinglePage(BufferedImage image, String datapath, String language) {
        try {
            ITesseract tesseract = new Tesseract();
            if (datapath != null) {
                tesseract.setDatapath(datapath);
            }
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(6);
            tesseract.setOcrEngineMode(1);
            return tesseract.doOCR(image);
        } catch (TesseractException | UnsatisfiedLinkError | NoClassDefFoundError ex) {
            log.warn("Tess4J JNA indisponível ({}); tentando binário CLI do Tesseract.", ex.getClass().getSimpleName());
            return extractViaTesseractCli(image, language);
        }
    }

    private String extractViaTesseractCli(BufferedImage image, String language) {
        Path tempImg = null;
        try {
            tempImg = Files.createTempFile("ocr-page-", ".png");
            ImageIO.write(image, "png", tempImg.toFile());

            ProcessBuilder pb = new ProcessBuilder(
                "tesseract",
                tempImg.toAbsolutePath().toString(),
                "stdout",
                "-l",
                language,
                "--psm",
                "6"
            );
            Process process = pb.start();
            byte[] stdout = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new DocumentProcessingException("Tempo limite excedido na extração OCR.");
            }
            if (process.exitValue() != 0) {
                throw new DocumentProcessingException("Falha na execução do motor Tesseract OCR.");
            }
            return new String(stdout, StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DocumentProcessingException(
                "Motor Tesseract OCR não disponível para processar o documento.",
                ex
            );
        } finally {
            if (tempImg != null) {
                try {
                    Files.deleteIfExists(tempImg);
                } catch (IOException ignored) {
                    // ignore cleanup failure
                }
            }
        }
    }

    private String resolveTessdataPath() {
        String configured = appProperties.getOcr().getDatapath();
        if (configured != null && !configured.isBlank() && Files.isDirectory(Paths.get(configured))) {
            return configured;
        }
        String envPrefix = System.getenv("TESSDATA_PREFIX");
        if (envPrefix != null && !envPrefix.isBlank() && Files.isDirectory(Paths.get(envPrefix))) {
            return envPrefix;
        }
        for (String candidate : CANDIDATE_DATAPATHS) {
            if (Files.isDirectory(Paths.get(candidate))) {
                return candidate;
            }
        }
        return configured;
    }

    private String resolveAvailableLanguage(String datapath) {
        String configuredLang = appProperties.getOcr().getLanguage();
        if (datapath == null || !Files.isDirectory(Paths.get(datapath))) {
            return configuredLang != null && !configuredLang.isBlank() ? configuredLang : "por";
        }
        Path dir = Paths.get(datapath);
        boolean hasPor = Files.exists(dir.resolve("por.traineddata"));
        boolean hasEng = Files.exists(dir.resolve("eng.traineddata"));
        if (hasPor && hasEng) {
            return "por+eng";
        }
        if (hasPor) {
            return "por";
        }
        if (hasEng) {
            return "eng";
        }
        return configuredLang;
    }
}
