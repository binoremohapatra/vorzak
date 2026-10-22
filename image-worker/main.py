import io
import math
import base64
from typing import List, Dict, Any, Optional
import cv2
import numpy as np
from PIL import Image
import imagehash
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel

app = FastAPI(
    title="VORZAK Image Worker",
    version="0.1.0",
    description="Deterministic visual alignment, SSIM analysis, and difference detection service"
)

# Safety caps
MAX_IMAGE_PIXELS = 40_000_000
MAX_FILE_BYTES = 10 * 1024 * 1024 # 10MB
MIN_INLIERS = 15
WORKING_MAX_DIM = 1024

class DiffRegion(BaseModel):
    x: int
    y: int
    w: int
    h: int
    area: int

class CompareResponse(BaseModel):
    alignment_status: str # "ALIGNED" | "NOT_COMPARABLE"
    inlier_count: int
    ssim_normalized: Optional[float]
    diff_region_count: int
    regions: List[DiffRegion]
    phash_distance: int
    diff_image_base64: Optional[str]
    notes: List[str]

class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    max_pixels: int

@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="UP",
        service="image-worker",
        version="0.1.0",
        max_pixels=MAX_IMAGE_PIXELS
    )

def safe_decode_image(file_bytes: bytes) -> np.ndarray:
    if len(file_bytes) > MAX_FILE_BYTES:
        raise HTTPException(status_code=400, detail="Image file exceeds 10MB limit")
    
    # Enforce decompression bomb protection via Pillow
    Image.MAX_IMAGE_PIXELS = MAX_IMAGE_PIXELS
    try:
        pil_img = Image.open(io.BytesIO(file_bytes))
        pil_img.verify()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid or corrupted image: {str(e)}")

    # Re-open for actual reading
    pil_img = Image.open(io.BytesIO(file_bytes))
    if pil_img.width * pil_img.height > MAX_IMAGE_PIXELS:
        raise HTTPException(status_code=400, detail="Image pixel dimensions exceed safety limit")

    # Convert to RGB OpenCV format (BGR)
    rgb_img = pil_img.convert("RGB")
    np_arr = np.array(rgb_img)
    bgr_img = cv2.cvtColor(np_arr, cv2.COLOR_RGB2BGR)
    return bgr_img

def compute_phash_distance(img1: np.ndarray, img2: np.ndarray) -> int:
    pil1 = Image.fromarray(cv2.cvtColor(img1, cv2.COLOR_BGR2RGB))
    pil2 = Image.fromarray(cv2.cvtColor(img2, cv2.COLOR_BGR2RGB))
    h1 = imagehash.phash(pil1)
    h2 = imagehash.phash(pil2)
    return int(h1 - h2)

def resize_to_max(img: np.ndarray, max_dim: int = WORKING_MAX_DIM) -> np.ndarray:
    h, w = img.shape[:2]
    if max(h, w) <= max_dim:
        return img
    scale = max_dim / float(max(h, w))
    nw, nh = int(round(w * scale)), int(round(h * scale))
    return cv2.resize(img, (nw, nh), interpolation=cv2.INTER_AREA)

def histogram_match_channel(source: np.ndarray, template: np.ndarray) -> np.ndarray:
    """Matches CDF of source channel to template channel to normalize lighting/contrast."""
    src_vals, src_counts = np.unique(source.ravel(), return_counts=True)
    tmpl_vals, tmpl_counts = np.unique(template.ravel(), return_counts=True)

    src_quantiles = np.cumsum(src_counts).astype(np.float64) / source.size
    tmpl_quantiles = np.cumsum(tmpl_counts).astype(np.float64) / template.size

    interp_vals = np.interp(src_quantiles, tmpl_quantiles, tmpl_vals)
    lut = np.zeros(256, dtype=np.uint8)
    lut[src_vals] = np.clip(np.round(interp_vals), 0, 255).astype(np.uint8)
    return cv2.LUT(source, lut)

def normalize_color(img: np.ndarray, target_ref: np.ndarray) -> np.ndarray:
    """Performs per-channel histogram matching to cancel out global brightness/contrast shifts."""
    matched = np.zeros_like(img)
    for c in range(3):
        matched[:, :, c] = histogram_match_channel(img[:, :, c], target_ref[:, :, c])
    return matched

def compute_ssim_map(img1_gray: np.ndarray, img2_gray: np.ndarray):
    """Computes mean SSIM and SSIM error map between two grayscale images of equal dimensions."""
    C1 = (0.01 * 255) ** 2
    C2 = (0.03 * 255) ** 2

    img1 = img1_gray.astype(np.float64)
    img2 = img2_gray.astype(np.float64)

    kernel = cv2.getGaussianKernel(11, 1.5)
    window = np.outer(kernel, kernel.transpose())

    mu1 = cv2.filter2D(img1, -1, window)[5:-5, 5:-5]
    mu2 = cv2.filter2D(img2, -1, window)[5:-5, 5:-5]

    mu1_sq = mu1 ** 2
    mu2_sq = mu2 ** 2
    mu1_mu2 = mu1 * mu2

    sigma1_sq = cv2.filter2D(img1 ** 2, -1, window)[5:-5, 5:-5] - mu1_sq
    sigma2_sq = cv2.filter2D(img2 ** 2, -1, window)[5:-5, 5:-5] - mu2_sq
    sigma12 = cv2.filter2D(img1 * img2, -1, window)[5:-5, 5:-5] - mu1_mu2

    ssim_map = ((2 * mu1_mu2 + C1) * (2 * sigma12 + C2)) / ((mu1_sq + mu2_sq + C1) * (sigma1_sq + sigma2_sq + C2))
    return float(np.mean(ssim_map)), ssim_map

@app.post("/compare", response_model=CompareResponse)
async def compare_images(
    submitted: UploadFile = File(...),
    candidate: UploadFile = File(...)
):
    notes = []
    sub_bytes = await submitted.read()
    cand_bytes = await candidate.read()

    sub_bgr = safe_decode_image(sub_bytes)
    cand_bgr = safe_decode_image(cand_bytes)

    # 1. Perceptual hash distance
    phash_dist = compute_phash_distance(sub_bgr, cand_bgr)

    # 2. Resize to common working scale
    sub_work = resize_to_max(sub_bgr, WORKING_MAX_DIM)
    cand_work = resize_to_max(cand_bgr, WORKING_MAX_DIM)

    # Target canvas matches submitted image size
    target_h, target_w = sub_work.shape[:2]

    # 3. Feature Detection & Alignment via ORB
    orb = cv2.ORB_create(nfeatures=2000, fastThreshold=12)
    kp_sub, des_sub = orb.detectAndCompute(sub_work, None)
    kp_cand, des_cand = orb.detectAndCompute(cand_work, None)

    inlier_count = 0
    homography = None
    aligned_status = "NOT_COMPARABLE"

    if des_sub is not None and des_cand is not None and len(kp_sub) >= 10 and len(kp_cand) >= 10:
        bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=False)
        matches = bf.knnMatch(des_cand, des_sub, k=2)

        good_matches = []
        for m_n in matches:
            if len(m_n) == 2:
                m, n = m_n
                if m.distance < 0.75 * n.distance:
                    good_matches.append(m)

        if len(good_matches) >= MIN_INLIERS:
            src_pts = np.float32([kp_cand[m.queryIdx].pt for m in good_matches]).reshape(-1, 1, 2)
            dst_pts = np.float32([kp_sub[m.trainIdx].pt for m in good_matches]).reshape(-1, 1, 2)

            H, mask = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC, 5.0)
            if mask is not None:
                inlier_count = int(np.sum(mask))
                if inlier_count >= MIN_INLIERS:
                    homography = H
                    aligned_status = "ALIGNED"

    # If ORB fails or has borderline inliers, fallback to direct aspect-ratio alignment if pHash is very close
    if aligned_status != "ALIGNED":
        # Check if images have near-identical dimensions or pHash distance <= 8 (e.g. crop or simple resize)
        if phash_dist <= 8:
            # Candidate can be directly scaled to target dimensions
            cand_warped = cv2.resize(cand_work, (target_w, target_h), interpolation=cv2.INTER_AREA)
            aligned_status = "ALIGNED"
            notes.append("Aligned via direct dimension projection (pHash distance <= 8)")
        else:
            return CompareResponse(
                alignment_status="NOT_COMPARABLE",
                inlier_count=inlier_count,
                ssim_normalized=None,
                diff_region_count=0,
                regions=[],
                phash_distance=phash_dist,
                diff_image_base64=None,
                notes=["Insufficient feature correspondences to establish geometric alignment"]
            )
    else:
        # Warp candidate onto submitted coordinate frame
        cand_warped = cv2.warpPerspective(cand_work, homography, (target_w, target_h), flags=cv2.INTER_LINEAR)

    # 4. Color & Contrast Normalization (Histogram Matching)
    cand_norm = normalize_color(cand_warped, sub_work)

    # 5. Grayscale conversion for SSIM and difference mapping
    sub_gray = cv2.cvtColor(sub_work, cv2.COLOR_BGR2GRAY)
    cand_gray = cv2.cvtColor(cand_norm, cv2.COLOR_BGR2GRAY)

    # Mask valid overlap region (ignore black boundary areas created by perspective warp)
    warp_mask = np.ones((target_h, target_w), dtype=np.uint8) * 255
    if homography is not None:
        cand_mask_orig = np.ones((cand_work.shape[0], cand_work.shape[1]), dtype=np.uint8) * 255
        warp_mask = cv2.warpPerspective(cand_mask_orig, homography, (target_w, target_h), flags=cv2.INTER_NEAREST)
        # Erode mask slightly to avoid boundary edge false positives
        warp_mask = cv2.erode(warp_mask, np.ones((5, 5), np.uint8))

    # 6. Compute SSIM
    ssim_val, _ = compute_ssim_map(sub_gray, cand_gray)

    # 7. Difference map & Morphological cleanup
    abs_diff = cv2.absdiff(sub_gray, cand_gray)
    abs_diff = cv2.bitwise_and(abs_diff, abs_diff, mask=warp_mask)

    # Absolute difference threshold (> 35 intensity delta)
    _, thresh = cv2.threshold(abs_diff, 35, 255, cv2.THRESH_BINARY)

    # Morphological closing (5x5 ellipse) to eliminate JPEG/compression speckles, then opening
    kernel_close = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    cleaned = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel_close)
    cleaned = cv2.morphologyEx(cleaned, cv2.MORPH_OPEN, kernel_close)

    # 8. Region Extraction (Connected Components / Contours)
    contours, _ = cv2.findContours(cleaned, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    regions: List[DiffRegion] = []

    # Overlay difference canvas (submitted image with glowing semi-transparent highlighted regions)
    diff_canvas = sub_work.copy()
    overlay = sub_work.copy()

    for cnt in contours:
        area = cv2.contourArea(cnt)
        if area >= 150: # Filter sub-visual artifacts < 150 px^2
            x, y, w, h = cv2.boundingRect(cnt)
            regions.append(DiffRegion(x=x, y=y, w=w, h=h, area=int(area)))
            # Draw semi-transparent neon mask in overlay
            cv2.rectangle(overlay, (x, y), (x + w, y + h), (0, 0, 240), -1)
            cv2.rectangle(diff_canvas, (x, y), (x + w, y + h), (0, 0, 255), 2)

    # Blend overlay with 35% opacity
    cv2.addWeighted(overlay, 0.35, diff_canvas, 0.65, 0, diff_canvas)

    # Encode diff image to PNG base64
    _, buf = cv2.imencode(".png", diff_canvas)
    diff_b64 = base64.b64encode(buf.tobytes()).decode("utf-8")

    return CompareResponse(
        alignment_status="ALIGNED",
        inlier_count=inlier_count,
        ssim_normalized=round(float(ssim_val), 4),
        diff_region_count=len(regions),
        regions=regions,
        phash_distance=phash_dist,
        diff_image_base64=diff_b64,
        notes=notes
    )
