# Niriksh – Digital Companion for Field Drug Testing

NiRIKSH is an AI-assisted, privacy-first Android mobile application designed to support law enforcement and narcotics control officers during field drug testing. It standardizes colorimetric test interpretation, compensates for variable lighting conditions, and generates tamper-evident digital evidence records.

---

## Overview

Niriksh serves as a digital companion for existing physical colorimetric field-test kits (such as rapid reagent kit boxes, dual-well pouches, and multi-panel fluid cups). It utilizes guided camera capture, reference-card color calibration, quality checking, reaction Region of Interest (ROI) extraction, and on-device machine learning classification.

> [!IMPORTANT]
> **Presumptive Results Notice**: Niriksh records and reports **presumptive field-test indications** only:
> - **POSITIVE**
> - **NEGATIVE**
> - **INCONCLUSIVE**
>
> The application does **NOT** perform definitive chemical identification or laboratory confirmation. Confirmatory laboratory testing (such as GC-MS or LC-MS) is mandatory prior to legal disposition.

---

## Problem Statement

**SIH Problem Statement 26231 – Digital Companion for Field Drug Testing**

In field law enforcement operations, chemical spot testing faces significant challenges:
- **Subjective Visual Interpretation**: Variances in human color perception, especially for faint or ambiguous color reactions.
- **Lighting & Environmental Variations**: Changing ambient sunlight, shade, or artificial illumination distorting observed chemical colors.
- **Camera Capture Inconsistencies**: Varying tilt angles, distances, motion blur, and incorrect exposure levels.
- **Record Preservation**: Lack of standardized, digital chain-of-custody documentation.
- **Evidence Integrity**: Vulnerability of field notes and photos to post-capture alteration or dispute in court.

Niriksh solves these challenges by combining automated computer vision quality gates, reference-card color calibration, MobileNetV2 neural classification, real-time GPS geocoding, and hardware-backed cryptographic signing.

---

## Key Features

- **Guided Camera Capture**: Real-time CameraX viewfinder with augmented reality (AR) guidance overlay ensuring proper distance, framing, and tilt alignment.
- **Reference-Card Detection**: Automated detection of 4-corner ArUco reference card markers (`DICT_4X4_50`).
- **Automated Quality Gate**: Real-time validation of Laplacian blur variance ($\ge 45.0$), exposure levels ($30 \le \mu \le 225$), and minimum resolution.
- **Lighting & Color Calibration**: Solves a 3x3 least-squares RGB gain matrix using 6 target color patches (White, Gray, Black, Red, Green, Blue) to eliminate ambient lighting distortions.
- **Test-Device Detection & Perspective Correction**: Perspective warp and automatic long-axis orientation alignment to extract the exact $386 \times 133$ pixel reaction ROI.
- **On-Device AI Classification**: TensorFlow Lite MobileNetV2 neural inference evaluating reaction ROIs into **POSITIVE**, **NEGATIVE**, or **INCONCLUSIVE** states.
- **Digital Test Records**: Structured, tamper-evident case dockets containing record ID, timestamp, kit profile, officer ID, classification result, and match confidence.
- **GPS Location & Geocoding**: Automatic capture of device GPS coordinates and reverse-geocoded address lines (e.g., `Bengaluru, Karnataka, India`).
- **Cryptographic Evidence Ledger**: Full-frame image SHA-256 hashing and Android KeyStore RSA-2048 digital signatures (`SHA256withRSA`).
- **Court-Admissible PDF Export**: Native PDF report generator producing signed case dockets ready for printing or instant sharing.
- **Searchable Test History & Tamper Verification**: Real-time search across historical records with built-in byte tampering simulation and re-verification checks.
- **Multilingual Support**: Support for 5 languages (English, Hindi, Kannada, Tamil, Telugu) with offline DataStore persistence and translation caching.
- **Audio Voice Feedback**: TextToSpeech audio announcements for app startup greeting (`"Welcome"`) and real-time result announcements (`"Test result: Positive"`).
- **Secure Officer Access**: Dedicated access portal with Officer ID authentication, biometric visual graphic, and system status command dashboard.

---

## AI/ML Pipeline

```
Camera Capture
      ↓
Quality Gate (Blur & Exposure Checks)
      ↓
Reference Card Detection (OpenCV ArUco)
      ↓
Lighting / Color Calibration (3x3 RGB Matrix)
      ↓
Test Device Detection & Perspective Correction
      ↓
Reaction ROI Extraction (386 × 133 × 3 RGB)
      ↓
MobileNetV2 TFLite Classifier
      ↓
POSITIVE / NEGATIVE / INCONCLUSIVE
      ↓
Digital Record Ledger & Signed PDF Docket
```

> [!NOTE]
> **Calibration vs. Classification**: The reference card is strictly used by OpenCV to calculate the 3x3 color calibration matrix and normalize lighting. It is not the target of the machine learning classifier. The classifier evaluates only the extracted $386 \times 133$ reaction window ROI.

---

## Machine Learning Model

- **Architecture**: MobileNetV2 (Transfer Learning)
- **Target Classes**: `NEGATIVE`, `POSITIVE`, `INCONCLUSIVE`
- **Input Dimension**: $386 \times 133 \times 3$ RGB Float32
- **TFLite Model File**: `app/src/main/assets/drug_test_classifier_float32.tflite`
- **Inference Runtime**: TensorFlow Lite Interpreter on-device execution (Offline-capable)

### Prototype Dataset & Benchmark Metrics
- **Total Usable ROI Images**: 292
- **Training Subset**: 204
- **Validation Subset**: 43
- **Testing Subset**: 45
- **Test Accuracy**: 97.78% (44 / 45)
- **Validation Accuracy**: 97.67% (42 / 43)
- **TFLite Verification Agreement**: 100.0% (45 / 45)

> [!WARNING]
> **Synthetic Prototype Notice**: The current dataset consists of synthetic prototype ROI images generated during development. These metrics reflect prototype performance and require physical laboratory/field validation with real chemical reagents before operational deployment.

---

## Digital Records & Evidence Integrity

Niriksh maintains an immutable digital chain-of-custody ledger for every captured test:
1. **Full-Frame Image Hash**: Calculates a SHA-256 cryptographic digest of the raw JPEG capture file.
2. **GPS Metadata**: Attaches real-time GPS coordinates and reverse-geocoded location strings.
3. **Hardware Digital Signature**: Signs the canonical test metadata using an RSA-2048 key pair generated inside the hardware-backed Android KeyStore.
4. **Tamper Check**: Allows officers or legal counsel to re-verify stored records against the image file on disk. If image bytes are modified, the verification engine detects the SHA-256 mismatch and flags the signature as `INVALID`.

---

## Multilingual Support

Niriksh supports 5 languages to facilitate field adoption across diverse regional units:
- **English** (`en`)
- **Hindi** (`hi`)
- **Kannada** (`kn`)
- **Tamil** (`ta`)
- **Telugu** (`te`)

Fixed UI strings resolve locally through Android resource values, while dynamic content uses DataStore persistence with offline translation caching.

---

## Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture + MVVM + MVI
- **Camera Engine**: CameraX 1.4.0 (Lifecycle, ImageCapture, ImageAnalysis)
- **Computer Vision**: OpenCV 4.9.0 (ArUco Marker Detection, WarpPerspective, Color Calibration)
- **On-Device ML**: TensorFlow Lite 2.16.1 (MobileNetV2 Float32)
- **Local Persistence**: Room 2.6.1 & DataStore
- **Dependency Injection**: Hilt 2.51.1
- **Async & Reactive**: Kotlin Coroutines & Flow
- **Build System**: Gradle 8.11.1 + KSP 2.0.21-1.0.25

---

## Application Flow

```
Home Page Portal
      ↓
Officer Login & Access
      ↓
Officer Dashboard
      ↓
Select Test Kit Profile
      ↓
Guided Camera Capture (AR Overlay)
      ↓
Quality Check (Blur & Exposure)
      ↓
AI Analysis & Spectrometry
      ↓
Test Result & Voice Announcement
      ↓
Digital Record Ledger & PDF Export
      ↓
Searchable Test History
```

Officers can access the **Test History** module at any time to search, review, open PDF case dockets, or perform integrity verification checks.

---

## Installation & Setup

### For Field Users
1. Download the latest Android APK from the [GitHub Releases](https://github.com/ranjitanaik-dev/Drug-Test/releases) section.
2. Install the `Niriksh-v1.0.0.apk` file on an Android device running **Android 8.0 (API Level 26)** or higher.
3. Launch **Niriksh** and grant **Camera** and **Location** permissions when prompted.

### For Developers

#### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 35

#### Clone and Build

```bash
# Clone the repository
git clone https://github.com/ranjitanaik-dev/Drug-Test.git
cd Drug-Test

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## License & Disclaimer
This project is developed for educational and evaluation purposes under SIH Problem Statement 26231. All field test results generated by Niriksh are presumptive indications and require confirmatory laboratory analysis prior to legal disposition.
