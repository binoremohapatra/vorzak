# Image Alignment & Difference Detection Calibration

## 1. Overview
VORZAK uses a deterministic image alignment and comparison pipeline:
`Decode -> Resize to common working canvas -> ORB/SIFT Feature Extraction -> RANSAC Homography -> Perspective Warp -> Histogram Matching / Normalization -> SSIM & Difference Map -> Morphological Cleanup -> Difference Region Bounding Box Extraction`.

## 2. Non-Negotiable Vocabulary
- The system reports **"unexplained difference regions"**, NEVER "tampering", "forgery", or "manipulation".
- The system reports **"earliest discovered appearance"**, NEVER "original" or "true source".
- When images cannot be aligned with sufficient inliers, alignment status is **NOT_COMPARABLE**.

## 3. Calibration Test Matrix (/image-worker/tests/fixtures)
The pipeline is calibrated against 5 transformation cases:
- **Case (a) Resized**: Downscaled or upscaled version of the same image.
  - Expected: `diff_region_count == 0` (or below noise floor threshold).
- **Case (b) JPEG Compression (q60)**: High compression artifacts.
  - Expected: `diff_region_count == 0` (compression noise filtered by morphological kernel).
- **Case (c) Brightness / Exposure Shift (+/- 25%)**: Global illumination change.
  - Expected: `diff_region_count == 0` (normalized via histogram matching).
- **Case (d) Mild Crop**: Peripheral content cropped out.
  - Expected: Inlier homography aligns overlapping sub-canvas; non-overlapping edge masked out.
- **Case (e) Pasted / Spliced Object**: Object spliced into candidate.
  - Expected: `diff_region_count >= 1` with precise bounding box coordinates `{x, y, w, h}`.

## 4. Threshold Values & Tolerances
- **Minimum Inliers**: 15 RANSAC matches required for `ALIGNED` status.
- **SSIM Window Size**: 11x11 Gaussian.
- **Difference Threshold**: Pixel delta > 30 (on 0-255 scale) following histogram matching.
- **Morphological Kernel**: 5x5 ellipse closing to eliminate single-pixel compression artifacts.
- **Minimum Region Area**: 150 px² (filters sub-visual speckles).

## 5. Known Failure Cases & Mitigations
- Severe perspective distortion (> 45 degree off-axis rotation).
- Monochromatic or featureless backgrounds (insufficient keypoints -> fallback to NOT_COMPARABLE).
- Heavy non-linear color grading (addressed via channel-wise CDF histogram matching).
