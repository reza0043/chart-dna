"""
توابع نرمال‌سازی و تغییر طول سری‌های قیمتی.
"""

import numpy as np


def zscore_normalize(prices: np.ndarray) -> np.ndarray:
    """
    نرمال‌سازی Z-score.

    ورودی یک‌بعدی به آرایه ستونی تبدیل می‌شود تا تمام بخش‌های
    موتور فرمت یکسانی داشته باشند.
    """
    values = np.asarray(prices, dtype=float)

    if values.ndim == 1:
        values = values.reshape(-1, 1)

    if values.size == 0:
        return values.copy()

    means = np.mean(values, axis=0)
    stds = np.std(values, axis=0)

    stds = np.where(stds < 1e-12, 1.0, stds)

    return (values - means) / stds


def normalize_shape(values: np.ndarray) -> np.ndarray:
    """
    نرمال‌سازی مسیر قیمت نسبت به نقطه شروع.

    این روش جهت و شکل حرکت قیمت را حفظ می‌کند و وابستگی
    الگو به سطح مطلق قیمت را کاهش می‌دهد.
    """
    values = np.asarray(values, dtype=float).reshape(-1)

    if values.size == 0:
        return values.copy()

    values = values - values[0]

    scale = float(np.max(values) - np.min(values))

    if scale < 1e-12:
        scale = float(np.std(values))

    if scale < 1e-12:
        return np.zeros_like(values)

    return values / scale


def resample_series(
    values: np.ndarray,
    target_length: int = 80,
) -> np.ndarray:
    """
    تغییر طول سری زمانی با درون‌یابی خطی.
    """
    values = np.asarray(values, dtype=float).reshape(-1)
    target_length = int(target_length)

    if values.size < 2:
        raise ValueError("طول الگو برای تغییر اندازه کافی نیست.")

    if target_length < 2:
        raise ValueError("طول جدید الگو باید حداقل ۲ باشد.")

    if values.size == target_length:
        return values.copy()

    old_x = np.linspace(0.0, 1.0, values.size)
    new_x = np.linspace(0.0, 1.0, target_length)

    return np.interp(new_x, old_x, values)