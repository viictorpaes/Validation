package com.cesar.validation.service.image;

import com.cesar.validation.exception.DocumentProcessingException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenCvImagePreprocessingService implements ImagePreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(OpenCvImagePreprocessingService.class);
    private static final boolean OPENCV_LOADED = initOpenCv();

    private final PdfPageRendererService pdfPageRendererService;

    public OpenCvImagePreprocessingService(PdfPageRendererService pdfPageRendererService) {
        this.pdfPageRendererService = pdfPageRendererService;
    }

    private static boolean initOpenCv() {
        try {
            OpenCV.loadLocally();
            log.info("Biblioteca nativa OpenCV inicializada com sucesso.");
            return true;
        } catch (Throwable t) {
            log.warn("OpenCV nativo indisponível neste ambiente ({}); fallback Java2D habilitado.",
                t.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public List<BufferedImage> prepareDocumentImages(Path documentPath, String contentType) {
        List<BufferedImage> rawPages;
        if ("application/pdf".equalsIgnoreCase(contentType)
            || documentPath.toString().toLowerCase().endsWith(".pdf")) {
            rawPages = pdfPageRendererService.renderPdfPages(documentPath);
        } else {
            try {
                BufferedImage img = ImageIO.read(documentPath.toFile());
                if (img == null) {
                    throw new DocumentProcessingException("Não foi possível decodificar a imagem enviada.");
                }
                rawPages = List.of(img);
            } catch (IOException ex) {
                throw new DocumentProcessingException("Erro de leitura ao carregar a imagem enviada.", ex);
            }
        }

        List<BufferedImage> processed = new ArrayList<>(rawPages.size());
        for (BufferedImage page : rawPages) {
            processed.add(preprocessSingleImage(page));
        }
        return processed;
    }

    @Override
    public BufferedImage preprocessSingleImage(BufferedImage sourceImage) {
        if (sourceImage == null) {
            throw new DocumentProcessingException("Imagem de entrada nula para pré-processamento.");
        }
        if (!OPENCV_LOADED) {
            return fallbackJava2dPreprocess(sourceImage);
        }
        return processWithOpenCv(sourceImage);
    }

    private BufferedImage processWithOpenCv(BufferedImage sourceImage) {
        Mat bgrMat = null;
        Mat resizedMat = null;
        Mat grayMat = null;
        Mat denoisedMat = null;
        Mat contrastMat = null;
        Mat deskewedMat = null;
        Mat thresholdMat = null;
        CLAHE clahe = null;

        try {
            bgrMat = bufferedImageToBgrMat(sourceImage);

            // 1. Redimensionamento proporcional caso a imagem tenha baixa resolução (< 1100px)
            if (bgrMat.cols() < 1100) {
                resizedMat = new Mat();
                double scale = 1400.0 / Math.max(1, bgrMat.cols());
                Imgproc.resize(
                    bgrMat,
                    resizedMat,
                    new Size(Math.round(bgrMat.cols() * scale), Math.round(bgrMat.rows() * scale)),
                    0,
                    0,
                    Imgproc.INTER_CUBIC
                );
            } else {
                resizedMat = bgrMat.clone();
            }

            // 2. Conversão para escala de cinza
            grayMat = new Mat();
            Imgproc.cvtColor(resizedMat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // 3. Redução de ruídos preservando bordas tipográficas
            denoisedMat = new Mat();
            Imgproc.GaussianBlur(grayMat, denoisedMat, new Size(3, 3), 0);

            // 4. Ajuste adaptativo de contraste local (CLAHE)
            contrastMat = new Mat();
            clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
            clahe.apply(denoisedMat, contrastMat);

            // 5. Correção automática de inclinação/orientação leve (Deskew)
            deskewedMat = correctSkewIfNeeded(contrastMat);

            // 6. Binarização Otsu para maximizar nitidez no Tesseract OCR
            thresholdMat = new Mat();
            Imgproc.threshold(
                deskewedMat,
                thresholdMat,
                0,
                255,
                Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU
            );

            return grayMatToBufferedImage(thresholdMat);
        } finally {
            releaseQuietly(bgrMat);
            releaseQuietly(resizedMat);
            releaseQuietly(grayMat);
            releaseQuietly(denoisedMat);
            releaseQuietly(contrastMat);
            releaseQuietly(deskewedMat);
            releaseQuietly(thresholdMat);
        }
    }

    private Mat correctSkewIfNeeded(Mat grayInput) {
        Mat inverted = new Mat();
        Mat nonZeroCoords = new Mat();
        MatOfPoint2f points2f = null;
        Mat rotationMatrix = null;
        try {
            Imgproc.threshold(
                grayInput,
                inverted,
                0,
                255,
                Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU
            );
            Core.findNonZero(inverted, nonZeroCoords);
            if (nonZeroCoords.empty() || nonZeroCoords.rows() < 150) {
                return grayInput.clone();
            }

            points2f = new MatOfPoint2f();
            nonZeroCoords.convertTo(points2f, CvType.CV_32F);
            RotatedRect box = Imgproc.minAreaRect(points2f);

            double angle = box.angle;
            if (angle < -45.0) {
                angle = -(90.0 + angle);
            } else if (angle > 45.0) {
                angle = 90.0 - angle;
            } else {
                angle = -angle;
            }

            if (Math.abs(angle) < 0.8 || Math.abs(angle) > 20.0) {
                return grayInput.clone();
            }

            Point center = new Point(grayInput.cols() / 2.0, grayInput.rows() / 2.0);
            rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
            Mat rotated = new Mat();
            Imgproc.warpAffine(
                grayInput,
                rotated,
                rotationMatrix,
                grayInput.size(),
                Imgproc.INTER_CUBIC,
                Core.BORDER_REPLICATE,
                new Scalar(255)
            );
            return rotated;
        } catch (Exception ex) {
            return grayInput.clone();
        } finally {
            releaseQuietly(inverted);
            releaseQuietly(nonZeroCoords);
            releaseQuietly(points2f);
            releaseQuietly(rotationMatrix);
        }
    }

    private Mat bufferedImageToBgrMat(BufferedImage source) {
        BufferedImage bgrImage = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR
        );
        Graphics2D g2d = bgrImage.createGraphics();
        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bgrImage.getWidth(), bgrImage.getHeight());
            g2d.drawImage(source, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        byte[] pixels = ((DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bgrImage.getHeight(), bgrImage.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    private BufferedImage grayMatToBufferedImage(Mat grayMat) {
        int width = grayMat.cols();
        int height = grayMat.rows();
        byte[] buffer = new byte[width * height];
        grayMat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }

    private BufferedImage fallbackJava2dPreprocess(BufferedImage source) {
        int targetWidth = source.getWidth() < 1100 ? source.getWidth() * 2 : source.getWidth();
        int targetHeight = source.getWidth() < 1100 ? source.getHeight() * 2 : source.getHeight();
        BufferedImage gray = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        RescaleOp rescaleOp = new RescaleOp(1.25f, -15f, null);
        return rescaleOp.filter(gray, null);
    }

    private void releaseQuietly(Mat mat) {
        if (mat != null) {
            mat.release();
        }
    }
}
