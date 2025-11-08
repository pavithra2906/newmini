# File Compression Utility

A web-based file compression and decompression utility built with pure Java. This application provides a simple interface to compress files using GZIP or ZIP formats, and decompress previously compressed files.

## Features

- **Compress Files**: Compress single files using GZIP or ZIP format
- **Decompress Files**: Decompress GZIP (.gz) and ZIP (.zip) files
- **Web Interface**: Accessible via web browser on any device
- **Pure Java**: Server-side rendering with no JavaScript required
- **Docker Support**: Easy deployment using Docker and Docker Compose

## Quick Start

### Using Docker (Recommended)

1. **Build and run the container:**
   ```bash
   docker-compose up -d --build
   ```

2. **Access the application:**
   - Open your browser and navigate to: `http://localhost:8080`

3. **Stop the container:**
   ```bash
   docker-compose down
   ```

### Manual Setup

1. **Prerequisites:**
   - Java 17 or higher
   - Java Development Kit (JDK)

2. **Compile the application:**
   ```bash
   javac JavaWebServer.java FileCompressor.java CompressionResult.java
   ```

3. **Run the server:**
   ```bash
   java JavaWebServer
   ```

4. **Access the application:**
   - Open your browser and navigate to: `http://localhost:8080`

## Usage

### Compress a File

1. Navigate to the Compress page
2. Click "Choose File" and select the file you want to compress
3. Select compression format (GZIP or ZIP)
4. Click "Compress"
5. Download the compressed file

### Decompress a File

1. Navigate to the Decompress page
2. Click "Choose File" and select a compressed file (.gz or .zip)
3. Click "Decompress"
4. Download the decompressed file

## Project Structure

```
.
├── JavaWebServer.java      # Main web server and HTTP handlers
├── FileCompressor.java     # Compression/decompression logic
├── CompressionResult.java  # Result object for compression operations
├── Dockerfile              # Docker image configuration
├── docker-compose.yml      # Docker Compose configuration
└── README.md              # This file

```

## Technical Details

- **Language**: Java 17
- **Web Server**: Java HTTP Server (com.sun.net.httpserver)
- **Compression Formats**: GZIP, ZIP
- **Port**: 8080
- **Network Binding**: 0.0.0.0 (accessible from network)

## Mobile Access

The server binds to all network interfaces, making it accessible from mobile devices on the same network:

1. Connect your mobile device to the same Wi-Fi network
2. Find your computer's IP address (displayed in server logs)
3. Open `http://<your-ip>:8080` in your mobile browser

## License

This project is provided as-is for educational and personal use.

