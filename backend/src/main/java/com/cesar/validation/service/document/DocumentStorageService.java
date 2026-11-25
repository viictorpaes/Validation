package com.cesar.validation.service.document;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.exception.DocumentProcessingException;
import com.cesar.validation.exception.InvalidDocumentFileException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/jpg"
    );

    private final AppProperties appProperties;

    public DocumentStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public StoredFileMetadata validateAndStore(MultipartFile file, UUID documentId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentFileException("Nenhum arquivo foi enviado ou o arquivo está vazio.");
        }

        long maxBytes = appProperties.getStorage().getMaxFileSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new InvalidDocumentFileException(
                "O arquivo excede o tamanho máximo permitido de " + (maxBytes / (1024 * 1024)) + " MB."
            );
        }

        String rawName = file.getOriginalFilename();
        if (rawName == null || rawName.isBlank()) {
            throw new InvalidDocumentFileException("O nome do arquivo é inválido.");
        }

        String sanitizedOriginalName = Paths.get(rawName).getFileName().toString()
            .replaceAll("[\\r\\n\\\\/]+", "_");
        String extension = extractExtension(sanitizedOriginalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidDocumentFileException(
                "Extensão de arquivo não permitida. Utilize apenas PDF, PNG ou JPG/JPEG."
            );
        }

        String contentType = file.getContentType() != null
            ? file.getContentType().toLowerCase(Locale.ROOT).trim()
            : "";
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidDocumentFileException(
                "Tipo MIME inválido (" + contentType + "). Envie apenas PDF, PNG ou JPEG."
            );
        }

        verifyMagicBytes(file, extension);

        try {
            Path baseDir = Paths.get(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            Path targetPath = baseDir.resolve(documentId + extension).normalize();
            if (!targetPath.startsWith(baseDir)) {
                throw new InvalidDocumentFileException("Caminho de armazenamento inválido.");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String normalizedMime = "image/jpg".equals(contentType) ? "image/jpeg" : contentType;
            return new StoredFileMetadata(
                sanitizedOriginalName,
                normalizedMime,
                file.getSize(),
                extension,
                targetPath
            );
        } catch (IOException ex) {
            throw new DocumentProcessingException("Não foi possível armazenar o arquivo para processamento.", ex);
        }
    }

    public void deleteStoredFile(String storedFilePath) {
        if (storedFilePath == null || storedFilePath.isBlank()) {
            return;
        }
        try {
            Path baseDir = Paths.get(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();
            Path filePath = Paths.get(storedFilePath).toAbsolutePath().normalize();
            if (filePath.startsWith(baseDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            log.warn("Falha não crítica ao remover arquivo físico do documento: {}", ex.getClass().getSimpleName());
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void verifyMagicBytes(MultipartFile file, String extension) {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        } catch (IOException ex) {
            throw new InvalidDocumentFileException("Não foi possível ler o cabeçalho do arquivo enviado.");
        }

        if (read < 4) {
            throw new InvalidDocumentFileException("Arquivo corrompido ou com conteúdo insuficiente.");
        }

        if (".pdf".equals(extension)) {
            boolean isPdf = header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            if (!isPdf) {
                throw new InvalidDocumentFileException("O arquivo enviado não possui assinatura válida de PDF.");
            }
        } else if (".png".equals(extension)) {
            boolean isPng = (header[0] & 0xFF) == 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G';
            if (!isPng) {
                throw new InvalidDocumentFileException("O arquivo enviado não possui assinatura válida de imagem PNG.");
            }
        } else if (".jpg".equals(extension) || ".jpeg".equals(extension)) {
            boolean isJpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            if (!isJpeg) {
                throw new InvalidDocumentFileException("O arquivo enviado não possui assinatura válida de imagem JPEG.");
            }
        }
    }

    public record StoredFileMetadata(
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        String extension,
        Path storedPath
    ) {
    }
}
