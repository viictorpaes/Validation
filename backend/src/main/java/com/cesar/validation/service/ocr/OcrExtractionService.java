package com.cesar.validation.service.ocr;

import java.awt.image.BufferedImage;
import java.util.List;

public interface OcrExtractionService {

    String extractText(List<BufferedImage> preprocessedPages);
}
