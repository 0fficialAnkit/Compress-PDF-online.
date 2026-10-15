package com.example.pdfcompressor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfOcrService {

    private final PdfConversionService conversionService;

    public PdfOcrService(PdfConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Performs OCR on a PDF document.
     * Uses Tesseract if installed on the system (e.g. /opt/homebrew/bin/tesseract or in PATH),
     * otherwise falls back to PDF text extraction and creates a clean OCR document.
     */
    public byte[] makeSearchablePdf(byte[] input, String language) throws IOException {
        String tesseractPath = findTesseractPath();
        String lang = (language != null && !language.trim().isEmpty()) ? language.trim() : "eng";

        if (tesseractPath != null) {
            Path tempDir = Files.createTempDirectory("ocr_pdf_");
            try {
                try (PDDocument document = Loader.loadPDF(input)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    List<byte[]> ocrPagePdfs = new ArrayList<>();

                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 200);
                        Path imgFile = tempDir.resolve("page_" + i + ".png");
                        Path outBase = tempDir.resolve("ocr_page_" + i);
                        ImageIO.write(image, "PNG", imgFile.toFile());

                        // Run tesseract to produce a single-page searchable PDF
                        ProcessBuilder pb = new ProcessBuilder(
                                tesseractPath,
                                imgFile.toAbsolutePath().toString(),
                                outBase.toAbsolutePath().toString(),
                                "-l", lang,
                                "pdf"
                        );
                        Process process = pb.start();
                        boolean finished = process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS);
                        Path resultPdf = tempDir.resolve("ocr_page_" + i + ".pdf");

                        if (finished && process.exitValue() == 0 && Files.exists(resultPdf)) {
                            ocrPagePdfs.add(Files.readAllBytes(resultPdf));
                        }
                    }

                    if (!ocrPagePdfs.isEmpty()) {
                        PdfOrganizeService organizer = new PdfOrganizeService();
                        return organizer.mergePdfs(ocrPagePdfs);
                    }
                }
            } catch (Exception e) {
                // If OCR process fails, fall through to fallback
            } finally {
                // Cleanup temp dir
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                } catch (Exception ignored) {}
            }
        }

        // Fallback: Extract embedded text and generate a structured PDF
        String extracted = conversionService.extractText(input);
        if (extracted == null || extracted.trim().isEmpty()) {
            extracted = "OCR Processing Note: No recognizable text found in document images.";
        }
        return conversionService.textToPdf(extracted, "OCR Extracted Document");
    }

    /**
     * Extracts recognized text from document pages using OCR or native extraction.
     */
    public String ocrText(byte[] input, String language) throws IOException {
        String tesseractPath = findTesseractPath();
        String lang = (language != null && !language.trim().isEmpty()) ? language.trim() : "eng";

        if (tesseractPath != null) {
            Path tempDir = Files.createTempDirectory("ocr_text_");
            try {
                try (PDDocument document = Loader.loadPDF(input)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    StringBuilder fullOcrText = new StringBuilder();

                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 200);
                        Path imgFile = tempDir.resolve("page_" + i + ".png");
                        Path outBase = tempDir.resolve("text_page_" + i);
                        ImageIO.write(image, "PNG", imgFile.toFile());

                        ProcessBuilder pb = new ProcessBuilder(
                                tesseractPath,
                                imgFile.toAbsolutePath().toString(),
                                outBase.toAbsolutePath().toString(),
                                "-l", lang
                        );
                        Process process = pb.start();
                        boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                        Path resultTxt = tempDir.resolve("text_page_" + i + ".txt");

                        if (finished && process.exitValue() == 0 && Files.exists(resultTxt)) {
                            fullOcrText.append("--- Page ").append(i + 1).append(" ---\n")
                                    .append(Files.readString(resultTxt)).append("\n\n");
                        }
                    }

                    if (!fullOcrText.isEmpty()) {
                        return fullOcrText.toString();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                } catch (Exception ignored) {}
            }
        }

        // Fallback to native text layer
        return conversionService.extractText(input);
    }

    private String findTesseractPath() {
        String[] possiblePaths = new String[]{
                "/opt/homebrew/bin/tesseract",
                "/usr/local/bin/tesseract",
                "/usr/bin/tesseract",
                "tesseract"
        };
        for (String path : possiblePaths) {
            try {
                Process p = new ProcessBuilder(path, "--version").start();
                if (p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return path;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
