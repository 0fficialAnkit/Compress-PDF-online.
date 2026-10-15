package com.example.pdfcompressor.controller;

import com.example.pdfcompressor.model.CompressionResponse;
import com.example.pdfcompressor.service.FileStorageService;
import com.example.pdfcompressor.service.PdfCompressionService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class PdfController {

    /**
     * In-memory holding area for compressed files, keyed by a random id.
     * A file lives here just long enough for the browser to download it,
     * then it's evicted. Good enough for a single-instance demo app;
     * swap for a real store (disk, S3, Redis) for production use.
     */
    private final FileStorageService storageService;
    private final PdfCompressionService compressionService;

    public PdfController(FileStorageService storageService, PdfCompressionService compressionService) {
        this.storageService = storageService;
        this.compressionService = compressionService;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResponse> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "level", defaultValue = "MEDIUM") String level) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CompressionResponse.failure(null, "No file was uploaded."));
        }

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "document.pdf";

        if (!originalFilename.toLowerCase().endsWith(".pdf")
                && !MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(CompressionResponse.failure(originalFilename, "Only PDF files are supported."));
        }

        try {
            byte[] originalBytes = file.getBytes();
            PdfCompressionService.CompressionLevel compressionLevel =
                    PdfCompressionService.CompressionLevel.fromString(level);

            byte[] compressedBytes = compressionService.compress(originalBytes, compressionLevel);

            String downloadFilename = buildDownloadName(originalFilename);
            String id = storageService.store(downloadFilename, MediaType.APPLICATION_PDF_VALUE, compressedBytes);

            CompressionResponse response = new CompressionResponse(
                    id,
                    originalFilename,
                    originalBytes.length,
                    compressedBytes.length,
                    true,
                    "Compression complete."
            );
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(originalFilename,
                            "Could not process this PDF. It may be corrupted, encrypted, or not a valid PDF file."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CompressionResponse.failure(originalFilename,
                            "Unexpected error while compressing: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        FileStorageService.StoredFile stored = storageService.get(id);
        if (stored == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_PDF;
        if (stored.contentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(stored.contentType());
            } catch (Exception ignored) {
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(stored.filename()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(stored.data());
    }

    private String buildDownloadName(String originalFilename) {
        String base = originalFilename.toLowerCase().endsWith(".pdf")
                ? originalFilename.substring(0, originalFilename.length() - 4)
                : originalFilename;
        return base + "-compressed.pdf";
    }
}
