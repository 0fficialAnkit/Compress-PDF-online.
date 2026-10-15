# PDFPulse — All-in-One Professional PDF Toolkit

A modern, high-performance, privacy-centric web platform for all common PDF operations. Built with **Java 21**, **Spring Boot 3.5**, **Apache PDFBox 3.0**, **Apache POI**, and **Vanilla HTML/CSS/JavaScript**, with zero external cloud dependencies required.

- **Branding:** PDFPulse Pro Studio
- **Backend:** Java 21 + Spring Boot 3.5.4
- **PDF Engine:** Apache PDFBox 3.0.3 (in-memory processing)
- **Document Conversion:** Apache POI 5.3.0 + Commonmark 0.24.0
- **OCR Engine:** Native Tesseract CLI integration with fallback text layer extraction
- **AI Intelligence:** Dual-mode (extensible with `GEMINI_API_KEY` or built-in offline NLP engine)
- **Frontend:** Responsive Thymeleaf + Vanilla CSS design system + Vanilla JavaScript
- **Local URL:** `http://localhost:8080`

---

## 🌟 Tool Catalog & Implementation Status

| Category | Tool | Description | Status |
|---|---|---|---|
| **🗜️ Compress & Repair** | **Compress PDF** | Original image downsampling & quality tuning (Low, Medium, High) | ✅ Fully Implemented |
| | **Flatten PDF** | Flatten interactive forms and annotations into page graphics | ✅ Fully Implemented |
| **📁 PDF Organization** | **Merge PDF** | Combine multiple PDF documents in user-defined order | ✅ Fully Implemented |
| | **Split PDF** | Extract page ranges or split all pages into a ZIP archive | ✅ Fully Implemented |
| | **Rotate PDF** | Rotate pages 90°, 180°, or 270° permanently | ✅ Fully Implemented |
| | **Remove Pages** | Delete selected pages or page ranges | ✅ Fully Implemented |
| | **Crop Margins** | Trim document margins in points for optimal reading | ✅ Fully Implemented |
| **🔄 Convert from PDF** | **PDF to Word (DOCX)** | Convert PDF to editable Word document via Apache POI | ✅ Fully Implemented |
| | **PDF to Excel (XLSX)** | Extract tabular data into structured spreadsheet | ✅ Fully Implemented |
| | **PDF to JPG / PNG** | Render pages at 150/300 DPI packed in ZIP or single image | ✅ Fully Implemented |
| | **PDF to Text** | Extract all readable text as plain-text `.txt` | ✅ Fully Implemented |
| **📄 Convert to PDF** | **Images to PDF** | Combine JPG, PNG, and WebP images into a single PDF | ✅ Fully Implemented |
| | **Text to PDF** | Format plain text into paginated PDF with margins | ✅ Fully Implemented |
| | **Markdown to PDF** | Parse Markdown headings, lists, code, and generate PDF | ✅ Fully Implemented |
| **✏️ Edit & Security** | **Watermark PDF** | Overlay semi-transparent text with customizable angle & opacity | ✅ Fully Implemented |
| | **Add Page Numbers** | Stamp page numbers with flexible formats and positions | ✅ Fully Implemented |
| | **Protect PDF** | Encrypt PDF with standard 128-bit AES password protection | ✅ Fully Implemented |
| | **Unlock PDF** | Decrypt protected PDFs with the authorized password | ✅ Fully Implemented |
| | **Redact PDF** | Permanently blackout sensitive keywords or confidential areas | ✅ Fully Implemented |
| **🔍 OCR & Extract** | **OCR Searchable PDF** | Recognize scanned text via Tesseract and generate indexed PDF | ✅ Fully Implemented |
| | **Extract Images** | Extract all embedded raster images into a ZIP archive | ✅ Fully Implemented |
| | **Inspect Metadata** | View properties, author, page count, dimensions, encryption | ✅ Fully Implemented |
| | **Remove Metadata** | Sanitize and scrub author, software signature, and timestamps | ✅ Fully Implemented |
| **🤖 AI Assistant** | **AI Summarizer** | Executive summary & bulleted key takeaways | ✅ Fully Implemented |
| | **Chat with PDF** | Ask questions grounded in document context | ✅ Fully Implemented |
| | **Extract Action Items** | Detect deadlines, action items, and statistics | ✅ Fully Implemented |

---

## 🔒 Privacy & Architecture Principles

1. **In-Memory Holding Area:** All operations take place in server memory. Files are assigned a random UUID token and held only as long as necessary for download.
2. **Scheduled Auto-Purge:** A daemon background task runs every 5 minutes and permanently evicts any stored files older than 30 minutes.
3. **No Tracking / No Watermarks:** The platform never injects watermarks or tracks document contents.
4. **Offline Capability:** Core document operations and local NLP intelligence run completely offline without calling external APIs.

---

## 🚀 Getting Started

### Prerequisites

1. **Java 21 or newer (JDK)**
   ```bash
   java -version
   ```
2. **Maven 3.9+**
   ```bash
   mvn -version
   ```
3. *(Optional for OCR)* **Tesseract**
   - macOS: `brew install tesseract`
   - Linux: `sudo apt install tesseract-ocr`

---

### Running Locally

1. **Clone and enter folder:**
   ```bash
   git clone https://github.com/0fficialAnkit/Compress-PDF-online..git
   cd Compress-PDF-online.
   ```

2. **Build and test:**
   ```bash
   mvn clean test
   ```

3. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the platform:**
   Open your browser to:
   ```
   http://localhost:8080
   ```

---

## 🐳 Docker Deployment

Build and run using the included multi-stage Dockerfile:

```bash
docker build -t pdfpulse-app .
docker run -p 8080:8080 -e PORT=8080 pdfpulse-app
```

---

## ⚙️ Environment Variables

See `.env.example` for reference:

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Port for the HTTP server |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `50MB` | Max upload size per file |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `50MB` | Max overall multipart request size |
| `GEMINI_API_KEY` | *(empty)* | Optional API key for Google Gemini AI features (if unset, offline NLP engine is used) |

---

## 📡 Key API Endpoints

- `POST /api/compress`: Original preserved endpoint for PDF compression (`file`, `level`)
- `GET /api/download/{id}`: Secure token-based file download
- `POST /api/tools/merge`: Combine multiple PDFs (`files`)
- `POST /api/tools/split`: Split document (`file`, `mode`, `range`)
- `POST /api/tools/rotate`: Rotate pages (`file`, `degrees`, `range`)
- `POST /api/tools/watermark`: Apply watermark (`file`, `text`, `opacity`, `rotation`)
- `POST /api/tools/protect`: Encrypt document (`file`, `password`)
- `POST /api/tools/unlock`: Decrypt document (`file`, `password`)
- `POST /api/tools/pdf-to-word`: Convert to Word DOCX (`file`)
- `POST /api/tools/pdf-to-img`: Convert to JPG/PNG images (`file`, `format`, `dpi`)
- `POST /api/tools/ocr`: OCR processing (`file`, `lang`)
- `POST /api/tools/ai-summarize`: AI summary (`file`)
- `POST /api/tools/ai-chat`: Grounded Q&A (`file`, `question`)
- `POST /api/tools/inspect`: Document metadata extraction (`file`)

---

## 🧪 Testing

Run the full automated test suite:

```bash
mvn test
```
All unit tests cover merge, split, compression, rotation, watermarking, encryption/decryption, metadata extraction, markdown conversion, and DOCX generation.
