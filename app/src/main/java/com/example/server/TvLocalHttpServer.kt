package com.example.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Lightweight, robust Android-native HTTP server using core java.net.ServerSocket.
 * Zero external library dependencies, 100% compatible with Android ART runtime.
 */
class TvLocalHttpServer(
    private val context: Context,
    private val onAlbumLinkReceived: (String) -> Unit,
    private val onPhotoUploaded: (File, String) -> Unit,
    private val onPhoneConnected: () -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val threadPool = Executors.newFixedThreadPool(4)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val port = 8080

    val localIpAddress: String
        get() = getDeviceIpAddress()

    val serverUrl: String
        get() = "http://${localIpAddress}:$port"

    fun start() {
        if (serverSocket != null && !serverSocket!!.isClosed) return

        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
            serverSocket = socket

            serverJob = scope.launch {
                Log.d(TAG, "Lumina TV Server listening at $serverUrl")
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        threadPool.execute {
                            handleClientConnection(client)
                        }
                    } catch (e: Exception) {
                        if (socket.isClosed) break
                        Log.e(TAG, "Socket accept error", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ServerSocket on port $port", e)
        }
    }

    fun stop() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ServerSocket", e)
        }
    }

    private fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 15000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            // 1. Read HTTP request line and headers
            val headerBytes = readHeaderBytes(input) ?: run {
                socket.close()
                return
            }

            val headerText = String(headerBytes, Charsets.ISO_8859_1)
            val headerLines = headerText.split("\r\n")
            if (headerLines.isEmpty()) {
                socket.close()
                return
            }

            val requestLine = headerLines[0].split(" ")
            if (requestLine.size < 2) {
                socket.close()
                return
            }

            val method = requestLine[0].uppercase()
            val path = requestLine[1].substringBefore("?")

            val headers = mutableMapOf<String, String>()
            for (i in 1 until headerLines.size) {
                val line = headerLines[i]
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val contentType = headers["content-type"] ?: ""

            // Handle routes
            when {
                method == "OPTIONS" -> {
                    sendResponse(output, 204, "No Content", "text/plain", ByteArray(0))
                }

                method == "GET" && (path == "/" || path == "/index.html") -> {
                    onPhoneConnected()
                    val html = getCompanionHtmlPage()
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    sendResponse(output, 200, "OK", "text/html; charset=UTF-8", bytes)
                }

                method == "GET" && path == "/api/status" -> {
                    val json = JSONObject().apply {
                        put("app", "Lumina TV Photo Frame")
                        put("status", "ready")
                        put("tvIp", localIpAddress)
                    }.toString()
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    sendResponse(output, 200, "OK", "application/json; charset=UTF-8", bytes)
                }

                method == "POST" && path == "/api/album-link" -> {
                    val bodyBytes = readExactBytes(input, contentLength)
                    val bodyJson = JSONObject(String(bodyBytes, Charsets.UTF_8))
                    val url = bodyJson.optString("url", "").trim()

                    if (url.isNotBlank()) {
                        scope.launch {
                            onAlbumLinkReceived(url)
                        }
                        val responseJson = JSONObject().apply {
                            put("success", true)
                            put("message", "Album link received! TV is syncing photos...")
                        }.toString().toByteArray(Charsets.UTF_8)
                        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", responseJson)
                    } else {
                        val errorJson = "{\"success\": false, \"message\": \"Empty URL\"}".toByteArray(Charsets.UTF_8)
                        sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", errorJson)
                    }
                }

                method == "POST" && path == "/api/upload-photo" -> {
                    val targetDir = File(context.filesDir, "cached_photos").apply {
                        if (!exists()) mkdirs()
                    }

                    if (contentType.contains("multipart/form-data")) {
                        val boundary = contentType.substringAfter("boundary=").trim()
                        val bodyBytes = readExactBytes(input, contentLength)
                        val savedFiles = parseAndSaveMultipart(bodyBytes, boundary, targetDir)
                        for (file in savedFiles) {
                            scope.launch {
                                onPhotoUploaded(file, file.nameWithoutExtension)
                            }
                        }
                        val responseJson = JSONObject().apply {
                            put("success", true)
                            put("count", savedFiles.size)
                            put("message", "Uploaded ${savedFiles.size} photos to TV!")
                        }.toString().toByteArray(Charsets.UTF_8)
                        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", responseJson)
                    } else {
                        val bodyBytes = readExactBytes(input, contentLength)
                        val fileName = "phone_photo_${System.currentTimeMillis()}.jpg"
                        val file = File(targetDir, fileName)
                        file.outputStream().use { it.write(bodyBytes) }
                        scope.launch {
                            onPhotoUploaded(file, "Phone Photo")
                        }
                        val responseJson = "{\"success\": true, \"message\": \"Photo uploaded to TV!\"}".toByteArray(Charsets.UTF_8)
                        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", responseJson)
                    }
                }

                else -> {
                    val notFound = "Not Found".toByteArray(Charsets.UTF_8)
                    sendResponse(output, 404, "Not Found", "text/plain", notFound)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client request", e)
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun readHeaderBytes(input: InputStream): ByteArray? {
        val buffer = ByteArrayOutputStream()
        var state = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            buffer.write(b)

            when (state) {
                0 -> if (b == '\r'.code) state = 1 else state = 0
                1 -> if (b == '\n'.code) state = 2 else state = 0
                2 -> if (b == '\r'.code) state = 3 else state = 0
                3 -> if (b == '\n'.code) return buffer.toByteArray() else state = 0
            }
            if (buffer.size() > 16384) return null // header too large
        }
    }

    private fun readExactBytes(input: InputStream, length: Int): ByteArray {
        val result = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(result, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return if (totalRead == length) result else result.copyOf(totalRead)
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val header = StringBuilder().apply {
            append("HTTP/1.1 $statusCode $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }.toString()

        output.write(header.toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }

    private fun parseAndSaveMultipart(bytes: ByteArray, boundary: String, outputDir: File): List<File> {
        val savedFiles = mutableListOf<File>()
        val boundaryBytes = ("--$boundary").toByteArray(Charsets.ISO_8859_1)

        var pos = 0
        while (pos < bytes.size) {
            val nextBoundary = indexOf(bytes, boundaryBytes, pos)
            if (nextBoundary == -1) break

            val partStart = pos
            val partEnd = nextBoundary
            pos = nextBoundary + boundaryBytes.size

            if (partStart < partEnd) {
                val headerEnd = indexOf(bytes, "\r\n\r\n".toByteArray(Charsets.ISO_8859_1), partStart)
                if (headerEnd in partStart until partEnd) {
                    val headers = String(bytes, partStart, headerEnd - partStart, Charsets.ISO_8859_1)
                    if (headers.contains("filename=")) {
                        val originalName = headers.substringAfter("filename=\"").substringBefore("\"")
                        val fileDataStart = headerEnd + 4
                        var fileDataEnd = partEnd
                        if (fileDataEnd >= 2 && bytes[fileDataEnd - 2] == '\r'.code.toByte() && bytes[fileDataEnd - 1] == '\n'.code.toByte()) {
                            fileDataEnd -= 2
                        }
                        if (fileDataEnd > fileDataStart) {
                            val cleanName = originalName.ifBlank { "photo_${System.currentTimeMillis()}.jpg" }
                            val targetFile = File(outputDir, "phone_${System.currentTimeMillis()}_$cleanName")
                            targetFile.outputStream().use { out ->
                                out.write(bytes, fileDataStart, fileDataEnd - fileDataStart)
                            }
                            savedFiles.add(targetFile)
                        }
                    }
                }
            }
        }
        return savedFiles
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        for (i in fromIndex..(source.size - target.size)) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving IP address", e)
        }
        return "127.0.0.1"
    }

    private fun getCompanionHtmlPage(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Lumina TV • Send Photos</title>
    <style>
        :root {
            --bg-dark: #0B0F19;
            --surface: #151D2E;
            --surface-elevated: #1E293B;
            --accent-cyan: #38BDF8;
            --accent-blue: #2563EB;
            --accent-green: #34D399;
            --text-primary: #F8FAFC;
            --text-secondary: #94A3B8;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg-dark); color: var(--text-primary); padding: 20px 16px; min-height: 100vh; display: flex; flex-direction: column; }
        .header { text-align: center; margin-bottom: 24px; padding-top: 8px; }
        .header h1 { font-size: 22px; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; justify-content: center; gap: 8px; }
        .header p { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
        .badge { display: inline-flex; align-items: center; gap: 6px; background: rgba(56, 189, 248, 0.15); color: var(--accent-cyan); font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 20px; margin-top: 10px; border: 1px solid rgba(56, 189, 248, 0.3); }
        .tabs { display: flex; background: var(--surface); border-radius: 12px; padding: 4px; margin-bottom: 20px; border: 1px solid rgba(255, 255, 255, 0.08); }
        .tab-btn { flex: 1; padding: 12px; border: none; background: transparent; color: var(--text-secondary); font-size: 14px; font-weight: 600; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
        .tab-btn.active { background: var(--accent-cyan); color: #000; }
        .card { background: var(--surface); border-radius: 16px; padding: 20px; border: 1px solid rgba(255, 255, 255, 0.08); margin-bottom: 16px; }
        .card h2 { font-size: 16px; font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
        .steps { background: var(--surface-elevated); border-radius: 10px; padding: 12px 14px; margin-bottom: 16px; font-size: 12px; color: var(--text-secondary); line-height: 1.6; }
        .steps b { color: var(--text-primary); }
        .input-group { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
        input[type="text"] { width: 100%; padding: 14px 16px; border-radius: 10px; border: 1px solid rgba(255, 255, 255, 0.15); background: var(--bg-dark); color: #FFF; font-size: 14px; outline: none; }
        input[type="text"]:focus { border-color: var(--accent-cyan); }
        .btn-paste { background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.2); color: #FFF; padding: 10px 14px; border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; align-self: flex-start; display: flex; align-items: center; gap: 6px; }
        .btn-primary { width: 100%; padding: 14px; border-radius: 12px; border: none; background: linear-gradient(135deg, var(--accent-cyan), var(--accent-blue)); color: #000; font-size: 15px; font-weight: 700; cursor: pointer; box-shadow: 0 4px 14px rgba(56, 189, 248, 0.3); transition: transform 0.1s; }
        .btn-primary:active { transform: scale(0.98); }
        .file-upload-box { border: 2px dashed rgba(56, 189, 248, 0.4); border-radius: 12px; padding: 28px 16px; text-align: center; cursor: pointer; background: rgba(56, 189, 248, 0.05); margin-bottom: 16px; }
        .file-upload-box svg { fill: var(--accent-cyan); width: 36px; height: 36px; margin-bottom: 8px; }
        .file-upload-box p { font-size: 13px; color: var(--text-secondary); }
        .file-upload-box b { color: var(--accent-cyan); font-weight: 600; }
        #fileInput { display: none; }
        .status-toast { display: none; margin-top: 14px; padding: 12px 14px; border-radius: 10px; font-size: 13px; font-weight: 500; text-align: center; }
        .status-toast.success { display: block; background: rgba(52, 211, 153, 0.15); color: var(--accent-green); border: 1px solid var(--accent-green); }
        .status-toast.error { display: block; background: rgba(239, 68, 68, 0.15); color: #EF4444; border: 1px solid #EF4444; }
        .status-toast.loading { display: block; background: rgba(56, 189, 248, 0.15); color: var(--accent-cyan); border: 1px solid var(--accent-cyan); }
        .preview-list { display: flex; gap: 8px; overflow-x: auto; margin-bottom: 14px; padding-bottom: 6px; }
        .preview-thumb { width: 64px; height: 64px; border-radius: 8px; object-fit: cover; border: 1px solid rgba(255, 255, 255, 0.2); }
    </style>
</head>
<body>
    <div class="header">
        <h1>📺 Lumina TV Companion</h1>
        <p>Send Google Photos or beam photos from your phone</p>
        <div class="badge">● Connected to Android TV</div>
    </div>

    <div class="tabs">
        <button class="tab-btn active" onclick="switchTab('google-photos')">Google Photos</button>
        <button class="tab-btn" onclick="switchTab('direct-upload')">Upload Photos</button>
    </div>

    <!-- TAB 1: GOOGLE PHOTOS -->
    <div id="tab-google-photos" class="card">
        <h2>Sync Google Photos Album</h2>
        <div class="steps">
            1. Open <b>Google Photos</b> app on your phone.<br>
            2. Tap your Album &gt; <b>Share</b> &gt; <b>Create link</b>.<br>
            3. Paste the link below and tap <b>Send to TV</b>.
        </div>
        <div class="input-group">
            <input type="text" id="albumUrl" placeholder="https://photos.app.goo.gl/... or album link">
            <button class="btn-paste" onclick="pasteFromClipboard()">📋 Paste from Clipboard</button>
        </div>
        <button class="btn-primary" onclick="sendAlbumLink()">Send to TV &amp; Start Slideshow</button>
        <div id="statusAlbum" class="status-toast"></div>
    </div>

    <!-- TAB 2: DIRECT UPLOAD -->
    <div id="tab-direct-upload" class="card" style="display: none;">
        <h2>Upload Photos to TV</h2>
        <p style="font-size: 12px; color: var(--text-secondary); margin-bottom: 14px;">Selected photos are transferred directly over Wi-Fi and cached on your TV.</p>
        
        <div class="file-upload-box" onclick="document.getElementById('fileInput').click()">
            <svg viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            <p><b>Choose Photos from Phone</b></p>
            <p id="fileCountLabel">Supports multiple images</p>
        </div>
        <input type="file" id="fileInput" multiple accept="image/*" onchange="handleFilesSelected(event)">
        <div id="previewContainer" class="preview-list"></div>
        <button class="btn-primary" id="btnUpload" onclick="uploadSelectedPhotos()" style="display: none;">Upload Selected Photos to TV</button>
        <div id="statusUpload" class="status-toast"></div>
    </div>

    <script>
        function switchTab(tabId) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            if (tabId === 'google-photos') {
                event.target.classList.add('active');
                document.getElementById('tab-google-photos').style.display = 'block';
                document.getElementById('tab-direct-upload').style.display = 'none';
            } else {
                event.target.classList.add('active');
                document.getElementById('tab-google-photos').style.display = 'none';
                document.getElementById('tab-direct-upload').style.display = 'block';
            }
        }

        async function pasteFromClipboard() {
            try {
                if (navigator.clipboard && navigator.clipboard.readText) {
                    const text = await navigator.clipboard.readText();
                    if (text) {
                        document.getElementById('albumUrl').value = text;
                    }
                }
            } catch (e) {
                alert('Please tap and hold the input box to paste your link.');
            }
        }

        async function sendAlbumLink() {
            const url = document.getElementById('albumUrl').value.trim();
            const statusEl = document.getElementById('statusAlbum');
            if (!url) {
                statusEl.className = 'status-toast error';
                statusEl.innerText = 'Please paste a valid Google Photos album link.';
                return;
            }

            statusEl.className = 'status-toast loading';
            statusEl.innerText = 'Sending album link to TV...';

            try {
                const response = await fetch('/api/album-link', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ url: url })
                });
                const data = await response.json();
                if (data.success) {
                    statusEl.className = 'status-toast success';
                    statusEl.innerText = '✓ ' + data.message;
                    document.getElementById('albumUrl').value = '';
                } else {
                    statusEl.className = 'status-toast error';
                    statusEl.innerText = data.message || 'Failed to send album link.';
                }
            } catch (err) {
                statusEl.className = 'status-toast error';
                statusEl.innerText = 'Error connecting to TV. Ensure your phone is on the same Wi-Fi.';
            }
        }

        let selectedFiles = [];
        function handleFilesSelected(event) {
            selectedFiles = Array.from(event.target.files);
            const label = document.getElementById('fileCountLabel');
            const preview = document.getElementById('previewContainer');
            const uploadBtn = document.getElementById('btnUpload');
            preview.innerHTML = '';

            if (selectedFiles.length > 0) {
                label.innerText = selectedFiles.length + ' photo(s) selected';
                uploadBtn.style.display = 'block';
                uploadBtn.innerText = 'Upload ' + selectedFiles.length + ' Photo(s) to TV';

                selectedFiles.slice(0, 8).forEach(file => {
                    const img = document.createElement('img');
                    img.className = 'preview-thumb';
                    img.src = URL.createObjectURL(file);
                    preview.appendChild(img);
                });
            } else {
                label.innerText = 'Supports multiple images';
                uploadBtn.style.display = 'none';
            }
        }

        async function uploadSelectedPhotos() {
            if (!selectedFiles.length) return;
            const statusEl = document.getElementById('statusUpload');
            statusEl.className = 'status-toast loading';
            statusEl.innerText = 'Uploading ' + selectedFiles.length + ' photos to TV...';

            const formData = new FormData();
            selectedFiles.forEach((file, idx) => {
                formData.append('photo_' + idx, file, file.name);
            });

            try {
                const response = await fetch('/api/upload-photo', {
                    method: 'POST',
                    body: formData
                });
                const data = await response.json();
                if (data.success) {
                    statusEl.className = 'status-toast success';
                    statusEl.innerText = '✓ ' + data.message;
                    document.getElementById('fileInput').value = '';
                    document.getElementById('previewContainer').innerHTML = '';
                    document.getElementById('btnUpload').style.display = 'none';
                    document.getElementById('fileCountLabel').innerText = 'Supports multiple images';
                    selectedFiles = [];
                } else {
                    statusEl.className = 'status-toast error';
                    statusEl.innerText = data.message || 'Upload failed.';
                }
            } catch (err) {
                statusEl.className = 'status-toast error';
                statusEl.innerText = 'Network error while uploading to TV.';
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    companion object {
        private const val TAG = "TvLocalHttpServer"
    }
}
