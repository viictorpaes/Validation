package com.cesar.validation.service.image;

import com.cesar.validation.exception.DocumentProcessingException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
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
        Mat grayMat = null;
        Mat resizedMat = null;
        Mat denoisedMat = null;
        Mat contrastMat = null;
        Mat deskewedMat = null;
        Mat paddedMat = null;
        MatOfDouble mean = null;
        MatOfDouble stdDev = null;

        try {
            int originalWidth = sourceImage.getWidth();
            int originalHeight = sourceImage.getHeight();

            bgrMat = bufferedImageToBgrMat(sourceImage);

            // 1. Conversão para escala de cinza
            grayMat = new Mat();
            Imgproc.cvtColor(bgrMat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // 2. Upscaling bicúbico inteligente para recortes pequenos ou baixa resolução
            double scale = computeOptimalScale(originalWidth, originalHeight);
            if (scale > 1.05) {
                resizedMat = new Mat();
                Imgproc.resize(
                    grayMat,
                    resizedMat,
                    new Size(Math.round(grayMat.cols() * scale), Math.round(grayMat.rows() * scale)),
                    0,
                    0,
                    Imgproc.INTER_CUBIC
                );
            } else {
                resizedMat = grayMat.clone();
            }

            // 3. Redução seletiva de ruídos (apenas filtro bilateral leve em fotos ruidosas,
            //    preservando separação sub-pixel de dígitos adjacentes como "11" em recortes nítidos)
            mean = new MatOfDouble();
            stdDev = new MatOfDouble();
            Core.meanStdDev(resizedMat, mean, stdDev);
            double contrastStd = stdDev.toArray().length > 0 ? stdDev.toArray()[0] : 50.0;

            denoisedMat = new Mat();
            if (contrastStd < 38.0) {
                // Foto escura/lavada com baixo contraste: filtro bilateral preserva bordas + CLAHE suave
                Imgproc.bilateralFilter(resizedMat, denoisedMat, 5, 35, 35);
                contrastMat = new Mat();
                CLAHE clahe = Imgproc.createCLAHE(1.6, new Size(12, 12));
                clahe.apply(denoisedMat, contrastMat);
            } else {
                // Imagem com bom contraste: normalização linear suave sem fundir glifos próximos
                contrastMat = new Mat();
                Core.normalize(resizedMat, contrastMat, 0, 255, Core.NORM_MINMAX);
            }

            // 4. Correção de inclinação apenas para páginas completas inclinadas
            if (originalHeight >= 450 && originalWidth >= 450) {
                deskewedMat = correctSkewIfNeeded(contrastMat);
            } else {
                deskewedMat = contrastMat.clone();
            }

            // 5. Margem branca protetora recomendada pelo motor LSTM do Tesseract 5
            paddedMat = new Mat();
            Core.copyMakeBorder(
                deskewedMat,
                paddedMat,
                24,
                24,
                24,
                24,
                Core.BORDER_CONSTANT,
                new Scalar(255)
            );

            return grayMatToBufferedImage(paddedMat);
        } finally {
            releaseQuietly(bgrMat);
            releaseQuietly(grayMat);
            releaseQuietly(resizedMat);
            releaseQuietly(denoisedMat);
            releaseQuietly(contrastMat);
            releaseQuietly(deskewedMat);
            releaseQuietly(paddedMat);
            releaseQuietly(mean);
            releaseQuietly(stdDev);
        }
    }

    private double computeOptimalScale(int width, int height) {
        double scaleByWidth = width < 1800 ? 1800.0 / Math.max(1, width) : 1.0;
        double scaleByHeight = height < 480 ? 480.0 / Math.max(1, height) : 1.0;
        double desired = Math.max(scaleByWidth, scaleByHeight);
        double capped = Math.min(desired, 3.2);
        if (width * capped > 3800) {
            capped = 3800.0 / width;
        }
        return Math.max(1.0, capped);
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
            if (nonZeroCoords.empty() || nonZeroCoords.rows() < 400) {
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

            if (Math.abs(angle) < 1.5 || Math.abs(angle) > 18.0) {
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
        double scale = computeOptimalScale(source.getWidth(), source.getHeight());
        int targetWidth = (int) Math.round(source.getWidth() * scale) + 48;
        int targetHeight = (int) Math.round(source.getHeight() * scale) + 48;
        BufferedImage gray = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.drawImage(
                source,
                24,
                24,
                targetWidth - 48,
                targetHeight - 48,
                null
            );
        } finally {
            g.dispose();
        }
        return gray;
    }

    private void releaseQuietly(Mat mat) {
        if (mat != null) {
            mat.release();
        }
    }
}
