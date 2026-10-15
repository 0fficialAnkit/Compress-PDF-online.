package com.example.pdfcompressor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PdfOrganizeService {

    /**
     * Merges multiple PDF byte arrays into a single combined PDF.
     */
    public byte[] mergePdfs(List<byte[]> pdfList) throws IOException {
        if (pdfList == null || pdfList.isEmpty()) {
            throw new IllegalArgumentException("At least one PDF file must be provided.");
        }
        if (pdfList.size() == 1) {
            return pdfList.get(0);
        }

        try (PDDocument mergedDoc = new PDDocument()) {
            for (byte[] pdfBytes : pdfList) {
                try (PDDocument sourceDoc = Loader.loadPDF(pdfBytes)) {
                    for (PDPage page : sourceDoc.getPages()) {
                        mergedDoc.importPage(page);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mergedDoc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Splits a PDF into either a single extracted page range or a ZIP of individual pages.
     *
     * @param input PDF bytes
     * @param mode "range" (extract specified pages) or "all" (split every page into a ZIP)
     * @param pageRange e.g. "1-3, 5" (1-indexed)
     */
    public byte[] splitPdf(byte[] input, String mode, String pageRange) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();

            if ("all".equalsIgnoreCase(mode) || pageRange == null || pageRange.trim().isEmpty()) {
                Splitter splitter = new Splitter();
                List<PDDocument> pages = splitter.split(document);

                ByteArrayOutputStream zipByteStream = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(zipByteStream)) {
                    for (int i = 0; i < pages.size(); i++) {
                        PDDocument singleDoc = pages.get(i);
                        ByteArrayOutputStream pageOut = new ByteArrayOutputStream();
                        singleDoc.save(pageOut);
                        singleDoc.close();

                        ZipEntry entry = new ZipEntry(String.format("page_%03d.pdf", i + 1));
                        zos.putNextEntry(entry);
                        zos.write(pageOut.toByteArray());
                        zos.closeEntry();
                    }
                }
                return zipByteStream.toByteArray();
            } else {
                // Extract specific page range
                Set<Integer> selectedPages = parsePageRange(pageRange, totalPages);
                if (selectedPages.isEmpty()) {
                    throw new IllegalArgumentException("No valid pages selected in range: " + pageRange);
                }

                try (PDDocument newDoc = new PDDocument()) {
                    for (int p = 1; p <= totalPages; p++) {
                        if (selectedPages.contains(p)) {
                            newDoc.importPage(document.getPage(p - 1));
                        }
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    newDoc.save(out);
                    return out.toByteArray();
                }
            }
        }
    }

    /**
     * Rotates pages by the specified angle (90, 180, 270 degrees).
     *
     * @param pageRange optional page range filter ("all", "1,2", "3-5")
     */
    public byte[] rotatePdf(byte[] input, int degrees, String pageRange) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();
            Set<Integer> selectedPages = (pageRange == null || pageRange.trim().equalsIgnoreCase("all") || pageRange.trim().isEmpty())
                    ? null
                    : parsePageRange(pageRange, totalPages);

            for (int i = 0; i < totalPages; i++) {
                int pageNum = i + 1;
                if (selectedPages == null || selectedPages.contains(pageNum)) {
                    PDPage page = document.getPage(i);
                    int currentRotation = page.getRotation();
                    int newRotation = (currentRotation + degrees) % 360;
                    if (newRotation < 0) newRotation += 360;
                    page.setRotation(newRotation);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Removes specified pages from the document.
     */
    public byte[] removePages(byte[] input, String pagesToRemoveStr) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();
            Set<Integer> pagesToRemove = parsePageRange(pagesToRemoveStr, totalPages);

            if (pagesToRemove.size() >= totalPages) {
                throw new IllegalArgumentException("Cannot remove all pages from document.");
            }

            try (PDDocument newDoc = new PDDocument()) {
                for (int i = 1; i <= totalPages; i++) {
                    if (!pagesToRemove.contains(i)) {
                        newDoc.importPage(document.getPage(i - 1));
                    }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                newDoc.save(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Unified organize pipeline: reorders pages, deletes selected pages, and applies rotation.
     */
    public byte[] organizePdf(byte[] input, String pageSequence, String pagesToRemove, int rotateDegrees) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();
            Set<Integer> removeSet = (pagesToRemove != null && !pagesToRemove.trim().isEmpty())
                    ? parsePageRange(pagesToRemove, totalPages)
                    : Collections.emptySet();

            List<Integer> orderList = new ArrayList<>();
            if (pageSequence != null && !pageSequence.trim().isEmpty() && !pageSequence.trim().equalsIgnoreCase("default")) {
                for (String part : pageSequence.split(",")) {
                    try {
                        int p = Integer.parseInt(part.trim());
                        if (p >= 1 && p <= totalPages && !removeSet.contains(p)) {
                            orderList.add(p);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                for (int i = 1; i <= totalPages; i++) {
                    if (!removeSet.contains(i)) {
                        orderList.add(i);
                    }
                }
            }

            if (orderList.isEmpty()) {
                throw new IllegalArgumentException("No pages remaining after organize filter.");
            }

            try (PDDocument newDoc = new PDDocument()) {
                for (int pageNum : orderList) {
                    PDPage page = document.getPage(pageNum - 1);
                    if (rotateDegrees != 0) {
                        int cur = page.getRotation();
                        int newRot = (cur + rotateDegrees) % 360;
                        if (newRot < 0) newRot += 360;
                        page.setRotation(newRot);
                    }
                    newDoc.importPage(page);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                newDoc.save(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Repairs and recovers readable PDF streams using PDFBox lenient loader and structure rebuild.
     */
    public byte[] repairPdf(byte[] input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Reorders pages based on a given 1-indexed page order list (e.g. "3,1,2").
     */
    public byte[] reorderPages(byte[] input, List<Integer> pageOrder) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            int totalPages = document.getNumberOfPages();

            try (PDDocument newDoc = new PDDocument()) {
                for (Integer pageIndex : pageOrder) {
                    if (pageIndex != null && pageIndex >= 1 && pageIndex <= totalPages) {
                        newDoc.importPage(document.getPage(pageIndex - 1));
                    }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                newDoc.save(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Flattens interactive form fields and annotations onto page contents.
     */
    public byte[] flattenPdf(byte[] input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                acroForm.flatten();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Crops page margins by a specified margin reduction in points (1 pt = 1/72 inch).
     */
    public byte[] cropPdf(byte[] input, float marginPts) throws IOException {
        try (PDDocument document = Loader.loadPDF(input)) {
            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                if (mediaBox != null) {
                    float newX = mediaBox.getLowerLeftX() + marginPts;
                    float newY = mediaBox.getLowerLeftY() + marginPts;
                    float newW = mediaBox.getWidth() - (2 * marginPts);
                    float newH = mediaBox.getHeight() - (2 * marginPts);

                    if (newW > 50 && newH > 50) {
                        PDRectangle cropBox = new PDRectangle(newX, newY, newW, newH);
                        page.setCropBox(cropBox);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Parses page strings like "1, 3-5, 8" into a 1-indexed set of page numbers.
     */
    public Set<Integer> parsePageRange(String pageRangeStr, int totalPages) {
        Set<Integer> pages = new TreeSet<>();
        if (pageRangeStr == null || pageRangeStr.trim().isEmpty()) {
            return pages;
        }

        String[] parts = pageRangeStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                if (range.length == 2) {
                    try {
                        int start = Math.max(1, Integer.parseInt(range[0].trim()));
                        int end = Math.min(totalPages, Integer.parseInt(range[1].trim()));
                        for (int i = start; i <= end; i++) {
                            pages.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    int p = Integer.parseInt(trimmed);
                    if (p >= 1 && p <= totalPages) {
                        pages.add(p);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return pages;
    }
}
