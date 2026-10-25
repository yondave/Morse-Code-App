# Signal Morse: Acoustic Decoder & Tactical Optical Rx Upgrade

Comprehensive architectural blueprint to expand Signal Morse with real-time acoustic microphone Morse decoding, tap-to-aim dynamic ROI targeting with exposure lock and zoom, and a high-contrast Tactical Amber HUD aesthetic.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed from your clarification responses and will guide this upgrade:

- **Confirmed Decision 1 (Audio Rx Decoder)**: Add an acoustic microphone Morse decoder alongside the camera optical receiver. Users can toggle between **Optical (Camera Flash)** and **Acoustic (Microphone Audio)** modes, decoding Morse sound beeps from radios, whistles, or speakers using digital bandpass filtering and pulse duration detection.
- **Confirmed Decision 2 (Targeted Optical Aiming)**: Implement tap-to-aim ROI (Region of Interest) targeting on the camera viewfinder, paired with digital/optical zoom controls (1x to 5x) and an exposure lock toggle to prevent bright flashlight bursts from blowing out the camera sensor.
- **Confirmed Decision 3 (Visual Identity)**: Tactical Dark with Amber Radar styling (#FFB300 / #FF8F00 accents, dark gunmetal backgrounds #0F1216, military-grade reticles, and real-time audio/optical oscilloscope displays).

---

## 1. Overview & Core Concept

Signal Morse currently transmits Morse code via screen flashes, camera torch, audio beeps, and vibrations, while receiving Morse exclusively through camera optical luminance tracking. 

This enhancement makes Signal Morse a complete **dual-mode transceiver**:
1. **Acoustic Morse Receiver**: Listens via the device microphone to audio tones (frequency-tunable 400Hz–1200Hz or auto-peak detection), calculates audio envelope energy via Goertzel/bandpass algorithm, detects dit/dah intervals, and transcribes messages into text in real time.
2. **Enhanced Optical Camera Receiver**: Replaces the static center-only 25% box with a movable **Tap-to-Aim Reticle**, pinch-to-zoom and zoom slider controls, and an **Exposure Lock** toggle so users can isolate distant light signals or avoid sensor clipping.
3. **Refreshed Tactical Interface**: Elevates the app with a unified military radar aesthetic, dual-mode visual oscilloscope (Audio Waveform / Optical Luminance), and one-tap switching between optical and acoustic reception.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Acoustic Audio Decoding Flow**:
   - User navigates to **Receive** tab and taps the mode selector chip to switch from **Optical** to **Acoustic (Mic)**.
   - The app checks for `RECORD_AUDIO` permission (displaying an access explanation card if not granted).
   - Once active, a real-time audio spectrum/envelope oscilloscope visualizes ambient noise vs. incoming tone pulses.
   - User can lock onto a target tone frequency (defaulting to ham standard 750 Hz or tapping "Auto-Tune") and set squelch/sensitivity threshold.
   - Incoming beeps generate live dits/dahs (`. - .`) in the raw stream, which instantly translate into characters and words with text-to-speech replay and log export.

2. **Precision Optical Aiming Flow**:
   - In **Optical** mode, the user taps anywhere on the camera preview to lock the reticle box onto the target light beacon.
   - The user adjusts the **Zoom Slider** (1.0x to 5.0x) to zoom in on a distant flashlight.
   - User toggles **Exposure Lock** to lock exposure settings, preventing the camera auto-exposure from darkening the entire frame when a bright LED activates.
   - The oscilloscope graphs the luminance signal with an adjustable trigger line.

3. **Transmission & Unified History**:
   - Transmit screen allows sending messages with synchronous torch, audio, vibration, and screen beaconing.
   - Both acoustic-received and optically-received messages are cataloged in Room database with timestamps, transmission mode tags (`AUDIO_RX`, `OPTICAL_RX`, `MANUAL_TX`, `AUTO_TX`), and speed metrics (WPM).

### Visual Identity & Theme Tokens

- **Aesthetic Direction**: Tactical Dark Radar HUD with Amber CRT phosphorus accents.
- **Color Palette**:
  - Background: `TacticalBg` (`#0D0F12`) and `TacticalSurface` (`#161B22`)
  - Accent Primary: `AmberRadar` (`#FFB300`) with bright glow (`#FFC107`)
  - Secondary / Grid Lines: `TacticalSlate` (`#2D3748`) and `RadarGreen` (`#00E676`) for locked signals
  - Warning / Peak: `EmergencyRed` (`#FF3D00`)
- **Typography & Layout**:
  - Monospace telemetry tags for WPM, frequency (Hz), squelch level, and Morse symbols (`.---`).
  - Clear high-contrast typography with minimum 48dp touch targets on all tactical buttons and sliders.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Audio Signal Processing via Direct Android AudioRecord + Goertzel Filter**
  - *Chosen Approach*: Stream raw PCM 16-bit 44.1kHz audio via a coroutine worker, computing narrow-band energy using the Goertzel algorithm centered around the selected frequency (default 750Hz, bandwidth ~100Hz).
  - *Why*: Goertzel is computationally lightweight, operates with negligible CPU and battery footprint on mobile devices, and isolates specific Morse audio tones from background speech or ambient room rumble far better than simple RMS volume.
  - *Alternatives Considered*: Full 2048-point FFT is more resource-heavy and introduces latency; pure amplitude/RMS threshold triggers false positives on room noise or talking.

- **Decision 2: CameraX Tap-to-Aim & Dynamic ROI Extraction**
  - *Chosen Approach*: Map touch coordinates from Compose UI onto normalized image analysis coordinates `(roiX, roiY, roiSize)`, dynamically reading the corresponding pixel sub-grid in `ImageAnalysis.Analyzer`. Use CameraX `CameraControl.setLinearZoom()` and `FocusMeteringAction` for zoom and focus/metering lock.
  - *Why*: Gives complete tracking precision for distant or off-center light sources without requiring physical repositioning of the phone.
  - *Alternatives Considered*: Fixed center box forced the user to awkwardly tilt their phone at extreme angles to keep the light dead center.

- **Decision 3: Room Database Message Type Tagging**
  - *Chosen Approach*: Enhance `MorseMessageEntity` to store `receptionMode` (`OPTICAL`, `ACOUSTIC`, `MANUAL_KEY`, `TEXT_TRANSMIT`).
  - *Why*: Allows users to filter and inspect history logs based on whether a transmission was heard over mic, seen via camera, or sent by hand.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Compose UI Layer                              │
│                                                                        │
│  ┌─────────────────────────┐            ┌───────────────────────────┐  │
│  │     TransmitScreen      │            │       ReceiveScreen       │  │
│  │  - Manual Telegraph Key │            │  - Mode Toggle [Opt / Mic]│  │
│  │  - Beacon & Sliders     │            │  - Camera Tap-to-Aim View │  │
│  └───────────┬─────────────┘            │  - Dual Oscilloscope      │  │
│              │                          └─────────────┬─────────────┘  │
│              │                                        │                │
│              ▼                                        ▼                │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                           MorseViewModel                         │  │
│  │  - Transmit State & WPM        - Decoder Mode [Optical / Audio]  │  │
│  │  - Live Morse Parser           - Oscilloscope History Buffer     │  │
│  └───────────┬────────────────────────────────────────┬─────────────┘  │
└──────────────┼────────────────────────────────────────┼────────────────┘
               │                                        │                 
               ▼                                        ▼                 
┌──────────────────────────────┐        ┌───────────────────────────────┐
│     Transmitter Engines      │        │        Receiver Engines       │
│  - MorseTransmitter          │        │  - CameraMorseDecoder         │
│  - TorchController (Flash)   │        │    (Tap ROI, Exposure, Zoom)  │
│  - AudioTonePlayer (Track)   │        │  - AudioMorseDecoder          │
│  - HapticController          │        │    (AudioRecord + Goertzel)   │
└──────────────────────────────┘        └───────────────────────────────┘
               │                                        │                 
               └───────────────────┬────────────────────┘                 
                                   ▼                                      
                    ┌──────────────────────────────┐                      
                    │     Room Local Persistence   │                      
                    │  - AppDatabase & MorseDao    │                      
                    │  - MorseMessageEntity        │                      
                    │    (SENT, OPTICAL, ACOUSTIC) │                      
                    └──────────────────────────────┘                      
```

### Component & Data Flow Details

1. **`AudioMorseDecoder` (`core/decoder/AudioMorseDecoder.kt`)**:
   - Background audio capture loop on `Dispatchers.Default` using `AudioRecord`.
   - Frequency detection: Goertzel filter centered at `targetPitchHz` (default 750 Hz, adjustable).
   - Moving average energy calculation with hysteresis thresholding (tone ON / tone OFF).
   - Morse symbol timing state machine identical to optical decoder standards (dot = 1 unit, dash = 3 units, character gap = 3 units, word gap = 7 units).
   - Emits `AudioDecoderState` containing live decibel/energy levels, detected frequency, pulse status, decoded letters, and audio waveform data for the oscilloscope.

2. **`CameraMorseDecoder` & `CameraPreviewView` Enhancements**:
   - Normalized ROI center offset: `var roiCenterNormalized: Offset = Offset(0.5f, 0.5f)` and size fraction.
   - Tap-to-focus and metering: CameraX `FocusMeteringAction` to lock exposure and focus on tap.
   - Zoom level state: `cameraControl.setLinearZoom(zoomFraction)`.
   - Exposure compensation adjustment.

3. **`SignalOscilloscope` Component Upgrade**:
   - Supports both luminance curve (green/amber) and audio tone envelope (amber/orange) with real-time trigger line and pulse indicator LED.

4. **Permissions & AndroidManifest**:
   - Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
   - Handle runtime permission requests in `ReceiveScreen` with clean permission prompts.
