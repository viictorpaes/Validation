package com.cesar.validation.exception;

import com.cesar.validation.dto.ApiErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidDocumentFileException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidFile(InvalidDocumentFileException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMaxSize(MaxUploadSizeExceededException ex) {
        return buildResponse(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "O arquivo enviado excede o tamanho máximo permitido de 10 MB.",
            List.of()
        );
    }

    @ExceptionHandler({
        MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ApiErrorResponseDto> handleBadRequestParams(Exception ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Parâmetros da requisição inválidos ou ausentes.",
            List.of(ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "Falha de validação nos dados enviados.", details);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiErrorResponseDto> handleProcessing(DocumentProcessingException ex) {
        log.error("Falha controlada durante processamento documental: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnexpected(Exception ex) {
        log.error("Erro interno inesperado na API (detalhes sensíveis omitidos): {}", ex.getClass().getSimpleName());
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro interno ao processar a solicitação.",
            List.of()
        );
    }

    private ResponseEntity<ApiErrorResponseDto> buildResponse(
        HttpStatus status,
        String message,
        List<String> details
    ) {
        ApiErrorResponseDto body = new ApiErrorResponseDto(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            details
        );
        return ResponseEntity.status(status).body(body);
    }
}
