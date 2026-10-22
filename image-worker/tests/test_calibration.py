import io
import os
import cv2
import numpy as np
from PIL import Image, ImageEnhance
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "fixtures")

@pytest.fixture(scope="session", autouse=True)
def generate_fixtures():
    os.makedirs(FIXTURES_DIR, exist_ok=True)
    orig_path = os.path.join(FIXTURES_DIR, "original.png")
    
    # 1. Generate base original image (rich multi-color texture with geometric shapes and text)
    img = np.zeros((400, 600, 3), dtype=np.uint8)
    # Background gradient
    for y in range(400):
        for x in range(600):
            img[y, x] = [int(x / 600 * 200), int(y / 400 * 180), 120]
    # Add shapes & text
    cv2.circle(img, (180, 180), 65, (0, 220, 255), -1)
    cv2.rectangle(img, (320, 100), (480, 260), (255, 100, 50), -1)
    cv2.putText(img, "VORZAK ARCHIVE", (120, 340), cv2.FONT_HERSHEY_SIMPLEX, 1.2, (255, 255, 255), 3)
    cv2.imwrite(orig_path, img)

    # (a) Resized (scaled down to 80%)
    resized = cv2.resize(img, (480, 320), interpolation=cv2.INTER_AREA)
    cv2.imwrite(os.path.join(FIXTURES_DIR, "case_a_resized.png"), resized)

    # (b) JPEG q60 compressed
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    pil_img.save(os.path.join(FIXTURES_DIR, "case_b_jpeg_q60.jpg"), "JPEG", quality=60)

    # (c) Brightness shift (+30%)
    enhancer = ImageEnhance.Brightness(pil_img)
    bright_img = enhancer.enhance(1.3)
    bright_img.save(os.path.join(FIXTURES_DIR, "case_c_brightness.png"), "PNG")

    # (d) Mild crop (top-left 50px cropped)
    cropped = img[40:380, 50:560]
    cv2.imwrite(os.path.join(FIXTURES_DIR, "case_d_crop.png"), cropped)

    # (e) Pasted / Spliced Object (paste a distinct contrasting badge at coordinate 220, 150)
    pasted = img.copy()
    cv2.circle(pasted, (360, 200), 40, (0, 0, 255), -1)
    cv2.putText(pasted, "NEW", (335, 210), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
    cv2.imwrite(os.path.join(FIXTURES_DIR, "case_e_pasted.png"), pasted)

def test_calibration_case_a_resized():
    with open(os.path.join(FIXTURES_DIR, "original.png"), "rb") as f_sub, \
         open(os.path.join(FIXTURES_DIR, "case_a_resized.png"), "rb") as f_cand:
        response = client.post(
            "/compare",
            files={
                "submitted": ("original.png", f_sub, "image/png"),
                "candidate": ("case_a_resized.png", f_cand, "image/png")
            }
        )
    assert response.status_code == 200
    data = response.json()
    assert data["alignment_status"] == "ALIGNED"
    assert data["diff_region_count"] <= 1 # ~zero unexplained difference regions
    assert data["ssim_normalized"] > 0.85

def test_calibration_case_b_jpeg_q60():
    with open(os.path.join(FIXTURES_DIR, "original.png"), "rb") as f_sub, \
         open(os.path.join(FIXTURES_DIR, "case_b_jpeg_q60.jpg"), "rb") as f_cand:
        response = client.post(
            "/compare",
            files={
                "submitted": ("original.png", f_sub, "image/png"),
                "candidate": ("case_b_jpeg_q60.jpg", f_cand, "image/jpeg")
            }
        )
    assert response.status_code == 200
    data = response.json()
    assert data["alignment_status"] == "ALIGNED"
    assert data["diff_region_count"] <= 1 # Compression noise successfully suppressed

def test_calibration_case_c_brightness():
    with open(os.path.join(FIXTURES_DIR, "original.png"), "rb") as f_sub, \
         open(os.path.join(FIXTURES_DIR, "case_c_brightness.png"), "rb") as f_cand:
        response = client.post(
            "/compare",
            files={
                "submitted": ("original.png", f_sub, "image/png"),
                "candidate": ("case_c_brightness.png", f_cand, "image/png")
            }
        )
    assert response.status_code == 200
    data = response.json()
    assert data["alignment_status"] == "ALIGNED"
    assert data["diff_region_count"] <= 1 # Lighting shifts normalized by histogram matching

def test_calibration_case_e_pasted_object():
    with open(os.path.join(FIXTURES_DIR, "original.png"), "rb") as f_sub, \
         open(os.path.join(FIXTURES_DIR, "case_e_pasted.png"), "rb") as f_cand:
        response = client.post(
            "/compare",
            files={
                "submitted": ("original.png", f_sub, "image/png"),
                "candidate": ("case_e_pasted.png", f_cand, "image/png")
            }
        )
    assert response.status_code == 200
    data = response.json()
    assert data["alignment_status"] == "ALIGNED"
    # Mandatory requirement: produces at least one unexplained difference region
    assert data["diff_region_count"] >= 1
    assert len(data["regions"]) >= 1
    assert data["diff_image_base64"] is not None
