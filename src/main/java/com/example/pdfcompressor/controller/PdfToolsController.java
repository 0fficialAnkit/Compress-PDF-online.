package com.example.pdfcompressor.controller;

import com.example.pdfcompressor.model.CompressionResponse;
import com.example.pdfcompressor.service.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/tools")
public class PdfToolsController {

    private final FileStorageService storageService;
    private final PdfOrganizeService organizeService;
    private final PdfPageNumberWatermarkService watermarkService;
    private final PdfSecurityService securityService;
    private final PdfConversionService conversionService;
    private final PdfOcrService ocrService;
    private final PdfAiService aiService;

    public PdfToolsController(FileStorageService storageService,
                              PdfOrganizeService organizeService,
                              PdfPageNumberWatermarkService watermarkService,
                              PdfSecurityService securityService,
                              PdfConversionService conversionService,
                              PdfOcrService ocrService,
                              PdfAiService aiService) {
        this.storageService = storageService;
        this.organizeService = organizeService;
        this.watermarkService = watermarkService;
        this.securityService = securityService;
        this.conversionService = conversionService;
        this.ocrService = ocrService;
        this.aiService = aiService;
    }

    // 0. Organize PDF (Reorder, Rotate, Delete)
    @PostMapping("/organize")
    public ResponseEntity<CompressionResponse> organize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "order", defaultValue = "default") String order,
            @RequestParam(value = "remove", required = false) String remove,
            @RequestParam(value = "rotate", defaultValue = "0") int rotate) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] organized = organizeService.organizePdf(input, order, remove, rotate);
            String outName = "organized-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, organized);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, organized.length, "PDF pages reorganized successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error organizing PDF: " + e.getMessage()));
        }
    }

    // 0.1 Repair PDF
    @PostMapping("/repair")
    public ResponseEntity<CompressionResponse> repair(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] repaired = organizeService.repairPdf(input);
            String outName = "repaired-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, repaired);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, repaired.length, "PDF structure analyzed and repaired."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error repairing PDF: " + e.getMessage()));
        }
    }

    // 1. Merge PDF
    @PostMapping("/merge")
    public ResponseEntity<CompressionResponse> merge(
            @RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(CompressionResponse.failure("merge.pdf", "Please upload at least 2 PDF files to merge."));
        }
        try {
            List<byte[]> pdfBytesList = new ArrayList<>();
            long totalInputSize = 0;
            for (MultipartFile f : files) {
                if (f.isEmpty()) continue;
                totalInputSize += f.getSize();
                pdfBytesList.add(f.getBytes());
            }

            byte[] merged = organizeService.mergePdfs(pdfBytesList);
            String id = storageService.store("merged-document.pdf", MediaType.APPLICATION_PDF_VALUE, merged);

            return ResponseEntity.ok(CompressionResponse.success(
                    id, "merged-document.pdf", totalInputSize, merged.length,
                    "Successfully merged " + pdfBytesList.size() + " PDF files."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure("merge.pdf", "Error merging PDFs: " + e.getMessage()));
        }
    }

    // 2. Split PDF
    @PostMapping("/split")
    public ResponseEntity<CompressionResponse> split(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "range") String mode,
            @RequestParam(value = "range", required = false) String range) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] inputBytes = file.getBytes();
            byte[] result = organizeService.splitPdf(inputBytes, mode, range);

            boolean isZip = "all".equalsIgnoreCase(mode) || range == null || range.trim().isEmpty();
            String outFilename = isZip ? "split-pages.zip" : "extracted-pages.pdf";
            String mime = isZip ? "application/zip" : MediaType.APPLICATION_PDF_VALUE;

            String id = storageService.store(outFilename, mime, result);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outFilename, inputBytes.length, result.length,
                    isZip ? "PDF split into individual pages archive." : "Selected pages extracted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error splitting PDF: " + e.getMessage()));
        }
    }

    // 3. Rotate PDF
    @PostMapping("/rotate")
    public ResponseEntity<CompressionResponse> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "degrees", defaultValue = "90") int degrees,
            @RequestParam(value = "range", defaultValue = "all") String range) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] rotated = organizeService.rotatePdf(input, degrees, range);
            String outName = "rotated-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, rotated);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, rotated.length, "Pages rotated by " + degrees + "°."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error rotating PDF: " + e.getMessage()));
        }
    }

    // 4. Remove Pages
    @PostMapping("/remove-pages")
    public ResponseEntity<CompressionResponse> removePages(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pages") String pages) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] result = organizeService.removePages(input, pages);
            String outName = "modified-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, result);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, result.length, "Removed pages: " + pages));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error removing pages: " + e.getMessage()));
        }
    }

    // 5. Crop PDF
    @PostMapping("/crop")
    public ResponseEntity<CompressionResponse> crop(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "margin", defaultValue = "20") float margin) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] cropped = organizeService.cropPdf(input, margin);
            String outName = "cropped-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, cropped);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, cropped.length, "Margins cropped by " + margin + " points."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error cropping PDF: " + e.getMessage()));
        }
    }

    // 6. Flatten PDF
    @PostMapping("/flatten")
    public ResponseEntity<CompressionResponse> flatten(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] flattened = organizeService.flattenPdf(input);
            String outName = "flattened-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, flattened);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, flattened.length, "PDF forms and annotations flattened."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error flattening PDF: " + e.getMessage()));
        }
    }

    // 7. Watermark PDF
    @PostMapping("/watermark")
    public ResponseEntity<CompressionResponse> watermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("text") String text,
            @RequestParam(value = "opacity", defaultValue = "0.25") float opacity,
            @RequestParam(value = "rotation", defaultValue = "45") float rotation,
            @RequestParam(value = "color", defaultValue = "#888888") String color,
            @RequestParam(value = "fontSize", defaultValue = "48") int fontSize) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] watermarked = watermarkService.addWatermark(input, text, opacity, rotation, color, fontSize);
            String outName = "watermarked-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, watermarked);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, watermarked.length, "Watermark applied successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error applying watermark: " + e.getMessage()));
        }
    }

    // 8. Add Page Numbers
    @PostMapping("/page-numbers")
    public ResponseEntity<CompressionResponse> pageNumbers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "Page {n} of {total}") String format,
            @RequestParam(value = "position", defaultValue = "bottom-center") String position,
            @RequestParam(value = "startNumber", defaultValue = "1") int startNumber) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] numbered = watermarkService.addPageNumbers(input, format, position, startNumber, 25);
            String outName = "numbered-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, numbered);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, numbered.length, "Page numbers added successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error adding page numbers: " + e.getMessage()));
        }
    }

    // 9. Protect PDF
    @PostMapping("/protect")
    public ResponseEntity<CompressionResponse> protect(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password,
            @RequestParam(value = "ownerPassword", required = false) String ownerPassword) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] protectedDoc = securityService.protectPdf(input, password, ownerPassword);
            String outName = "protected-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, protectedDoc);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, protectedDoc.length, "PDF password protection enabled."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error protecting PDF: " + e.getMessage()));
        }
    }

    // 10. Unlock PDF
    @PostMapping("/unlock")
    public ResponseEntity<CompressionResponse> unlock(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] unlockedDoc = securityService.unlockPdf(input, password);
            String outName = "unlocked-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, unlockedDoc);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, unlockedDoc.length, "Password protection removed successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Invalid password or corrupted file: " + e.getMessage()));
        }
    }

    // 11. Inspect PDF Metadata
    @PostMapping("/inspect")
    public ResponseEntity<CompressionResponse> inspect(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            Map<String, Object> metadata = securityService.inspectPdf(input, file.getOriginalFilename());
            CompressionResponse resp = CompressionResponse.success(
                    null, file.getOriginalFilename(), input.length, input.length, "Metadata inspected successfully.");
            resp.setData(metadata);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error inspecting PDF: " + e.getMessage()));
        }
    }

    // 12. Remove Metadata
    @PostMapping("/remove-metadata")
    public ResponseEntity<CompressionResponse> removeMetadata(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] cleaned = securityService.removeMetadata(input);
            String outName = "clean-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, cleaned);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, cleaned.length, "Metadata sanitized and removed."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error removing metadata: " + e.getMessage()));
        }
    }

    // 13. Redact PDF
    @PostMapping("/redact")
    public ResponseEntity<CompressionResponse> redact(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "text", required = false) String text) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] redacted = securityService.redactPdf(input, text);
            String outName = "redacted-" + file.getOriginalFilename();
            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, redacted);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, redacted.length, "Sensitive zones permanently redacted."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error redacting PDF: " + e.getMessage()));
        }
    }

    // 14. PDF to Images (JPG / PNG)
    @PostMapping("/pdf-to-img")
    public ResponseEntity<CompressionResponse> pdfToImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "jpg") String format,
            @RequestParam(value = "dpi", defaultValue = "150") int dpi) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] result = conversionService.pdfToImages(input, format, dpi);
            String ext = format.equalsIgnoreCase("png") ? "png" : "jpg";

            // If single image vs zip
            boolean isZip = result.length > 4 && result[0] == 0x50 && result[1] == 0x4B;
            String outName = isZip ? "pdf-images.zip" : "page-1." + ext;
            String mime = isZip ? "application/zip" : ("image/" + (ext.equals("png") ? "png" : "jpeg"));

            String id = storageService.store(outName, mime, result);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, result.length, "PDF pages converted to " + format.toUpperCase() + "."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error converting PDF to images: " + e.getMessage()));
        }
    }

    // 15. Images to PDF
    @PostMapping("/img-to-pdf")
    public ResponseEntity<CompressionResponse> imgToPdf(
            @RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure("images.pdf", "No images uploaded."));
        }
        try {
            List<byte[]> imageBytesList = new ArrayList<>();
            long totalSize = 0;
            for (MultipartFile f : files) {
                if (f.isEmpty()) continue;
                totalSize += f.getSize();
                imageBytesList.add(f.getBytes());
            }

            byte[] pdfResult = conversionService.imagesToPdf(imageBytesList);
            String id = storageService.store("images-combined.pdf", MediaType.APPLICATION_PDF_VALUE, pdfResult);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, "images-combined.pdf", totalSize, pdfResult.length,
                    "Converted " + imageBytesList.size() + " images to PDF document."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure("images.pdf", "Error converting images to PDF: " + e.getMessage()));
        }
    }

    // 16. PDF to Text
    @PostMapping("/pdf-to-text")
    public ResponseEntity<CompressionResponse> pdfToText(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            String text = conversionService.extractText(input);
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

            String outName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("(?i)\\.pdf$", ".txt") : "extracted.txt";
            String id = storageService.store(outName, MediaType.TEXT_PLAIN_VALUE, textBytes);

            CompressionResponse resp = CompressionResponse.success(
                    id, outName, input.length, textBytes.length, "Text extracted successfully.");
            resp.setData(Map.of("text", text));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error extracting text: " + e.getMessage()));
        }
    }

    // 17. Text to PDF
    @PostMapping("/text-to-pdf")
    public ResponseEntity<CompressionResponse> textToPdf(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "title", defaultValue = "Document") String title) {
        try {
            String content = text;
            long inputSize = 0;
            if (file != null && !file.isEmpty()) {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                inputSize = file.getSize();
            } else if (content != null) {
                inputSize = content.getBytes(StandardCharsets.UTF_8).length;
            }

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No text content provided."));
            }

            byte[] pdfResult = conversionService.textToPdf(content, title);
            String id = storageService.store("text-document.pdf", MediaType.APPLICATION_PDF_VALUE, pdfResult);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, "text-document.pdf", inputSize, pdfResult.length, "Text converted to PDF."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure("text.pdf", "Error creating PDF: " + e.getMessage()));
        }
    }

    // 18. Markdown to PDF
    @PostMapping("/markdown-to-pdf")
    public ResponseEntity<CompressionResponse> markdownToPdf(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "markdown", required = false) String markdown,
            @RequestParam(value = "title", defaultValue = "Markdown Document") String title) {
        try {
            String content = markdown;
            long inputSize = 0;
            if (file != null && !file.isEmpty()) {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                inputSize = file.getSize();
            } else if (content != null) {
                inputSize = content.getBytes(StandardCharsets.UTF_8).length;
            }

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No Markdown content provided."));
            }

            byte[] pdfResult = conversionService.markdownToPdf(content, title);
            String id = storageService.store("markdown-document.pdf", MediaType.APPLICATION_PDF_VALUE, pdfResult);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, "markdown-document.pdf", inputSize, pdfResult.length, "Markdown formatted and converted to PDF."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure("document.pdf", "Error parsing Markdown: " + e.getMessage()));
        }
    }

    // 19. PDF to Word (DOCX)
    @PostMapping("/pdf-to-word")
    public ResponseEntity<CompressionResponse> pdfToWord(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] docx = conversionService.pdfToWordDocx(input);
            String outName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("(?i)\\.pdf$", ".docx")
                    : "converted-document.docx";

            String id = storageService.store(outName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, docx.length, "PDF converted to editable Word (DOCX)."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error converting to Word: " + e.getMessage()));
        }
    }

    // 20. PDF to Excel (XLSX)
    @PostMapping("/pdf-to-excel")
    public ResponseEntity<CompressionResponse> pdfToExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] xlsx = conversionService.pdfToExcelXlsx(input);
            String outName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("(?i)\\.pdf$", ".xlsx")
                    : "extracted-table.xlsx";

            String id = storageService.store(outName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, xlsx.length, "Tables extracted to Excel (XLSX)."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error extracting to Excel: " + e.getMessage()));
        }
    }

    // 21. Extract Images
    @PostMapping("/extract-images")
    public ResponseEntity<CompressionResponse> extractImages(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] zipBytes = conversionService.extractImages(input);
            String outName = "extracted-images.zip";

            String id = storageService.store(outName, "application/zip", zipBytes);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, zipBytes.length, "Embedded images extracted into ZIP archive."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error extracting images: " + e.getMessage()));
        }
    }

    // 22. OCR Searchable PDF
    @PostMapping("/ocr")
    public ResponseEntity<CompressionResponse> ocr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", defaultValue = "eng") String lang) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            byte[] ocrPdf = ocrService.makeSearchablePdf(input, lang);
            String outName = "searchable-" + file.getOriginalFilename();

            String id = storageService.store(outName, MediaType.APPLICATION_PDF_VALUE, ocrPdf);
            return ResponseEntity.ok(CompressionResponse.success(
                    id, outName, input.length, ocrPdf.length, "OCR complete. Searchable PDF created."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "OCR processing failed: " + e.getMessage()));
        }
    }

    // 23. AI Summarize
    @PostMapping("/ai-summarize")
    public ResponseEntity<CompressionResponse> aiSummarize(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            Map<String, Object> summaryResult = aiService.summarize(input);

            CompressionResponse resp = CompressionResponse.success(
                    null, file.getOriginalFilename(), input.length, input.length, "Summary generated successfully.");
            resp.setData(summaryResult);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error generating summary: " + e.getMessage()));
        }
    }

    // 24. AI Chat / Q&A
    @PostMapping("/ai-chat")
    public ResponseEntity<CompressionResponse> aiChat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            Map<String, Object> chatResult = aiService.chatWithPdf(input, question);

            CompressionResponse resp = CompressionResponse.success(
                    null, file.getOriginalFilename(), input.length, input.length, "Answer grounded in document context.");
            resp.setData(chatResult);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error processing question: " + e.getMessage()));
        }
    }

    // 25. AI Key Points & Action Items
    @PostMapping("/ai-keypoints")
    public ResponseEntity<CompressionResponse> aiKeypoints(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            Map<String, Object> kpResult = aiService.extractKeyPoints(input);

            CompressionResponse resp = CompressionResponse.success(
                    null, file.getOriginalFilename(), input.length, input.length, "Key points and action items identified.");
            resp.setData(kpResult);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error extracting key points: " + e.getMessage()));
        }
    }

    // 26. AI Question Generation
    @PostMapping("/ai-questions")
    public ResponseEntity<CompressionResponse> aiQuestions(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(CompressionResponse.failure(null, "No file uploaded."));
        }
        try {
            byte[] input = file.getBytes();
            Map<String, Object> qResult = aiService.generateQuestions(input);

            CompressionResponse resp = CompressionResponse.success(
                    null, file.getOriginalFilename(), input.length, input.length, "Q&A study pairs generated.");
            resp.setData(qResult);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(file.getOriginalFilename(), "Error generating questions: " + e.getMessage()));
        }
    }
}
