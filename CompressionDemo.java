import java.io.File;

/**
 * Demo class to demonstrate file compression and decompression
 */
public class CompressionDemo {
    public static void main(String[] args) {
        FileCompressor compressor = new FileCompressor();
        
        System.out.println("========================================");
        System.out.println("  File Compression Demo");
        System.out.println("========================================");
        System.out.println();
        
        // Example: Compress a file
        if (args.length >= 2) {
            String sourceFile = args[0];
            String destFile = args[1];
            
            System.out.println("Compressing: " + sourceFile);
            System.out.println("Output: " + destFile);
            System.out.println();
            
            CompressionResult result = FileCompressor.compressFile(sourceFile, destFile);
            
            if (result.isSuccess()) {
                System.out.println("✓ Compression successful!");
                System.out.println("Original size: " + result.formatFileSize(result.getOriginalSize()));
                System.out.println("Compressed size: " + result.formatFileSize(result.getCompressedSize()));
                System.out.println("Compression ratio: " + String.format("%.2f", result.getCompressionRatio()) + "%");
            } else {
                System.out.println("✗ Compression failed: " + result.getMessage());
            }
        } else {
            System.out.println("Usage: java CompressionDemo <sourceFile> <destFile>");
            System.out.println("Example: java CompressionDemo input.txt input.txt.gz");
        }
    }
}

