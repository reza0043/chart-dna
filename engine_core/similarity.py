"""
توابع محاسبه شباهت الگوهای قیمت در Chart DNA.

معیارهای پشتیبانی‌شده:
- pearson: همبستگی پیرسون
- mean_abs_diff: میانگین قدرمطلق اختلاف
- slope: شباهت جهت و شیب حرکت
- dtw: Dynamic Time Warping
- structural: شباهت نقاط چرخش و ساختار
"""

from typing import Dict, Optional

import numpy as np

from core.config import (
    DEFAULT_SIMILARITY_WEIGHTS,
    get_similarity_weights,
)


SUPPORTED_CRITERIA = (
    "pearson",
    "mean_abs_diff",
    "slope",
    "dtw",
    "structural",
)


def _as_float_array(pattern: np.ndarray) -> np.ndarray:
    """
    تبدیل الگو به آرایه float و بررسی معتبر بودن داده‌ها.
    """
    array = np.asarray(pattern, dtype=float)

    if array.size == 0:
        raise ValueError("الگوی ورودی خالی است.")

    if not np.all(np.isfinite(array)):
        raise ValueError("الگوی ورودی شامل مقدار نامعتبر است.")

    return array


def _prepare_weights(
    weights: Optional[Dict] = None,
) -> Dict[str, float]:
    """
    آماده‌سازی، اعتبارسنجی و نرمال‌سازی وزن‌ها.

    خروجی فقط شامل معیارهای فعال با وزن مثبت است و مجموع وزن‌ها
    همیشه برابر 1 خواهد بود.
    """
    if weights is None:
        try:
            weights = get_similarity_weights()
        except Exception:
            weights = None

    if not isinstance(weights, dict):
        weights = {}

    active_weights = {}

    for criterion in SUPPORTED_CRITERIA:
        raw_value = weights.get(criterion, 0.0)

        # پشتیبانی از ساختار کامل تنظیمات:
        # {"pearson": {"enabled": true, "weight": 0.35}}
        if isinstance(raw_value, dict):
            if not raw_value.get("enabled", True):
                continue
            raw_value = raw_value.get("weight", 0.0)

        try:
            weight = float(raw_value)
        except (TypeError, ValueError):
            continue

        if not np.isfinite(weight) or weight <= 0.0:
            continue

        active_weights[criterion] = weight

    # اگر وزن معتبر و فعالی وجود نداشت، از تنظیمات پیش‌فرض استفاده می‌شود.
    if not active_weights:
        for criterion, config in DEFAULT_SIMILARITY_WEIGHTS.items():
            if criterion not in SUPPORTED_CRITERIA:
                continue

            if not config.get("enabled", True):
                continue

            try:
                weight = float(config.get("weight", 0.0))
            except (TypeError, ValueError):
                continue

            if np.isfinite(weight) and weight > 0.0:
                active_weights[criterion] = weight

    total_weight = sum(active_weights.values())

    if total_weight <= 0.0:
        return {}

    return {
        criterion: weight / total_weight
        for criterion, weight in active_weights.items()
    }


def pearson_similarity(
    pattern1: np.ndarray,
    pattern2: np.ndarray,
) -> float:
    """
    محاسبه شباهت پیرسون بین دو الگو.

    خروجی بین صفر و یک است. همبستگی منفی، صفر در نظر گرفته می‌شود.
    """
    p1 = _as_float_array(pattern1).reshape(-1)
    p2 = _as_float_array(pattern2).reshape(-1)

    if p1.size != p2.size or p1.size < 2:
        return 0.0

    p1_std = float(np.std(p1))
    p2_std = float(np.std(p2))

    # برای الگوهای کاملاً ثابت، Pearson قابل محاسبه نیست.
    if p1_std < 1e-12 or p2_std < 1e-12:
        return 1.0 if np.allclose(p1, p2) else 0.0

    correlation = float(np.corrcoef(p1, p2)[0, 1])

    if not np.isfinite(correlation):
        return 0.0

    return float(np.clip(correlation, 0.0, 1.0))


def mean_abs_diff_similarity(
    pattern1: np.ndarray,
    pattern2: np.ndarray,
) -> float:
    """
    شباهت بر اساس میانگین قدرمطلق اختلاف.

    برای داده‌های z-score شده، اختلاف 2.0 یا بیشتر معادل شباهت صفر
    در نظر گرفته می‌شود. خروجی بین صفر و یک است.
    """
    p1 = _as_float_array(pattern1)
    p2 = _as_float_array(pattern2)

    if p1.shape != p2.shape:
        return 0.0

    difference = float(np.mean(np.abs(p1 - p2)))
    normalized_difference = min(difference, 2.0) / 2.0

    return float(np.clip(1.0 - normalized_difference, 0.0, 1.0))


def slope_similarity(
    pattern1: np.ndarray,
    pattern2: np.ndarray,
) -> float:
    """
    شباهت شیب و جهت حرکت بین دو الگو.

    از شباهت کسینوسی تغییرات متوالی استفاده می‌شود.
    خروجی بین صفر و یک است.
    """
    p1 = _as_float_array(pattern1)
    p2 = _as_float_array(pattern2)

    if p1.shape != p2.shape or p1.shape[0] < 2:
        return 0.0

    slopes1 = np.diff(p1, axis=0).reshape(-1)
    slopes2 = np.diff(p2, axis=0).reshape(-1)

    norm1 = float(np.linalg.norm(slopes1))
    norm2 = float(np.linalg.norm(slopes2))

    # اگر هر دو الگو بدون تغییر باشند، از نظر شیب مشابه‌اند.
    if norm1 < 1e-12 and norm2 < 1e-12:
        return 1.0

    if norm1 < 1e-12 or norm2 < 1e-12:
        return 0.0

    cosine_similarity = float(
        np.dot(slopes1, slopes2) / (norm1 * norm2)
    )

    return float(
        np.clip((cosine_similarity + 1.0) / 2.0, 0.0, 1.0)
    )


def combined_similarity(
    pattern1: np.ndarray,
    pattern2: np.ndarray,
    weights: Optional[Dict] = None,
) -> float:
    """
    محاسبه امتیاز ترکیبی معیارهای شباهت.

    اگر ``weights`` برابر None باشد، وزن‌های فعلی از
    ``app_settings.json`` خوانده می‌شوند.

    برای کارایی بهتر در تحلیل‌های حجیم، توصیه می‌شود وزن‌ها یک‌بار
    خوانده شده و به این تابع ارسال شوند؛ PatternMatcher این کار را
    به‌صورت خودکار انجام می‌دهد.
    """
    p1 = _as_float_array(pattern1)
    p2 = _as_float_array(pattern2)

    if p1.shape != p2.shape or p1.shape[0] < 2:
        return 0.0

    normalized_weights = _prepare_weights(weights)

    if not normalized_weights:
        return 0.0

    scores = {}

    # فقط معیارهای فعال محاسبه می‌شوند.
    if "pearson" in normalized_weights:
        scores["pearson"] = pearson_similarity(p1, p2)

    if "mean_abs_diff" in normalized_weights:
        scores["mean_abs_diff"] = mean_abs_diff_similarity(p1, p2)

    if "slope" in normalized_weights:
        scores["slope"] = slope_similarity(p1, p2)

    if "dtw" in normalized_weights:
        from .dtw import dtw_similarity

        try:
            scores["dtw"] = float(dtw_similarity(p1, p2))
        except Exception:
            scores["dtw"] = 0.0

    if "structural" in normalized_weights:
        from .pattern_detector import structural_similarity

        try:
            scores["structural"] = float(
                structural_similarity(p1, p2)
            )
        except Exception:
            scores["structural"] = 0.0

    total_score = 0.0

    for criterion, weight in normalized_weights.items():
        score = scores.get(criterion, 0.0)

        if not np.isfinite(score):
            score = 0.0

        total_score += weight * float(
            np.clip(score, 0.0, 1.0)
        )

    return float(np.clip(total_score, 0.0, 1.0))