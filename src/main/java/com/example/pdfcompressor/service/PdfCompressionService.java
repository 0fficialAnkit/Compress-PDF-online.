package com.example.pdfcompressor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Compresses PDF files by re-encoding the embedded raster images
 * (JPEG re-compression + optional downsampling). Text, vector
 * graphics and fonts are left untouched, so the readability of
 * actual document text is never affected - only photo/scan-style
 * images are shrunk.
 */
@Service
public class PdfCompressionService {

    public enum CompressionLevel {
        LOW(0.9f, 1.0),
        MEDIUM(0.6f, 0.85),
        HIGH(0.35f, 0.6);

        final float jpegQuality;
        final double scaleFactor;

        CompressionLevel(float jpegQuality, double scaleFactor) {
            this.jpegQuality = jpegQuality;
            this.scaleFactor = scaleFactor;
        }

        public static CompressionLevel fromString(String value) {
            if (value == null) {
                return MEDIUM;
            }
            try {
                return CompressionLevel.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MEDIUM;
            }
        }
    }

    /**
     * Compress the given PDF bytes and return the resulting PDF bytes.
     * Evaluates both native PDFBox raster downsampling and Ghostscript (if installed),
     * returning the result that achieves the highest valid size reduction.
     *
     * @param input the original PDF file contents
     * @param level how aggressively to compress
     * @return compressed PDF bytes
     */
    public byte[] compress(byte[] input, CompressionLevel level) throws IOException {
        byte[] pdfboxCompressed = compressWithPdfbox(input, level);

        // Attempt Ghostscript compression if binary is available
        byte[] gsCompressed = compressWithGhostscript(input, level);

        if (gsCompressed != null && gsCompressed.length > 0 && gsCompressed.length < pdfboxCompressed.length) {
            return gsCompressed;
        }

        return pdfboxCompressed;
    }

    public byte[] compressWithPdfbox(byte[] input, CompressionLevel level) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {

            // Track which image resources we've already replaced so a
            // shared/reused image isn't re-encoded once per page.
            Set<COSName> processed = new HashSet<>();

            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null) {
                    continue;
                }
                compressImagesInResources(document, resources, level, processed);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    public byte[] compressWithGhostscript(byte[] input, CompressionLevel level) {
        String gsPath = findGhostscript();
        if (gsPath == null) {
            return null;
        }

        String pdfSetting = switch (level) {
            case HIGH -> "/screen";
            case MEDIUM -> "/ebook";
            case LOW -> "/printer";
        };

        try {
            java.nio.file.Path tempIn = java.nio.file.Files.createTempFile("compress_in_", ".pdf");
            java.nio.file.Path tempOut = java.nio.file.Files.createTempFile("compress_out_", ".pdf");

            try {
                java.nio.file.Files.write(tempIn, input);

                ProcessBuilder pb = new ProcessBuilder(
                        gsPath,
                        "-sDEVICE=pdfwrite",
                        "-dCompatibilityLevel=1.4",
                        "-dPDFSETTINGS=" + pdfSetting,
                        "-dNOPAUSE",
                        "-dQUIET",
                        "-dBATCH",
                        "-sOutputFile=" + tempOut.toAbsolutePath().toString(),
                        tempIn.toAbsolutePath().toString()
                );

                Process process = pb.start();
                boolean finished = process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);

                if (finished && process.exitValue() == 0 && java.nio.file.Files.exists(tempOut)) {
                    byte[] outBytes = java.nio.file.Files.readAllBytes(tempOut);
                    if (outBytes.length > 100) {
                        return outBytes;
                    }
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tempIn);
                java.nio.file.Files.deleteIfExists(tempOut);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String findGhostscript() {
        String[] candidates = {"/opt/homebrew/bin/gs", "/usr/local/bin/gs", "/usr/bin/gs", "gs"};
        for (String path : candidates) {
            try {
                Process p = new ProcessBuilder(path, "--version").start();
                if (p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return path;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void compressImagesInResources(PDDocument document,
                                            PDResources resources,
                                            CompressionLevel level,
                                            Set<COSName> processed) throws IOException {
        for (COSName name : resources.getXObjectNames()) {
            if (processed.contains(name)) {
                continue;
            }
            var xobject = resources.getXObject(name);

            if (xobject instanceof PDImageXObject image) {
                processed.add(name);
                try {
                    PDImageXObject compressedImage = recompressImage(document, image, level);
                    if (compressedImage != null) {
                        resources.put(name, compressedImage);
                    }
                } catch (Exception e) {
                    // If a single image can't be re-encoded (e.g. an
                    // unusual color space or masked image), keep the
                    // original rather than fail the whole document.
                }
            } else if (xobject instanceof PDFormXObject form) {
                // Forms can contain nested images (e.g. watermarks, stamps).
                compressImagesInResources(document, form.getResources(), level, processed);
            }
        }
    }

    private PDImageXObject recompressImage(PDDocument document,
                                            PDImageXObject original,
                                            CompressionLevel level) throws IOException {
        BufferedImage bufferedImage = original.getImage();
        if (bufferedImage == null) {
            return null;
        }

        int newWidth = Math.max(1, (int) Math.round(bufferedImage.getWidth() * level.scaleFactor));
        int newHeight = Math.max(1, (int) Math.round(bufferedImage.getHeight() * level.scaleFactor));

        BufferedImage toEncode = bufferedImage;
        if (level.scaleFactor < 1.0
                && (newWidth < bufferedImage.getWidth() || newHeight < bufferedImage.getHeight())) {
            toEncode = scale(bufferedImage, newWidth, newHeight);
        }

        // JPEG has no transparency, so flatten any alpha channel onto
        // a white background before re-encoding.
        BufferedImage rgbImage = toEncode.getColorModel().hasAlpha()
                ? flattenAlpha(toEncode)
                : toEncode;

        return JPEGFactory.createFromImage(document, rgbImage, level.jpegQuality);
    }

    private BufferedImage flattenAlpha(BufferedImage source) {
        BufferedImage flattened = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flattened.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return flattened;
    }

    private BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }
}
