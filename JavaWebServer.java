import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Pure Java Web Server - No JavaScript Required
 * Server-side rendering with Java-only implementation
 */
public class JavaWebServer {
    private static final int PORT = 8080;
    private static String networkIP = "localhost";
    
    public static void main(String[] args) throws IOException {
        // Get network IP address for mobile access
        networkIP = getNetworkIP();
        
        // Bind to all network interfaces (0.0.0.0) to allow mobile access
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        // Serve pages
        server.createContext("/", new HomePageHandler());
        server.createContext("/compress", new CompressPageHandler());
        server.createContext("/decompress", new DecompressPageHandler());
        server.createContext("/api/compress", new CompressAPIHandler());
        server.createContext("/api/decompress", new DecompressAPIHandler());
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("========================================");
        System.out.println("  Pure Java Web Server");
        System.out.println("  No JavaScript Required!");
        System.out.println("========================================");
        System.out.println("Server started on http://localhost:" + PORT);
        System.out.println("Open your browser: http://localhost:" + PORT);
        if (!networkIP.equals("localhost")) {
            System.out.println("");
            System.out.println("📱 MOBILE ACCESS:");
            System.out.println("   URL: http://" + networkIP + ":" + PORT);
            System.out.println("");
            System.out.println("   Instructions:");
            System.out.println("   1. Connect mobile device to same Wi-Fi network");
            System.out.println("   2. Open the URL above in mobile browser");
            System.out.println("   3. If connection fails, run configure-firewall.bat as Administrator");
            System.out.println("");
        } else {
            System.out.println("");
            System.out.println("⚠️  Mobile access not available (no network interface found)");
            System.out.println("");
        }
        System.out.println("========================================");
        System.out.println("Press Ctrl+C to stop the server");
    }
    
    // Get network IP address for mobile access
    private static String getNetworkIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress()) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    // Prefer IPv4 addresses
                    if (ip.indexOf(':') == -1) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not determine network IP: " + e.getMessage());
        }
        return "localhost";
    }
    
    // Home Page Handler
    static class HomePageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateHomePage();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }
    
    // Compress Page Handler
    static class CompressPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateCompressPage();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }
    
    // Decompress Page Handler
    static class DecompressPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateDecompressPage();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }
    
    // Compress API Handler
    static class CompressAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // Parse multipart form data
                    InputStream requestBody = exchange.getRequestBody();
                    byte[] data = requestBody.readAllBytes();
                    
                    // Simple multipart parsing
                    String boundary = extractBoundary(exchange);
                    if (boundary != null) {
                        Map<String, byte[]> files = parseMultipart(data, boundary);
                        
                        if (!files.isEmpty()) {
                            Map.Entry<String, byte[]> entry = files.entrySet().iterator().next();
                            String filename = entry.getKey();
                            byte[] fileData = entry.getValue();
                            
                            // Save to temp file
                            File tempFile = File.createTempFile("compress_", "_" + filename);
                            Files.write(tempFile.toPath(), fileData);
                            
                            // Compress
                            String compressedPath = tempFile.getAbsolutePath() + ".gz";
                            CompressionResult result = FileCompressor.compressFile(
                                tempFile.getAbsolutePath(), compressedPath);
                            
                            if (result.isSuccess()) {
                                byte[] compressedData = Files.readAllBytes(Paths.get(compressedPath));
                                
                                // Generate HTML response with download link and statistics
                                String html = generateCompressResultPage(
                                    filename,
                                    result.getOriginalSize(),
                                    result.getCompressedSize(),
                                    result.getCompressionRatio(),
                                    compressedData,
                                    filename + ".gz"
                                );
                                
                                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                                
                                // Cleanup
                                tempFile.delete();
                                new File(compressedPath).delete();
                                return;
                            }
                        }
                    }
                    
                    String errorHtml = generateErrorPage("Compression failed. Please try again.");
                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                } catch (Exception e) {
                    String errorHtml = generateErrorPage("Error: " + e.getMessage());
                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method not allowed");
            }
        }
    }
    
    // Decompress API Handler
    static class DecompressAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream requestBody = exchange.getRequestBody();
                    byte[] data = requestBody.readAllBytes();
                    
                    String boundary = extractBoundary(exchange);
                    if (boundary != null) {
                        Map<String, byte[]> files = parseMultipart(data, boundary);
                        
                        if (!files.isEmpty()) {
                            Map.Entry<String, byte[]> entry = files.entrySet().iterator().next();
                            String filename = entry.getKey();
                            byte[] fileData = entry.getValue();
                            
                            File tempFile = File.createTempFile("decompress_", "_" + filename);
                            Files.write(tempFile.toPath(), fileData);
                            
                            String outputFilename;
                            String decompressedPath = tempFile.getAbsolutePath() + "_decompressed";
                            
                            CompressionResult result;
                            long compressedSize = fileData.length;
                            long decompressedSize = 0;
                            byte[] decompressedData = null;
                            
                            // Determine file type and set output filename
                            String lowerFilename = filename.toLowerCase();
                            if (lowerFilename.endsWith(".zip")) {
                                outputFilename = filename.substring(0, filename.length() - 4) + "_extracted";
                            } else if (lowerFilename.endsWith(".gz")) {
                                outputFilename = filename.substring(0, filename.length() - 3);
                            } else {
                                outputFilename = filename + "_decompressed";
                            }
                            
                            if (lowerFilename.endsWith(".zip")) {
                                String extractDir = tempFile.getParent() + File.separator + "extracted";
                                boolean success = FileCompressor.decompressZip(
                                    tempFile.getAbsolutePath(), extractDir);
                                
                                File extractDirFile = new File(extractDir);
                                File[] extractedFiles = extractDirFile.listFiles();
                                
                                if (success && extractedFiles != null && extractedFiles.length > 0) {
                                    // Get all extracted files
                                    for (File extractedFile : extractedFiles) {
                                        if (extractedFile.isFile()) {
                                            decompressedData = Files.readAllBytes(extractedFile.toPath());
                                            decompressedSize = decompressedData.length;
                                            outputFilename = extractedFile.getName();
                                            break; // Use first file for now
                                        }
                                    }
                                    
                                    if (decompressedData != null) {
                                        String html = generateDecompressResultPage(
                                            filename,
                                            compressedSize,
                                            decompressedSize,
                                            outputFilename,
                                            decompressedData
                                        );
                                        
                                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                        sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                                        
                                        deleteDirectory(extractDirFile);
                                        tempFile.delete();
                                        return;
                                    } else {
                                        String errorHtml = generateErrorPage("ZIP file extracted but no files found inside.");
                                        sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                                        deleteDirectory(extractDirFile);
                                        tempFile.delete();
                                        return;
                                    }
                                } else {
                                    String errorMsg = success ? "ZIP file extracted but no files found." : "Failed to extract ZIP file.";
                                    String errorHtml = generateErrorPage(errorMsg);
                                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                                    if (extractDirFile.exists()) {
                                        deleteDirectory(extractDirFile);
                                    }
                                    tempFile.delete();
                                    return;
                                }
                            } else {
                                // GZIP decompression
                                result = FileCompressor.decompressFile(
                                    tempFile.getAbsolutePath(), decompressedPath);
                                
                                if (result.isSuccess()) {
                                    File decompressedFile = new File(decompressedPath);
                                    if (decompressedFile.exists()) {
                                        decompressedData = Files.readAllBytes(Paths.get(decompressedPath));
                                        decompressedSize = decompressedData.length;
                                        
                                        String html = generateDecompressResultPage(
                                            filename,
                                            compressedSize,
                                            decompressedSize,
                                            outputFilename,
                                            decompressedData
                                        );
                                        
                                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                                        sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                                        
                                        tempFile.delete();
                                        decompressedFile.delete();
                                        return;
                                    } else {
                                        String errorHtml = generateErrorPage("Decompression completed but output file not found.");
                                        sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                                        tempFile.delete();
                                        return;
                                    }
                                } else {
                                    // Show actual error message from decompression
                                    String errorHtml = generateErrorPage("Decompression failed: " + result.getMessage());
                                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                                    tempFile.delete();
                                    return;
                                }
                            }
                        }
                    }
                    
                    String errorHtml = generateErrorPage("Decompression failed. Please try again.");
                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                } catch (Exception e) {
                    String errorHtml = generateErrorPage("Error: " + e.getMessage());
                    sendResponse(exchange, 500, "text/html; charset=UTF-8", errorHtml);
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method not allowed");
            }
        }
    }
    
    // Upload Handler
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "text/plain", "OK");
        }
    }
    
    // Generate Home Page HTML
    private static String generateHomePage() {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Smart File Compressor Utility</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
                <meta http-equiv="Pragma" content="no-cache">
                <meta http-equiv="Expires" content="0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        padding: 40px 0;
                    }
                    .header h1 {
                        font-size: 3em;
                        margin: 0;
                        text-shadow: 2px 2px 10px rgba(0,0,0,0.3);
                    }
                    .header p {
                        font-size: 1.2em;
                        margin-top: 10px;
                    }
                    .cards {
                        display: flex;
                        gap: 30px;
                        justify-content: center;
                        flex-wrap: wrap;
                        margin-top: 40px;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.2);
                        backdrop-filter: blur(10px);
                        border: 1px solid rgba(255, 255, 255, 0.3);
                        border-radius: 20px;
                        padding: 40px;
                        text-align: center;
                        color: white;
                        text-decoration: none;
                        display: block;
                        width: 300px;
                        transition: transform 0.3s;
                        cursor: pointer;
                    }
                    .card:active {
                        transform: scale(0.95);
                    }
                    .card h2 {
                        margin: 20px 0;
                        font-size: 2em;
                    }
                    .card p {
                        font-size: 1.1em;
                        line-height: 1.6;
                    }
                    .icon {
                        font-size: 4em;
                    }
                    .footer {
                        text-align: center;
                        color: white;
                        margin-top: 60px;
                        padding: 20px;
                    }
                    @media (max-width: 768px) {
                        .header h1 {
                            font-size: 2em;
                        }
                        .header p {
                            font-size: 1em;
                        }
                        .card {
                            width: 100%;
                            max-width: 400px;
                            padding: 30px 20px;
                        }
                        .card h2 {
                            font-size: 1.5em;
                        }
                        .card p {
                            font-size: 1em;
                        }
                        .icon {
                            font-size: 3em;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Smart File Compressor Utility</h1>
                        <p>Compress and decompress files with ease</p>
                    </div>
        """);
        
        html.append("""
                    <div class="cards">
                        <a href="/compress" class="card">
                            <div class="icon">🗜️</div>
                            <h2>Compress Files</h2>
                            <p>Reduce file sizes using GZIP compression</p>
                        </a>
                        <a href="/decompress" class="card">
                            <div class="icon">📂</div>
                            <h2>Decompress Files</h2>
                            <p>Extract compressed ZIP and GZIP files</p>
                        </a>
                    </div>
                    <div class="footer">
                        <p>File Compression Utility</p>
                    </div>
                </div>
            </body>
            </html>
        """);
        
        return html.toString();
    }
    
    // Generate Compress Page HTML
    private static String generateCompressPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Compress Files</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
                <meta http-equiv="Pragma" content="no-cache">
                <meta http-equiv="Expires" content="0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        padding: 20px 0;
                    }
                    .header h1 {
                        font-size: 2.5em;
                        margin: 0;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 20px;
                        padding: 30px;
                        margin: 20px 0;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    }
                    .form-group {
                        margin: 20px 0;
                    }
                    label {
                        display: block;
                        margin-bottom: 10px;
                        font-weight: bold;
                        color: #333;
                    }
                    input[type="file"] {
                        width: 100%;
                        padding: 15px;
                        border: 2px dashed #667eea;
                        border-radius: 10px;
                        background: #f5f5f5;
                        box-sizing: border-box;
                        font-size: 16px;
                        min-height: 50px;
                        display: block;
                        visibility: visible;
                        opacity: 1;
                        position: relative;
                        z-index: 1;
                        cursor: pointer;
                        -webkit-tap-highlight-color: rgba(102, 126, 234, 0.3);
                    }
                    input[type="file"]:active {
                        background: #e8e8e8;
                    }
                    @media (max-width: 768px) {
                        input[type="file"] {
                            font-size: 18px;
                            padding: 25px 20px;
                            min-height: 70px;
                            width: 100%;
                            display: block !important;
                            visibility: visible !important;
                            opacity: 1 !important;
                            touch-action: manipulation;
                            -webkit-appearance: none;
                        }
                        .form-group {
                            margin: 30px 0;
                        }
                        label {
                            font-size: 1.1em;
                            margin-bottom: 15px;
                        }
                        button {
                            width: 100%;
                            padding: 20px;
                            font-size: 1.2em;
                        }
                        .card {
                            padding: 20px;
                        }
                    }
                    button {
                        background: linear-gradient(135deg, #667eea, #764ba2);
                        color: white;
                        border: none;
                        padding: 15px 40px;
                        border-radius: 50px;
                        font-size: 1.1em;
                        cursor: pointer;
                        margin: 10px 5px;
                    }
                    button:hover {
                        transform: scale(1.05);
                        box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
                    }
                    .back-button {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 50px;
                        text-decoration: none;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                    .back-button:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }
                    .stats {
                        background: #e8f4f8;
                        border: 1px solid #b3d9e6;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 20px 0;
                        display: none;
                    }
                    .stats.show {
                        display: block;
                    }
                    .stat-item {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #b3d9e6;
                    }
                    .stat-item:last-child {
                        border-bottom: none;
                    }
                    .stat-label {
                        font-weight: bold;
                        color: #333;
                    }
                    .stat-value {
                        color: #667eea;
                        font-weight: bold;
                    }
                    .compression-ratio {
                        font-size: 1.5em;
                        color: #48bb78;
                        text-align: center;
                        padding: 20px;
                        background: #d4edda;
                        border-radius: 10px;
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <a href="/" class="back-button">← Back to Home</a>
                    <div class="header">
                        <h1>🗜️ Compress Files</h1>
                    </div>
                    <div class="card">
                        <form action="/api/compress" method="post" enctype="multipart/form-data" autocomplete="off">
                            <div class="form-group">
                                <label for="file">Select File to Compress:</label>
                                <input type="file" id="file" name="file" required autocomplete="off" accept="*/*">
                            </div>
                            <button type="submit">Compress File</button>
                        </form>
                        <div id="stats" class="stats">
                            <h3 style="margin-top: 0;">Compression Statistics</h3>
                            <div class="stat-item">
                                <span class="stat-label">Original File:</span>
                                <span class="stat-value" id="originalFile">-</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">Original Size:</span>
                                <span class="stat-value" id="originalSize">-</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">Compressed Size:</span>
                                <span class="stat-value" id="compressedSize">-</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">Space Saved:</span>
                                <span class="stat-value" id="spaceSaved">-</span>
                            </div>
                            <div class="compression-ratio" id="compressionRatio">
                                Compression Ratio: -
                            </div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    // Generate Decompress Page HTML
    private static String generateDecompressPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Decompress Files</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
                <meta http-equiv="Pragma" content="no-cache">
                <meta http-equiv="Expires" content="0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        padding: 20px 0;
                    }
                    .header h1 {
                        font-size: 2.5em;
                        margin: 0;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 20px;
                        padding: 30px;
                        margin: 20px 0;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    }
                    .form-group {
                        margin: 20px 0;
                    }
                    label {
                        display: block;
                        margin-bottom: 10px;
                        font-weight: bold;
                        color: #333;
                    }
                    input[type="file"] {
                        width: 100%;
                        padding: 15px;
                        border: 2px dashed #667eea;
                        border-radius: 10px;
                        background: #f5f5f5;
                        box-sizing: border-box;
                        font-size: 16px;
                        min-height: 50px;
                        display: block;
                        visibility: visible;
                        opacity: 1;
                        position: relative;
                        z-index: 1;
                        cursor: pointer;
                        -webkit-tap-highlight-color: rgba(102, 126, 234, 0.3);
                    }
                    input[type="file"]:active {
                        background: #e8e8e8;
                    }
                    @media (max-width: 768px) {
                        .header h1 {
                            font-size: 1.8em;
                        }
                        body {
                            padding: 10px;
                        }
                        .container {
                            max-width: 100%;
                        }
                        input[type="file"] {
                            font-size: 18px;
                            padding: 25px 20px;
                            min-height: 70px;
                            width: 100%;
                            display: block !important;
                            visibility: visible !important;
                            opacity: 1 !important;
                            touch-action: manipulation;
                            -webkit-appearance: none;
                        }
                        .form-group {
                            margin: 30px 0;
                        }
                        label {
                            font-size: 1.1em;
                            margin-bottom: 15px;
                        }
                        button {
                            width: 100%;
                            padding: 20px;
                            font-size: 1.2em;
                        }
                        .card {
                            padding: 20px;
                        }
                    }
                    button {
                        background: linear-gradient(135deg, #667eea, #764ba2);
                        color: white;
                        border: none;
                        padding: 15px 40px;
                        border-radius: 50px;
                        font-size: 1.1em;
                        cursor: pointer;
                        margin: 10px 5px;
                    }
                    button:hover {
                        transform: scale(1.05);
                        box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
                    }
                    .back-button {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 50px;
                        text-decoration: none;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                    .back-button:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }
                    .message {
                        padding: 15px;
                        border-radius: 10px;
                        margin: 20px 0;
                    }
                    .success {
                        background: #d4edda;
                        color: #155724;
                        border: 1px solid #c3e6cb;
                    }
                    .error {
                        background: #f8d7da;
                        color: #721c24;
                        border: 1px solid #f5c6cb;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <a href="/" class="back-button">← Back to Home</a>
                    <div class="header">
                        <h1>📂 Decompress Files</h1>
                    </div>
                    <div class="card">
                        <form action="/api/decompress" method="post" enctype="multipart/form-data" autocomplete="off">
                            <div class="form-group">
                                <label for="file">Select Compressed File (.zip or .gz):</label>
                                <input type="file" id="file" name="file" accept=".zip,.gz" required autocomplete="off">
                            </div>
                            <button type="submit">Decompress File</button>
                        </form>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    // Generate compression result page with statistics
    private static String generateCompressResultPage(String originalFilename, 
            long originalSize, long compressedSize, double compressionRatio,
            byte[] compressedData, String compressedFilename) {
        
        String originalSizeStr = formatFileSize(originalSize);
        String compressedSizeStr = formatFileSize(compressedSize);
        String spaceSaved = formatFileSize(originalSize - compressedSize);
        String ratioStr = String.format("%.2f", compressionRatio);
        
        // Encode file data as base64 for download
        String base64Data = java.util.Base64.getEncoder().encodeToString(compressedData);
        
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Compression Complete</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        padding: 20px 0;
                    }
                    .header h1 {
                        font-size: 2.5em;
                        margin: 0;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 20px;
                        padding: 30px;
                        margin: 20px 0;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    }
                    .success-icon {
                        text-align: center;
                        font-size: 4em;
                        margin: 20px 0;
                    }
                    .stats {
                        background: #e8f4f8;
                        border: 1px solid #b3d9e6;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .stat-item {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #b3d9e6;
                    }
                    .stat-item:last-child {
                        border-bottom: none;
                    }
                    .stat-label {
                        font-weight: bold;
                        color: #333;
                    }
                    .stat-value {
                        color: #667eea;
                        font-weight: bold;
                    }
                    .compression-ratio {
                        font-size: 2em;
                        color: #48bb78;
                        text-align: center;
                        padding: 20px;
                        background: #d4edda;
                        border-radius: 10px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .download-button {
                        display: block;
                        width: 100%;
                        background: linear-gradient(135deg, #48bb78, #38a169);
                        color: white;
                        border: none;
                        padding: 20px;
                        border-radius: 50px;
                        font-size: 1.2em;
                        cursor: pointer;
                        text-align: center;
                        text-decoration: none;
                        margin: 20px 0;
                    }
                    .download-button:hover {
                        transform: scale(1.02);
                        box-shadow: 0 5px 20px rgba(72, 187, 120, 0.4);
                    }
                    .back-button {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 50px;
                        text-decoration: none;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                    .back-button:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <a href="/compress" class="back-button">← Compress Another File</a>
                    <div class="header">
                        <h1>✅ Compression Complete!</h1>
                    </div>
                    <div class="card">
                        <div class="success-icon">🎉</div>
                        <div class="stats">
                            <h3 style="margin-top: 0;">Compression Statistics</h3>
                            <div class="stat-item">
                                <span class="stat-label">Original File:</span>
                                <span class="stat-value">""");
        html.append(originalFilename);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Original Size:</span><span class=\"stat-value\">");
        html.append(originalSizeStr);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Compressed Size:</span><span class=\"stat-value\">");
        html.append(compressedSizeStr);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Space Saved:</span><span class=\"stat-value\">");
        html.append(spaceSaved);
        html.append("</span></div><div class=\"compression-ratio\">Compression Ratio: ");
        html.append(ratioStr);
        html.append("%</div></div><a href=\"data:application/gzip;base64,");
        html.append(base64Data);
        html.append("\" download=\"");
        html.append(compressedFilename);
        html.append("\" class=\"download-button\">⬇️ Download Compressed File</a></div></div></body></html>");
        return html.toString();
    }
    
    // Generate decompress result page
    private static String generateDecompressResultPage(String compressedFilename,
            long compressedSize, long decompressedSize, String outputFilename,
            byte[] decompressedData) {
        
        String compressedSizeStr = formatFileSize(compressedSize);
        String decompressedSizeStr = formatFileSize(decompressedSize);
        
        // Encode file data as base64 for download
        String base64Data = java.util.Base64.getEncoder().encodeToString(decompressedData);
        
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Decompression Complete</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        padding: 20px 0;
                    }
                    .header h1 {
                        font-size: 2.5em;
                        margin: 0;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 20px;
                        padding: 30px;
                        margin: 20px 0;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    }
                    .success-icon {
                        text-align: center;
                        font-size: 4em;
                        margin: 20px 0;
                    }
                    .stats {
                        background: #e8f4f8;
                        border: 1px solid #b3d9e6;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .stat-item {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #b3d9e6;
                    }
                    .stat-item:last-child {
                        border-bottom: none;
                    }
                    .stat-label {
                        font-weight: bold;
                        color: #333;
                    }
                    .stat-value {
                        color: #667eea;
                        font-weight: bold;
                    }
                    .download-button {
                        display: block;
                        width: 100%;
                        background: linear-gradient(135deg, #48bb78, #38a169);
                        color: white;
                        border: none;
                        padding: 20px;
                        border-radius: 50px;
                        font-size: 1.2em;
                        cursor: pointer;
                        text-align: center;
                        text-decoration: none;
                        margin: 20px 0;
                    }
                    .download-button:hover {
                        transform: scale(1.02);
                        box-shadow: 0 5px 20px rgba(72, 187, 120, 0.4);
                    }
                    .back-button {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 50px;
                        text-decoration: none;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                    .back-button:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <a href="/decompress" class="back-button">← Decompress Another File</a>
                    <div class="header">
                        <h1>✅ Decompression Complete!</h1>
                    </div>
                    <div class="card">
                        <div class="success-icon">🎉</div>
                        <div class="stats">
                            <h3 style="margin-top: 0;">Decompression Statistics</h3>
                            <div class="stat-item">
                                <span class="stat-label">Compressed File:</span>
                                <span class="stat-value">""");
        html.append(compressedFilename);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Compressed Size:</span><span class=\"stat-value\">");
        html.append(compressedSizeStr);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Decompressed Size:</span><span class=\"stat-value\">");
        html.append(decompressedSizeStr);
        html.append("</span></div><div class=\"stat-item\"><span class=\"stat-label\">Output File:</span><span class=\"stat-value\">");
        html.append(outputFilename);
        html.append("</span></div></div><a href=\"data:application/octet-stream;base64,");
        html.append(base64Data);
        html.append("\" download=\"");
        html.append(outputFilename);
        html.append("\" class=\"download-button\">⬇️ Download Decompressed File</a></div></div></body></html>");
        return html.toString();
    }
    
    // Generate error page
    private static String generateErrorPage(String errorMessage) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Error</title>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0;
                        padding: 20px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 20px;
                        padding: 30px;
                        margin: 20px 0;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    }
                    .error {
                        color: #e53e3e;
                        padding: 15px;
                        background: #fed7d7;
                        border-radius: 10px;
                        margin: 20px 0;
                    }
                    .back-button {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 50px;
                        text-decoration: none;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <a href="/compress" class="back-button">← Back</a>
                    <div class="card">
                        <div class="error">
                            <strong>Error:</strong> """);
        html.append(errorMessage);
        html.append("</div></div></div></body></html>");
        return html.toString();
    }
    
    // Format file size
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    // Helper methods
    private static String extractBoundary(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.contains("boundary=")) {
            return "--" + contentType.substring(contentType.indexOf("boundary=") + 9);
        }
        return null;
    }
    
    private static Map<String, byte[]> parseMultipart(byte[] data, String boundary) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        byte[] boundaryBytes = boundary.getBytes("ISO-8859-1");
        
        // Find boundary positions
        int start = 0;
        while (true) {
            int boundaryPos = indexOf(data, boundaryBytes, start);
            if (boundaryPos == -1) break;
            
            // Find next boundary or end
            int nextBoundary = indexOf(data, boundaryBytes, boundaryPos + boundaryBytes.length);
            if (nextBoundary == -1) nextBoundary = data.length;
            
            // Extract part
            byte[] part = new byte[nextBoundary - boundaryPos - boundaryBytes.length];
            System.arraycopy(data, boundaryPos + boundaryBytes.length, part, 0, part.length);
            
            // Find headers (until \r\n\r\n)
            int headerEnd = indexOf(part, "\r\n\r\n".getBytes("ISO-8859-1"), 0);
            if (headerEnd > 0) {
                // Extract headers as string
                String headers = new String(part, 0, headerEnd, "ISO-8859-1");
                
                // Extract filename
                if (headers.contains("filename=\"")) {
                    int nameStart = headers.indexOf("filename=\"") + 10;
                    int nameEnd = headers.indexOf("\"", nameStart);
                    if (nameEnd > nameStart) {
                        String filename = headers.substring(nameStart, nameEnd);
                        
                        // Extract file content (skip headers and \r\n\r\n)
                        int contentStart = headerEnd + 4;
                        int contentEnd = part.length;
                        
                        // Remove trailing \r\n if present
                        if (contentEnd >= 2 && part[contentEnd - 2] == '\r' && part[contentEnd - 1] == '\n') {
                            contentEnd -= 2;
                        }
                        
                        byte[] fileData = new byte[contentEnd - contentStart];
                        System.arraycopy(part, contentStart, fileData, 0, fileData.length);
                        
                        files.put(filename, fileData);
                    }
                }
            }
            
            start = nextBoundary;
        }
        
        return files;
    }
    
    private static int indexOf(byte[] array, byte[] pattern, int start) {
        for (int i = start; i <= array.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (array[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }
    
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, 
                                    String contentType, String response) throws IOException {
        sendResponse(exchange, statusCode, contentType, response.getBytes("UTF-8"));
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, 
                                    String contentType, byte[] response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}

