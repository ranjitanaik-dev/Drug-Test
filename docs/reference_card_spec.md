# Reference Card Specification — Digital Companion for Field Drug Testing

> [!IMPORTANT]
> **PROJECT DRAFT SPECIFICATION**  
> This document defines the project-owned software reference-card architecture, geometry, ArUco marker configuration, and color calibration framework. It serves as the single source of truth for computer-vision calibration and alignment pipelines.  
> **Physical color target values are currently UNMEASURED and marked as `STATUS: TO BE MEASURED / VERIFIED`.**

---

## 1. Purpose

The Reference Card provides a standardized physical anchor placed alongside drug-test kits during ambient-light camera capture. It enables the computer-vision pipeline to:
1. Detect and localize the reference card quadrilateral boundary using 4 corner ArUco markers.
2. Correct geometric perspective distortion by warping the detected card into a canonical coordinate space ($640 \times 960$ px).
3. Sample 6 standardized color patches (White, Gray, Black, Red, Green, Blue) to measure ambient lighting variance.
4. Perform color space transformations (sRGB $\rightarrow$ CIE L\*a\*b\*) and calculate color correction gain matrices prior to strip/patch evaluation.

---

## 2. Reference-Card Architecture

Each physical reference card uses a unified physical layout to ensure that a single computer-vision detection pipeline can process all test kit variants.

### Conceptual Layout

```
┌──────────────────────────────────────────────────────────┐
│  [ARUCO 1]                                    [ARUCO 2]  │
│  (ID: 1, Top-Left)                            (ID: 2, TR)│
│                                                          │
│                 [ CARD TITLE / KIT ID ]                  │
│                      (Variant Header)                    │
│                                                          │
│         ┌───────────┐   ┌───────────┐   ┌───────────┐    │
│         │   WHITE   │   │   GRAY    │   │   BLACK   │    │
│         └───────────┘   └───────────┘   └───────────┘    │
│                                                          │
│         ┌───────────┐   ┌───────────┐   ┌───────────┐    │
│         │    RED    │   │   GREEN   │   │   BLUE    │    │
│         └───────────┘   └───────────┘   └───────────┘    │
│                                                          │
│  [ARUCO 3]                                    [ARUCO 4]  │
│  (ID: 3, Bottom-Left)                      (ID: 4, BR)   │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Five Logical Variants

The software architecture supports 5 distinct reference card variants corresponding to different field drug-testing kit configurations.

| Variant ID | Kit Type | Card Title | Primary Field Use Case | Software Identification |
|---|---|---|---|---|
| **VARIANT_A** | Standard 5-Panel Drug Test Kit | `5-PANEL REFERENCE CARD` | Standard 5-substance urine test cassette | `card_variant_id = "VARIANT_A"` |
| **VARIANT_B** | Standard 10-Panel Drug Test Kit | `10-PANEL REFERENCE CARD` | Multi-panel urine test cassette | `card_variant_id = "VARIANT_B"` |
| **VARIANT_C** | Premium Multi-Panel Drug Test Kit | `PREMIUM MULTI-PANEL CARD` | High-density multi-analyte test panel | `card_variant_id = "VARIANT_C"` |
| **VARIANT_D** | Rapid Cup-Style Drug Test Kit | `RAPID CUP REFERENCE CARD` | Integrated urine test cup | `card_variant_id = "VARIANT_D"` |
| **VARIANT_E** | Laboratory / Reference Variant | `LAB CALIBRATION CARD` | Laboratory verification & quality control | `card_variant_id = "VARIANT_E"` |

> [!NOTE]
> All 5 variants share identical physical dimensions, ArUco dictionary settings, corner ordering conventions, and normalized patch sampling ROIs. Software identifies variants via the `card_variant_id` metadata field and ArUco marker configuration, avoiding reliance on unreliable visual color estimation.

---

## 4. Physical Dimensions

- **Physical Card Width:** $100\text{ mm}$
- **Physical Card Height:** $150\text{ mm}$
- **Physical Aspect Ratio:** $2:3$ ($1:1.50$)
- **ArUco Marker Outer Size:** $20\text{ mm} \times 20\text{ mm}$
- **Color Patch Size:** $18\text{ mm} \times 18\text{ mm}$
- **Card Substrate Material:** Matte non-reflective rigid synthetic stock (prevents specular glare)

---

## 5. Canonical Image Coordinate System

For computer-vision processing, every detected reference card image is perspective-warped into a standard canonical pixel coordinate space:
- **Canonical Image Width ($W_c$):** $640\text{ pixels}$
- **Canonical Image Height ($H_c$):** $960\text{ pixels}$
- **Canonical Aspect Ratio:** $640 / 960 = 0.6667$ ($2:3$)

---

## 6. ArUco Dictionary & Marker Configuration

To ensure robust marker detection under field lighting conditions:

- **ArUco Dictionary:** `DICT_4X4_50` (OpenCV predefined 4x4 bit dictionary with 50 unique IDs)
- **Marker IDs:**
  - **Marker 1 (Top-Left):** `ID = 1`
  - **Marker 2 (Top-Right):** `ID = 2`
  - **Marker 3 (Bottom-Left):** `ID = 3`
  - **Marker 4 (Bottom-Right):** `ID = 4`
- **Minimum Visible Marker Size:** At least $24 \times 24$ pixels in captured raw image.

---

## 7. Marker Placement & Normalized Coordinates

Marker positions are defined in normalized coordinates $[0.0, 1.0]$ relative to the full card bounds $(W_c, H_c)$:

| Marker Position | Marker ID | Normalized Bounding Box $[x_{\min}, y_{\min}, x_{\max}, y_{\max}]$ | Canonical Pixel Box (640x960) |
|---|---|---|---|
| **Top-Left** | `1` | $[0.05, 0.03, 0.25, 0.18]$ | $[32, 29, 160, 173]$ |
| **Top-Right** | `2` | $[0.75, 0.03, 0.95, 0.18]$ | $[480, 29, 608, 173]$ |
| **Bottom-Left** | `3` | $[0.05, 0.82, 0.25, 0.97]$ | $[32, 787, 160, 931]$ |
| **Bottom-Right** | `4` | $[0.75, 0.82, 0.95, 0.97]$ | $[480, 787, 608, 931]$ |

---

## 8. Corner Ordering Convention

ArUco detection returns 4 boundary corners for perspective warping. The detector MUST order points in strict clockwise order starting from Top-Left:

```
Index 0: Top-Left (TL)     -> Target Canonical Point: (0, 0)
Index 1: Top-Right (TR)    -> Target Canonical Point: (640, 0)
Index 2: Bottom-Right (BR) -> Target Canonical Point: (640, 960)
Index 3: Bottom-Left (BL)  -> Target Canonical Point: (0, 960)
```

> [!IMPORTANT]
> **Why Order Matters:**  
> OpenCV's `Imgproc.getPerspectiveTransform(srcMat, dstMat)` requires `srcMat` and `dstMat` points to match exactly in index order. Incorrect point ordering causes invalid image rotation, inversion, or mathematical transformation errors.

---

## 9. Color Patch Layout & Normalized Coordinates

Six color patches are placed in the central region of the card, arranged in 2 rows of 3 columns.

### Patch Layout Grid

- **Top Row (Grayscale Scale):** White, Gray, Black
- **Bottom Row (Primary Color Scale):** Red, Green, Blue

### Normalized Patch ROIs & Canonical Coordinates

To avoid boundary edge bleeding, sampling is restricted to a **safe 20% inner margin** around the center of each patch.

| Patch Name | Row / Col | Normalized Bounding Box $[x_{\min}, y_{\min}, x_{\max}, y_{\max}]$ | Normalized Center $(x_c, y_c)$ | Canonical Pixel ROI ($640 \times 960$) | Safe Sampling Center Box ($10 \times 10$ px) |
|---|---|---|---|---|---|
| **White** | Top / 1 | $[0.25, 0.45, 0.43, 0.57]$ | $(0.34, 0.51)$ | $[160, 432, 275, 547]$ | $[212, 485, 222, 495]$ |
| **Gray** | Top / 2 | $[0.43, 0.45, 0.61, 0.57]$ | $(0.52, 0.51)$ | $[275, 432, 390, 547]$ | $[328, 485, 338, 495]$ |
| **Black** | Top / 3 | $[0.61, 0.45, 0.79, 0.57]$ | $(0.70, 0.51)$ | $[390, 432, 505, 547]$ | $[443, 485, 453, 495]$ |
| **Red** | Bottom / 1 | $[0.25, 0.63, 0.43, 0.75]$ | $(0.34, 0.69)$ | $[160, 605, 275, 720]$ | $[212, 658, 222, 668]$ |
| **Green** | Bottom / 2 | $[0.43, 0.63, 0.61, 0.75]$ | $(0.52, 0.69)$ | $[275, 605, 390, 720]$ | $[328, 658, 338, 668]$ |
| **Blue** | Bottom / 3 | $[0.61, 0.63, 0.79, 0.75]$ | $(0.70, 0.69)$ | $[390, 605, 505, 720]$ | $[443, 658, 453, 668]$ |

---

## 10. LAB Color Calibration Reference Table

> [!WARNING]
> **STATUS: TO BE MEASURED / VERIFIED**  
> The target CIE L\*a\*b\* values below must be measured from physical reference cards using a calibrated spectrophotometer (D65 illuminant, $2^\circ$ standard observer) under controlled lighting. The table below represents the required schema structure with placeholder values marked as `TBD`.

| Patch Name | Target $L^*$ | Target $a^*$ | Target $b^*$ | Status |
|---|---|---|---|---|
| **White** | `TBD` | `TBD` | `TBD` | To be measured |
| **Gray** | `TBD` | `TBD` | `TBD` | To be measured |
| **Black** | `TBD` | `TBD` | `TBD` | To be measured |
| **Red** | `TBD` | `TBD` | `TBD` | To be measured |
| **Green** | `TBD` | `TBD` | `TBD` | To be measured |
| **Blue** | `TBD` | `TBD` | `TBD` | To be measured |

---

## 11. Calibration Procedure

When an image passes Phase 2 Quality Gate (Blur & Exposure checks), the Phase 3 Calibration Engine executes the following deterministic steps:

```
[ Captured Image ]
        │
        ▼
1. Detect 4 ArUco Markers (IDs 1, 2, 3, 4 via OpenCV ArucoDetector)
        │
        ▼
2. Order Corners clockwise: [Top-Left, Top-Right, Bottom-Right, Bottom-Left]
        │
        ▼
3. Compute Perspective Transform Matrix & Warp to Canonical 640x960 Frame
        │
        ▼
4. Crop & Sample Safe Inner Center ROIs for 6 Color Patches
        │
        ▼
5. Convert Sampled sRGB Values -> Linear sRGB -> CIE XYZ -> CIE L*a*b* (D65)
        │
        ▼
6. Compare Measured LAB against Target Reference LAB Table
        │
        ▼
7. Calculate CIE Delta E (ΔE_ab) & Fit Color Correction Transformation Matrix
```

---

## 12. $\Delta E$ Calculation Requirement

Color error is calculated using the CIE $1976$ $\Delta E_{ab}$ color difference formula:

$$\Delta E_{ab} = \sqrt{(L_1^* - L_2^*)^2 + (a_1^* - a_2^*)^2 + (b_1^* - b_2^*)^2}$$

Where:
- $(L_1^*, a_1^*, b_1^*)$ = Measured/Sampled patch LAB color.
- $(L_2^*, a_2^*, b_2^*)$ = Target reference LAB color from specification.

---

## 13. Tolerance Status

- **Calibration Acceptance Tolerance ($\Delta E_{\text{max}}$):** `STATUS: TBD`
- **Tuning Requirement:** The maximum acceptable $\Delta E$ threshold cannot be arbitrarily declared. It must be empirically validated against a dataset of physical card captures under varied ambient illuminations ($2500\text{K} - 6500\text{K}$).

---

## 14. Variant Identification Schema

The software identifies the active test kit variant using a structured metadata record embedded in the system payload:

```json
{
  "card_variant_id": "VARIANT_A",
  "kit_type": "5-Panel Drug Test Kit",
  "card_title": "5-PANEL REFERENCE CARD",
  "aruco_dictionary": "DICT_4X4_50",
  "aruco_marker_ids": [1, 2, 3, 4],
  "canonical_dimensions": {
    "width_px": 640,
    "height_px": 960
  }
}
```

---

## 15. Draft Evidence Data Schema

> [!IMPORTANT]
> **PROJECT DRAFT ONLY — NOT AN OFFICIAL BACKEND SCHEMA**  
> The record structure below defines the mobile data model produced during Phase 3 capture & calibration.

```json
{
  "image_id": "img_20261024_104522_001",
  "timestamp_epoch_ms": 1792838722000,
  "card_variant": "VARIANT_A",
  "quality_status": "PASS",
  "detected_corners": [
    {"x": 32.5, "y": 29.1},
    {"x": 608.2, "y": 29.5},
    {"x": 607.8, "y": 931.2},
    {"x": 32.1, "y": 930.8}
  ],
  "patch_measurements": {
    "white": {"sampled_rgb": [240, 242, 245], "measured_lab": {"l": 94.8, "a": -0.2, "b": 1.1}},
    "gray":  {"sampled_rgb": [128, 130, 131], "measured_lab": {"l": 51.2, "a": -0.1, "b": -0.3}},
    "black": {"sampled_rgb": [25, 26, 28],    "measured_lab": {"l": 9.8,  "a": 0.3,  "b": -0.5}},
    "red":   {"sampled_rgb": [180, 45, 42],   "measured_lab": {"l": 44.2, "a": 53.8, "b": 28.5}},
    "green": {"sampled_rgb": [40, 160, 65],   "measured_lab": {"l": 54.1, "a": -48.9, "b": 33.2}},
    "blue":  {"sampled_rgb": [35, 75, 170],   "measured_lab": {"l": 31.0, "a": 14.2, "b": -48.1}}
  },
  "calibration_data": {
    "mean_delta_e": 3.42,
    "calibration_status": "CALIBRATED"
  },
  "device_info": {
    "manufacturer": "Google",
    "model": "Pixel 8",
    "android_version": "14"
  }
}
```

---

## 16. Required Physical Assets

To implement and test Phase 3, the following physical assets are required:
1. **Printed Reference Cards:** Physical cards printed on matte non-reflective stock matching Variants A–E.
2. **Spectrophotometer Measurements:** Official CIE L\*a\*b\* measurements for each of the 6 color patches under D65 lighting.
3. **Official OpenCV 4.9.0 Android AAR (`opencv-4.9.0.aar`):** Built with `opencv_objdetect` module containing Java bindings for `org.opencv.objdetect.ArucoDetector`.
4. **Physical Capture Fixtures:** 2–3 real physical photographs of the card taken under varied lighting and tilt angles.

---

## 17. Open Questions / Items Requiring Physical Measurement

1. What are the exact spectrophotometer $L^*, a^*, b^*$ values for the printed physical reference card?
2. What is the empirical $\Delta E$ tolerance threshold across different smartphone camera sensors (e.g. Pixel, Samsung, Motorola)?
3. Will the 5 card variants share identical ArUco marker IDs (1, 2, 3, 4) or will different variants use unique marker ID sets (e.g. Variant B = 5, 6, 7, 8) for hardware-level variant identification?

---

## 18. Phase 3 Implementation Checklist (Future Execution Phase)

- [ ] Obtain official `opencv-4.9.0.aar` with `ArucoDetector` bindings.
- [ ] Measure physical card color patches to complete Section 10 LAB table.
- [ ] Add 2–3 real reference card fixture images under `app/src/test/resources/fixtures/`.
- [ ] Implement `cv/CardDetector.kt` using `org.opencv.objdetect.ArucoDetector`.
- [ ] Implement `cv/ColorCalibrator.kt` using `Imgproc.warpPerspective` and sRGB-to-LAB conversion.
- [ ] Update `QualityGate.kt` Check 5 to run `CardDetector`.
- [ ] Create `CardDetectorTest` and `ColorCalibratorTest` verifying corner detection and Delta E calculations against real image fixtures.
