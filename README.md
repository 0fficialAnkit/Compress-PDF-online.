# PDF Compressor

A small Spring Boot web app that compresses PDF files by re-encoding
their embedded images (JPEG re-compression + optional downscaling),
using Apache PDFBox. Text, fonts, and vector graphics are left
untouched — only raster images are shrunk — so the result stays
readable while the file size drops.

- **Backend:** Java 21 + Spring Boot
- **Compression engine:** Apache PDFBox (in-process, no external tools needed)
- **Frontend:** Thymeleaf + plain HTML/CSS/JavaScript
- **Runs at:** http://localhost:8080

---

## Features

- Drag-and-drop or click-to-browse PDF upload
- Three compression levels: Low, Medium, High
- Before/after size comparison and % saved
- One-click download of the compressed file
- No data persisted to disk — everything happens in memory and is
  cleared automatically after ~30 minutes

---

## How it works

1. You choose a PDF and a compression level in the browser, then click
   **Compress**.
2. The file is sent to the Spring Boot server (`POST /api/compress`).
3. The server scans every page for embedded images, downsamples them,
   and re-encodes them as JPEG at a quality tied to your chosen level.
   Text and vector graphics are left completely untouched.
4. The server responds with the original size, compressed size, and a
   download id.
5. Clicking **Download** hits `GET /api/download/{id}` to fetch the
   compressed file.

Nothing is written to disk — the compressed file is held in memory
just long enough for you to download it.

---

## Prerequisites

1. **Java 21 or newer (JDK)**
   Check with:
   ```bash
   java -version
   ```
2. **Maven 3.9+**
   Check with:
   ```bash
   mvn -version
   ```
3. **Git**, to clone the repository.

If you don't already have Java or Maven installed:

- **macOS:** `brew install openjdk@21 maven`
- **Linux (Debian/Ubuntu):** `sudo apt install openjdk-21-jdk maven`
- **Windows:** Download a JDK from [Adoptium](https://adoptium.net/) and
  Maven from [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi),
  then add both to your PATH.

---

## Setup & running the project

The steps are the same on every machine — clone, move into the folder,
point the terminal at your Java/Maven install, then run. Replace the
paths below with wherever Java/Maven and this project actually live on
your computer.

### 1. Clone the repository

```bash
git clone https://github.com/0fficialAnkit/Compress-PDF-online..git
```

### 2. Open a terminal in the project folder

**macOS / Linux:**
```bash
cd /Users/YOUR_USER_NAME/Downloads/Compress-PDF-online.
```

**Windows (Command Prompt / PowerShell):**
```bash
cd C:\Users\YOUR_USER_NAME\Downloads\Compress-PDF-online.
```

### 3. Point the terminal at your Java and Maven installs

If `java -version` and `mvn -version` already work in your terminal,
you can skip this step and go straight to step 4.

If not — for example, if you installed a JDK or unpacked Maven
manually instead of through a package manager — set the paths
explicitly for this terminal session:

**macOS / Linux:**
```bash
export JAVA_HOME="/path/to/your/jdk-21"
export PATH="$JAVA_HOME/bin:/path/to/apache-maven-3.9.9/bin:$PATH"
```
Example, matching a typical install layout:
```bash
export JAVA_HOME="/Users/YOUR_USER_NAME/Library/Java/JavaVirtualMachines/openjdk-21/Contents/Home"
export PATH="$JAVA_HOME/bin:/tmp/apache-maven-3.9.9/bin:$PATH"
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;C:\apache-maven-3.9.9\bin;$env:PATH"
```

You only need to do this once per terminal session — if you close the
terminal, you'll need to export these again next time (unless you add
them to your shell profile, e.g. `.zshrc` or `.bash_profile`).

### 4. Build the project (first time only)

```bash
mvn clean install
```

This downloads dependencies and compiles everything. You should see
`BUILD SUCCESS` at the end.

### 5. Run the app

```bash
mvn spring-boot:run
```

Or, if you set `JAVA_HOME`/`PATH` manually in step 3, you can be
explicit about which Maven to use:

```bash
/path/to/apache-maven-3.9.9/bin/mvn spring-boot:run
```

You should see log output ending with something like:

```
Tomcat started on port 8080
Started PdfCompressorApplication
```

### 6. Open the website

Go to:

```
http://localhost:8080
```

Upload a PDF, pick a compression level, click **Compress PDF**, and
use the download button that appears.

---

## Stopping the server

Press `Ctrl + C` in the terminal where `mvn spring-boot:run` is
running.

---

## Project structure

```
Compress-PDF-online./
├── pom.xml                          # Maven build config & dependencies
├── src/main/java/com/example/pdfcompressor/
│   ├── PdfCompressorApplication.java   # Spring Boot entry point
│   ├── config/WebConfig.java           # CORS configuration for /api
│   ├── controller/PdfController.java   # /api/compress and /api/download endpoints
│   ├── model/CompressionResponse.java  # JSON response shape
│   └── service/PdfCompressionService.java  # Core PDFBox compression logic
└── src/main/resources/
    ├── application.properties
    ├── templates/index.html         # UI
    └── static/
        ├── style.css
        └── script.js
```

---

## API reference

### `POST /api/compress`

Multipart form fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | file | yes | The PDF to compress |
| `level` | string | no | `LOW`, `MEDIUM` (default), or `HIGH` |

Response:

```json
{
  "id": "a1b2c3d4-...",
  "originalFilename": "report.pdf",
  "originalSize": 5242880,
  "compressedSize": 1048576,
  "savingsPercent": 80.0,
  "success": true,
  "message": "Compression complete."
}
```

### `GET /api/download/{id}`

Streams back the compressed PDF as `application/pdf`, using the `id`
returned by `/api/compress`.

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| `mvn: command not found` | Maven isn't installed or not on PATH | Install Maven, or set `PATH` as shown in step 3 |
| `JAVA_HOME` errors during build | Java isn't installed, or `JAVA_HOME` points to the wrong folder | Confirm with `java -version`, then re-check the path in step 3 |
| `Port 8080 already in use` | Another app is already using that port | Stop the other process, or run with a different port: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| Build fails downloading dependencies | No internet access, or a firewall/proxy is blocking Maven Central | Check your connection; corporate networks sometimes need a Maven proxy/mirror configured |
| Upload succeeds but file size barely changes | The PDF is mostly text/vector graphics with few or no embedded images | Expected — there's nothing to re-encode; try a scan-heavy or photo-heavy PDF instead |

---

## Notes

- Compressed files are held in an in-memory map, so they won't survive
  an app restart, and this won't scale across multiple instances
  without a shared store (e.g. Redis, S3, or disk + a cleanup job).
- Encrypted/password-protected PDFs aren't supported.
- Vector-only PDFs (no embedded raster images) will see little to no
  size reduction, since there's nothing to re-encode.
