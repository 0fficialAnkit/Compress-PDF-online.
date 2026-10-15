package com.example.pdfcompressor.model;

/**
 * Result returned to the client after a PDF has been compressed.
 * Holds enough information for the UI to show before/after sizes
 * and to trigger the actual file download via the download id.
 */
public class CompressionResponse {

    private String id;
    private String originalFilename;
    private long originalSize;
    private long compressedSize;
    private double savingsPercent;
    private boolean success;
    private String message;
    private String downloadUrl;
    private java.util.Map<String, Object> data;

    public CompressionResponse() {
    }

    public CompressionResponse(String id, String originalFilename, long originalSize,
                                long compressedSize, boolean success, String message) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.success = success;
        this.message = message;
        this.downloadUrl = id != null ? "/api/download/" + id : null;
        this.savingsPercent = originalSize > 0
                ? (1.0 - ((double) compressedSize / (double) originalSize)) * 100.0
                : 0.0;
    }

    public static CompressionResponse success(String id, String originalFilename, long originalSize, long compressedSize, String message) {
        return new CompressionResponse(id, originalFilename, originalSize, compressedSize, true, message);
    }

    public static CompressionResponse failure(String originalFilename, String message) {
        return new CompressionResponse(null, originalFilename, 0, 0, false, message);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public double getSavingsPercent() {
        return savingsPercent;
    }

    public void setSavingsPercent(double savingsPercent) {
        this.savingsPercent = savingsPercent;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public java.util.Map<String, Object> getData() {
        return data;
    }

    public void setData(java.util.Map<String, Object> data) {
        this.data = data;
    }
}
