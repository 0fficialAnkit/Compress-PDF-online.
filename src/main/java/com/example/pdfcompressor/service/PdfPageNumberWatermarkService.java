package com.example.pdfcompressor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfPageNumberWatermarkService {

    /**
     * Adds diagonal or horizontal transparent text watermark across all pages.
     */
    public byte[] addWatermark(byte[] input, String watermarkText, float opacity, float rotationDegrees,
                               String colorHex, int fontSize) throws IOException {
        if (watermarkText == null || watermarkText.trim().isEmpty()) {
            return input;
        }

        Color color = parseColor(colorHex, new Color(180, 180, 180));
        float effectiveOpacity = Math.max(0.05f, Math.min(1.0f, opacity));
        int effectiveSize = fontSize > 0 ? fontSize : 48;

        try (PDDocument document = Loader.loadPDF(input)) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();

                float textWidth = font.getStringWidth(watermarkText) / 1000f * effectiveSize;
                float centerX = (width - textWidth) / 2f;
                float centerY = height / 2f;

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(effectiveOpacity);
                    cs.setGraphicsStateParameters(gs);

                    cs.setNonStrokingColor(color);
                    cs.beginText();
                    cs.setFont(font, effectiveSize);

                    Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotationDegrees), width / 2f, height / 2f);
                    matrix.translate(centerX - (width / 2f), centerY - (height / 2f));
                    cs.setTextMatrix(matrix);

                    cs.showText(watermarkText);
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Adds page numbers to pages with customized format and positioning.
     *
     * @param format "Page {n}", "{n} / {total}", or "{n}"
     * @param position "bottom-center", "bottom-right", "bottom-left", "top-center", "top-right"
     */
    public byte[] addPageNumbers(byte[] input, String format, String position, int startNumber, int marginPts) throws IOException {
        String effectiveFormat = (format == null || format.trim().isEmpty()) ? "Page {n} of {total}" : format;
        String effectivePosition = (position == null || position.trim().isEmpty()) ? "bottom-center" : position.toLowerCase();
        int effectiveStart = startNumber > 0 ? startNumber : 1;
        float margin = marginPts > 0 ? marginPts : 25f;

        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float fontSize = 10f;

            for (int i = 0; i < totalPages; i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();

                int currentNumber = effectiveStart + i;
                String text = effectiveFormat
                        .replace("{n}", String.valueOf(currentNumber))
                        .replace("{total}", String.valueOf(totalPages + effectiveStart - 1));

                float textWidth = font.getStringWidth(text) / 1000f * fontSize;

                float x;
                float y;

                if (effectivePosition.contains("top")) {
                    y = height - margin;
                } else {
                    y = margin;
                }

                if (effectivePosition.contains("right")) {
                    x = width - margin - textWidth;
                } else if (effectivePosition.contains("left")) {
                    x = margin;
                } else {
                    x = (width - textWidth) / 2f;
                }

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.setNonStrokingColor(Color.DARK_GRAY);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(x, y);
                    cs.showText(text);
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.trim().isEmpty()) {
            return defaultColor;
        }
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() == 6) {
                int r = Integer.parseInt(clean.substring(0, 2), 16);
                int g = Integer.parseInt(clean.substring(2, 4), 16);
                int b = Integer.parseInt(clean.substring(4, 6), 16);
                return new Color(r, g, b);
            }
        } catch (Exception ignored) {}
        return defaultColor;
    }
}
