/**
 * Class to hold compression operation results and statistics
 */
public class CompressionResult {
    private boolean success;
    private String message;
    private long originalSize;
    private long compressedSize;
    private double compressionRatio;
    
    public CompressionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public CompressionResult(boolean success, String message, long originalSize, long compressedSize) {
        this.success = success;
        this.message = message;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        if (originalSize > 0) {
            this.compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;
        }
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getOriginalSize() {
        return originalSize;
    }
    
    public long getCompressedSize() {
        return compressedSize;
    }
    
    public double getCompressionRatio() {
        return compressionRatio;
    }
    
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

