import java.io.*;
import java.util.zip.*;


public class FileCompressor {
    
    /**
     * Compresses a file using GZIP compression
     * 
     * @param sourceFile Path to the file to compress
     * @param destFile Path to save the compressed file
     * @return CompressionResult with operation status and statistics
     */
    public static CompressionResult compressFile(String sourceFile, String destFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPOutputStream gzos = null;
        BufferedInputStream bis = null;
        
        try {
            // Create input stream for source file
            fis = new FileInputStream(sourceFile);
            bis = new BufferedInputStream(fis);
            
            // Create output stream for compressed file
            fos = new FileOutputStream(destFile);
            gzos = new GZIPOutputStream(fos);
            BufferedOutputStream bos = new BufferedOutputStream(gzos);
            
            // Read from source and write to compressed file
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            
            System.out.println("Compressing file: " + sourceFile);
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            bos.flush();
            gzos.finish();
            
            // Get file sizes
            File originalFile = new File(sourceFile);
            File compressedFile = new File(destFile);
            long originalSize = originalFile.length();
            long compressedSize = compressedFile.length();
            double compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;
            
            System.out.println("Compression completed!");
            System.out.println("Original size: " + originalSize + " bytes");
            System.out.println("Compressed size: " + compressedSize + " bytes");
            System.out.println("Compression ratio: " + String.format("%.2f", compressionRatio) + "%");
            
            return new CompressionResult(true, "Compression completed successfully!", 
                                       originalSize, compressedSize);
            
        } catch (FileNotFoundException e) {
            String errorMsg = "Error: Source file not found - " + e.getMessage();
            System.err.println(errorMsg);
            return new CompressionResult(false, errorMsg);
        } catch (IOException e) {
            String errorMsg = "Error during compression: " + e.getMessage();
            System.err.println(errorMsg);
            return new CompressionResult(false, errorMsg);
        } finally {
            // Close all streams
            try {
                if (bis != null) bis.close();
                if (fis != null) fis.close();
                if (gzos != null) gzos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                System.err.println("Error closing streams: " + e.getMessage());
            }
        }
    }
    
    /**
     * Decompresses a GZIP compressed file
     * 
     * @param sourceFile Path to the compressed file
     * @param destFile Path to save the decompressed file
     * @return CompressionResult with operation status and statistics
     */
    public static CompressionResult decompressFile(String sourceFile, String destFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPInputStream gzis = null;
        BufferedOutputStream bos = null;
        
        try {
            // Create input stream for compressed file
            fis = new FileInputStream(sourceFile);
            gzis = new GZIPInputStream(fis);
            BufferedInputStream bis = new BufferedInputStream(gzis);
            
            // Create output stream for decompressed file
            fos = new FileOutputStream(destFile);
            bos = new BufferedOutputStream(fos);
            
            // Read from compressed file and write to decompressed file
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            
            System.out.println("Decompressing file: " + sourceFile);
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            bos.flush();
            
            // Get file sizes
            File compressedFile = new File(sourceFile);
            File decompressedFile = new File(destFile);
            long compressedSize = compressedFile.length();
            long decompressedSize = decompressedFile.length();
            
            System.out.println("Decompression completed!");
            System.out.println("Compressed size: " + compressedSize + " bytes");
            System.out.println("Decompressed size: " + decompressedSize + " bytes");
            
            return new CompressionResult(true, "Decompression completed successfully!", 
                                       compressedSize, decompressedSize);
            
        } catch (FileNotFoundException e) {
            String errorMsg = "Error: Source file not found - " + e.getMessage();
            System.err.println(errorMsg);
            return new CompressionResult(false, errorMsg);
        } catch (ZipException e) {
            String errorMsg = "Error: File is not a valid GZIP file - " + e.getMessage();
            System.err.println(errorMsg);
            return new CompressionResult(false, errorMsg);
        } catch (IOException e) {
            String errorMsg = "Error during decompression: " + e.getMessage();
            System.err.println(errorMsg);
            return new CompressionResult(false, errorMsg);
        } finally {
            // Close all streams
            try {
                if (gzis != null) gzis.close();
                if (fis != null) fis.close();
                if (bos != null) bos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                System.err.println("Error closing streams: " + e.getMessage());
            }
        }
    }
    
    /**
     * Compresses multiple files into a ZIP archive
     * 
     * @param filePaths Array of file paths to compress
     * @param zipFilePath Path to save the ZIP file
     * @return true if compression successful, false otherwise
     */
    public static boolean compressToZip(String[] filePaths, String zipFilePath) {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        
        try {
            fos = new FileOutputStream(zipFilePath);
            zos = new ZipOutputStream(fos);
            BufferedOutputStream bos = new BufferedOutputStream(zos);
            
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println("Warning: File not found - " + filePath);
                    continue;
                }
                
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                
                // Create zip entry
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);
                
                // Write file content to zip
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                
                bis.close();
                fis.close();
                zos.closeEntry();
                
                System.out.println("Added to ZIP: " + file.getName());
            }
            
            bos.flush();
            zos.finish();
            
            System.out.println("ZIP file created successfully: " + zipFilePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error creating ZIP file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (zos != null) zos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                System.err.println("Error closing streams: " + e.getMessage());
            }
        }
    }
    
    /**
     * Decompresses a ZIP archive
     * 
     * @param zipFilePath Path to the ZIP file
     * @param destDirectory Directory to extract files to
     * @return true if decompression successful, false otherwise
     */
    public static boolean decompressZip(String zipFilePath, String destDirectory) {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        
        try {
            fis = new FileInputStream(zipFilePath);
            zis = new ZipInputStream(fis);
            BufferedInputStream bis = new BufferedInputStream(zis);
            
            // Create destination directory if it doesn't exist
            File destDir = new File(destDirectory);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File outputFile = new File(destDirectory, entryName);
                
                // Create parent directories if needed
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                    continue;
                }
                
                outputFile.getParentFile().mkdirs();
                
                // Extract file
                FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                
                bos.flush();
                bos.close();
                fos.close();
                
                System.out.println("Extracted: " + entryName);
                zis.closeEntry();
            }
            
            System.out.println("ZIP file extracted successfully to: " + destDirectory);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error extracting ZIP file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (zis != null) zis.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                System.err.println("Error closing streams: " + e.getMessage());
            }
        }
    }
}

