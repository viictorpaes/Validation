package com.cesar.validation.service.image;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

public interface ImagePreprocessingService {

    List<BufferedImage> prepareDocumentImages(Path documentPath, String contentType);

    BufferedImage preprocessSingleImage(BufferedImage sourceImage);
}
