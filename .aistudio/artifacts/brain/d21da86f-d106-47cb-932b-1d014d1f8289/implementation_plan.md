# TV QR Code Phone Companion & Album Sync

Build an on-screen QR code pairing system powered by an embedded local HTTP server running directly on Android TV. Scanning the QR code with any smartphone opens a web portal to paste Google Photos album links and upload high-resolution photos straight to the TV over local Wi-Fi without typing with a remote control.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The confirmed choices from Phase 1 clarifications will be implemented as follows:
> - **Phone-to-TV Communication**: The TV generates a local QR code encoding `http://<tv-local-ip>:8080`. Scanning it opens an in-browser mobile companion app served directly by the TV on the local Wi-Fi network. No cloud relay, third-party accounts, or paid services are required.
> - **Dual Input Capability**: The mobile companion webpage provides both a **Google Photos Album Link** form and a **Direct Photo Upload** multi-file picker to transfer pictures directly from phone storage to the TV's offline cache.

- **Confirmed Decision 1**: Local embedded micro-server on TV (zero external server costs, 100% private to local network, 100% free tier).
- **Confirmed Decision 2**: Mobile web companion with dual tabs: "Paste Google Photos Link" and "Upload Photos from Phone".
- **Default Visual Integration**: A dedicated "Connect Phone (QR)" card in the TV settings drawer and HUD, opening a clean 10-foot modal with QR code, step-by-step instructions, and live status pulses.

---

### 1. Overview & Core Concept

- **What It Does**: Eliminates the frustration of typing long URLs on a TV remote. The TV displays a crisp QR code with its local IP address. Scanning it opens a responsive mobile web interface on the phone where users can paste their Google Photos album link or select multiple photos to beam directly to the TV.
- **Target Persona**: Android TV users and families wanting to display Google Photos albums or personal memories on their TV without manual typing or connecting keyboards.
- **Key Value**: Zero-friction setup (under 10 seconds), 100% free tier, zero cloud middleman, and immediate offline local caching.

---

### 2. User Experience & Visual Design

#### TV Screen User Flow:
1. **Entry**: User presses "▲ Settings" or selects the "Connect Phone (QR)" action badge in the ambient HUD or settings drawer.
2. **Pairing Modal**: A full-screen dialog appears:
   - **Left Pane (50%)**: A high-contrast QR code rendered on the TV, along with the fallback local URL (e.g., `http://192.168.1.45:8080`).
   - **Right Pane (50%)**: 3 simple visual steps:
     1. Scan the QR code using your phone's camera.
     2. Paste your Google Photos shared album link or choose photos.
     3. Tap **Send to TV**.
   - **Live Connection Status**: Glowing indicator showing "Waiting for phone connection..." transitioning to "Phone connected • Receiving photos...".
3. **Automatic Handoff**: Once the phone submits an album link or uploads photos, the modal shows a success celebration checkmark, automatically closes after 2 seconds, and transitions into the slideshow with a smooth cross-fade animation.

#### Mobile Web Companion UX:
- **Responsive Dark Mode UI**: Crafted specifically for mobile Safari/Chrome with large touch targets.
- **Tab 1 — Google Photos Album**:
  - One-tap paste field with helpful instructions on how to copy a public share link from the Google Photos app (`Share > Create link`).
  - "Send Album to TV" button with instant feedback.
- **Tab 2 — Upload from Phone**:
  - Native multi-photo picker (`<input type="file" multiple accept="image/*">`).
  - Live progress bar showing upload to TV storage.

---

### 3. Key Product Decisions & Trade-Offs

- **Local Embedded HTTP Server vs. Cloud Relay Database**:
  - *Chosen Approach*: Embedded micro-server (`com.sun.net.httpserver.HttpServer` or lightweight Kotlin socket handler) running on Android TV port 8080.
  - *Why*: Requires no third-party cloud service, no external accounts, zero monthly fees, works offline within home Wi-Fi, and offers ultra-low latency for heavy image transfers.
  - *Alternative Considered*: Firebase Realtime Database pairing relay (rejected because it requires internet connectivity and introduces cloud payload size limits for direct image uploads).

- **Pure Canvas QR Generator vs. External Heavy Libraries**:
  - *Chosen Approach*: Lightweight QR matrix generator producing sharp Compose `Canvas` / `Bitmap` on the TV.
  - *Why*: Keeps APK lean, fast compilation, and does not require internet access to fetch external QR images.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Android TV Device                    │
│                                                        │
│  ┌─────────────────────────┐   ┌────────────────────┐  │
│  │    TvSlideshowScreen    │   │  EmbeddedHttpServer│  │
│  │   - Crossfade engine    │   │  (Port 8080)       │  │
│  │   - TV Remote D-Pad     │   │                    │  │
│  │   - QR Modal Dialog     │   │  - GET / (UI)      │  │
│  │   - Ambient HUD         │   │  - POST /api/link  │  │
│  └───────────┬─────────────┘   │  - POST /api/upload│  │
│              │                 └──────────▲─────────┘  │
│              ▼                            │            │
│  ┌─────────────────────────┐              │            │
│  │    SlideshowViewModel   │◄─────────────┘            │
│  │   - CloudSyncManager    │   (Link / Image events)   │
│  │   - OfflineCacheManager │                           │
│  └───────────┬─────────────┘                           │
│              │                                         │
│              ▼                                         │
│  ┌─────────────────────────┐                           │
│  │ Room Database & Storage │                           │
│  │  - cached_photos/       │                           │
│  │  - PhotoItem records    │                           │
│  └─────────────────────────┘                           │
└────────────────────────────────────────────────────────┘
                           ▲
                           │ Local Wi-Fi (HTTP)
                           ▼
┌────────────────────────────────────────────────────────┐
│                 User Smartphone Device                 │
│                                                        │
│  1. Scans TV QR Code                                   │
│  2. Opens http://<tv-ip>:8080 in mobile browser        │
│  3. Pastes Google Photos URL or uploads photos         │
│  4. Sends directly to TV over home Wi-Fi               │
└────────────────────────────────────────────────────────┘
```

#### Key Implementation Components:
1. **`TvLocalHttpServer.kt`**:
   - Detects local Wi-Fi IP address (`192.168.x.x`).
   - Serves an embedded, self-contained HTML/CSS/JS single-page web app to the phone.
   - Endpoint `POST /api/album-link`: Receives JSON `{ "url": "https://..." }`, triggers `CloudSyncManager.syncCustomUrl()`, and broadcasts progress.
   - Endpoint `POST /api/upload-photo`: Receives multipart/streamed image files, saves directly to `filesDir/cached_photos/`, and inserts into Room DB as `PhotoSource.LOCAL`.
2. **`QrCodeGenerator.kt`**:
   - Generates the QR matrix for `http://<ip>:8080`.
   - Renders cleanly at any resolution on big-screen TVs.
3. **`TvQrPairingDialog.kt`**:
   - Modern TV dialog with prominent QR code, readable instructions, and live status updates.
   - Accessible via remote control (`Settings > Connect Phone via QR` or shortcut).
