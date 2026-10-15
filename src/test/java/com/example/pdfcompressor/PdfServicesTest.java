package com.example.pdfcompressor;

import com.example.pdfcompressor.service.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PdfServicesTest {

    private PdfCompressionService compressionService;
    private PdfOrganizeService organizeService;
    private PdfPageNumberWatermarkService watermarkService;
    private PdfSecurityService securityService;
    private PdfConversionService conversionService;
    private byte[] samplePdfBytes;

    @BeforeEach
    public void setup() throws IOException {
        compressionService = new PdfCompressionService();
        organizeService = new PdfOrganizeService();
        watermarkService = new PdfPageNumberWatermarkService();
        securityService = new PdfSecurityService();
        conversionService = new PdfConversionService();

        // Create a 3-page sample PDF
        try (PDDocument doc = new PDDocument()) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 18);
                    cs.newLineAtOffset(100, 700);
                    cs.showText("Sample Page " + i + " content for testing.");
                    cs.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            samplePdfBytes = out.toByteArray();
        }
    }

    @Test
    public void testCompressionServicePreserved() throws IOException {
        byte[] compressed = compressionService.compress(samplePdfBytes, PdfCompressionService.CompressionLevel.MEDIUM);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
    }

    @Test
    public void testMergePdfs() throws IOException {
        byte[] merged = organizeService.mergePdfs(List.of(samplePdfBytes, samplePdfBytes));
        assertNotNull(merged);
        try (PDDocument doc = Loader.loadPDF(merged)) {
            assertEquals(6, doc.getNumberOfPages());
        }
    }

    @Test
    public void testSplitPdf() throws IOException {
        byte[] extracted = organizeService.splitPdf(samplePdfBytes, "range", "1,3");
        try (PDDocument doc = Loader.loadPDF(extracted)) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    public void testRotatePdf() throws IOException {
        byte[] rotated = organizeService.rotatePdf(samplePdfBytes, 90, "all");
        try (PDDocument doc = Loader.loadPDF(rotated)) {
            assertEquals(90, doc.getPage(0).getRotation());
        }
    }

    @Test
    public void testWatermarkAndPageNumbers() throws IOException {
        byte[] watermarked = watermarkService.addWatermark(samplePdfBytes, "CONFIDENTIAL", 0.3f, 45, "#FF0000", 36);
        assertNotNull(watermarked);

        byte[] numbered = watermarkService.addPageNumbers(watermarked, "Page {n} of {total}", "bottom-center", 1, 20);
        assertNotNull(numbered);
        try (PDDocument doc = Loader.loadPDF(numbered)) {
            assertEquals(3, doc.getNumberOfPages());
        }
    }

    @Test
    public void testProtectAndUnlockPdf() throws IOException {
        String password = "secretPassword123";
        byte[] protectedPdf = securityService.protectPdf(samplePdfBytes, password, null);
        assertNotNull(protectedPdf);

        // Verify protected
        try (PDDocument doc = Loader.loadPDF(protectedPdf, password)) {
            assertTrue(doc.isEncrypted());
        }

        // Unlock
        byte[] unlockedPdf = securityService.unlockPdf(protectedPdf, password);
        try (PDDocument doc = Loader.loadPDF(unlockedPdf)) {
            assertFalse(doc.isEncrypted());
        }
    }

    @Test
    public void testInspectMetadata() throws IOException {
        Map<String, Object> info = securityService.inspectPdf(samplePdfBytes, "test.pdf");
        assertNotNull(info);
        assertEquals(3, info.get("pageCount"));
        assertEquals("test.pdf", info.get("filename"));
    }

    @Test
    public void testOrganizePdfPipeline() throws IOException {
        // Reorder sample 3-page PDF to "3,1,2" and rotate by 90
        byte[] organized = organizeService.organizePdf(samplePdfBytes, "3,1,2", null, 90);
        assertNotNull(organized);
        try (PDDocument doc = Loader.loadPDF(organized)) {
            assertEquals(3, doc.getNumberOfPages());
            assertEquals(90, doc.getPage(0).getRotation());
        }

        // Reorder and remove page 2
        byte[] organizedWithRemove = organizeService.organizePdf(samplePdfBytes, "default", "2", 0);
        try (PDDocument doc = Loader.loadPDF(organizedWithRemove)) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    public void testRepairPdf() throws IOException {
        byte[] repaired = organizeService.repairPdf(samplePdfBytes);
        assertNotNull(repaired);
        try (PDDocument doc = Loader.loadPDF(repaired)) {
            assertEquals(3, doc.getNumberOfPages());
        }
    }

    @Test
    public void testPdfToJpgAndImagesToPdf() throws IOException {
        // 1. PDF to JPG (multi-page returns ZIP)
        byte[] zipBytes = conversionService.pdfToImages(samplePdfBytes, "jpg", 150);
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 50);
        // Verify ZIP header (PK\x03\x04)
        assertEquals(0x50, zipBytes[0]);
        assertEquals(0x4B, zipBytes[1]);

        // 2. Generate small sample image in memory
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "JPG", imgOut);
        byte[] dummyJpg = imgOut.toByteArray();

        // 3. JPG to PDF
        byte[] generatedPdf = conversionService.imagesToPdf(List.of(dummyJpg, dummyJpg));
        assertNotNull(generatedPdf);
        try (PDDocument doc = Loader.loadPDF(generatedPdf)) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    public void testTextAndMarkdownConversion() throws IOException {
        String text = conversionService.extractText(samplePdfBytes);
        assertTrue(text.contains("Sample Page 1"));

        byte[] fromText = conversionService.textToPdf("Hello World from Plain Text", "Title");
        assertNotNull(fromText);

        byte[] fromMd = conversionService.markdownToPdf("# Heading 1\n\nParagraph text\n- Bullet item", "Doc");
        assertNotNull(fromMd);

        byte[] docx = conversionService.pdfToWordDocx(samplePdfBytes);
        assertNotNull(docx);
        assertTrue(docx.length > 100);
    }
}
