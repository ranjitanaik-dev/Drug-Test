# Test Kit Profiles & ROI Specification — DrugTestCompanion

> [!IMPORTANT]
> **DEMO / PROTOTYPE / PHYSICALLY TO BE VALIDATED**  
> This document defines the project-owned test-kit profiles and ROI (Region of Interest) specifications for the 5 sample demo kit variants. The ROI geometries defined herein map the test-strip result windows in canonical $640 \times 960\text{ px}$ reference-card coordinate space. Physical placement dimensions are subject to empirical measurement against manufactured kit hardware.

---

## 1. Overview & Architecture

When an operator selects a drug-test kit in the application, the system automatically resolves the matching `TestKitProfile` and `ReferenceCardProfile`, which provides the kit-specific `RoiSpec`.

```
Selected Test Kit
       │
       ▼
ReferenceCardProfileRepository
       │
       ▼
ReferenceCardProfile (e.g. VARIANT_A)
       │
       ▼
Kit-Specific RoiSpec
       │
       ▼
RoiExtractor (Crops Test Region in Canonical 640x960 Space)
```

---

## 2. Canonical Coordinate System & Excluded Calibration Region

All ROI coordinates are specified in canonical pixel bounds ($640 \times 960\text{ px}$):
- **Canonical Card Width ($W_c$):** $640\text{ pixels}$
- **Canonical Card Height ($H_c$):** $960\text{ pixels}$
- **Color Calibration Patch Region (EXCLUDED):** $y \in [432, 720]\text{ px}$  
  *(Contains White, Gray, Black, Red, Green, Blue calibration patches. Test-strip ROIs MUST NOT overlap this region).*

---

## 3. The 5 Sample Kit Profiles & ROI Geometries

| Kit ID | Reference Card Variant | Kit Description | Canonical ROI Bounding Box `[xMin, yMin, width, height]` | Physical Region Represented | Validation Status |
|---|---|---|---|---|---|
| **`KIT_A`** | `VARIANT_A` | 5-Panel Urine Drug Test Cassette | `[140, 250, 360, 160]` | 5-Panel Cassette Result Window (Upper Region) | `DEMO / PROTOTYPE` |
| **`KIT_B`** | `VARIANT_B` | 10-Panel Urine Drug Test Cassette | `[100, 240, 440, 180]` | 10-Panel Multi-Strip Window (Upper Region) | `DEMO / PROTOTYPE` |
| **`KIT_C`** | `VARIANT_C` | 12-Panel Urine Drug Test Cassette | `[80, 230, 480, 195]` | 12-Panel Wide Strip Window (Upper Region) | `DEMO / PROTOTYPE` |
| **`KIT_D`** | `VARIANT_D` | Multi-Panel Urine Drug Test Cup | `[120, 730, 400, 120]` | Urine Cup Test Panel Window (Lower Region) | `DEMO / PROTOTYPE` |
| **`KIT_E`** | `VARIANT_E` | Multi-Panel Saliva / Oral-Fluid Device | `[180, 250, 280, 170]` | Oral Fluid Result Window (Upper-Right Region) | `DEMO / PROTOTYPE` |

---

## 4. ROI Coordinate Verification Matrix

Every profile's ROI geometry is verified to lie strictly within the $640 \times 960\text{ px}$ frame and avoid the $[432, 720]\text{ px}$ color calibration zone:

- **`KIT_A`**: $x \in [140, 500]$, $y \in [250, 410]$ $\rightarrow$ $y_{\max} = 410 < 432$ (CLEARS COLOR PATCHES)
- **`KIT_B`**: $x \in [100, 540]$, $y \in [240, 420]$ $\rightarrow$ $y_{\max} = 420 < 432$ (CLEARS COLOR PATCHES)
- **`KIT_C`**: $x \in [80, 560]$, $y \in [230, 425]$ $\rightarrow$ $y_{\max} = 425 < 432$ (CLEARS COLOR PATCHES)
- **`KIT_D`**: $x \in [120, 520]$, $y \in [730, 850]$ $\rightarrow$ $y_{\min} = 730 > 720$ (CLEARS COLOR PATCHES)
- **`KIT_E`**: $x \in [180, 460]$, $y \in [250, 420]$ $\rightarrow$ $y_{\max} = 420 < 432$ (CLEARS COLOR PATCHES)
