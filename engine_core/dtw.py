"""
DTW (Dynamic Time Warping) - مقایسه الگوهای با طول متفاوت یا جابجایی زمانی

نسخه بهینه‌شده:
- ماتریس فاصله بین تمام نقاط دو توالی یک‌بار و به‌صورت برداری (vectorized)
  با numpy محاسبه می‌شود؛ نسخه‌ی قبلی این فاصله را داخل حلقه‌ی تودرتوی
  پایتون و برای هر سلول جداگانه با numpy محاسبه می‌کرد که به‌خاطر سربار
  فراخوانی numpy روی آرایه‌های بسیار کوچک، بسیار کند بود (علت اصلی هنگ
  کردن برنامه هنگام تحلیل فایل‌های بزرگ).
- حلقه‌ی برنامه‌نویسی پویا (DP) که ذاتاً پی‌درپی است، حالا فقط روی
  اعداد از‌پیش‌محاسبه‌شده کار می‌کند، نه روی آرایه‌های numpy؛ این کار
  ده‌ها برابر سریع‌تر است.
- خروجی و رفتار دقیقاً مثل نسخه‌ی قبلی است؛ فقط سرعت اجرا تغییر کرده.
"""
import numpy as np
from typing import Tuple


def _pairwise_distance_matrix(seq1: np.ndarray, seq2: np.ndarray) -> np.ndarray:
    """
    محاسبه‌ی یک‌جای فاصله‌ی اقلیدسی بین تمام جفت نقاط دو توالی.

    خروجی آرایه‌ای به شکل (n, m) است که سلول [i, j] فاصله‌ی نقطه‌ی i از
    seq1 تا نقطه‌ی j از seq2 است. این کار با broadcasting در یک فراخوانی
    numpy انجام می‌شود، به‌جای صدها/هزاران فراخوانی جداگانه.
    """
    diff = seq1[:, np.newaxis, :] - seq2[np.newaxis, :, :]
    return np.sqrt(np.sum(diff * diff, axis=2))


def _dtw_cost_matrix(seq1: np.ndarray, seq2: np.ndarray) -> np.ndarray:
    """
    ساخت ماتریس هزینه‌ی DTW با استفاده از ماتریس فاصله‌ی از‌پیش‌محاسبه‌شده.
    """
    n, m = len(seq1), len(seq2)
    distances = _pairwise_distance_matrix(seq1, seq2)

    dtw = np.full((n + 1, m + 1), np.inf)
    dtw[0, 0] = 0.0

    # حلقه‌ی DP ذاتاً پی‌درپی است (هر سلول به سه همسایه‌ی قبلی خود نیاز
    # دارد) و نمی‌تواند به‌طور کامل برداری شود؛ اما چون دیگر داخل آن هیچ
    # فراخوانی numpy‌ای نداریم (فقط اعداد float ساده)، بسیار سریع است.
    for i in range(1, n + 1):
        row_distances = distances[i - 1]
        dtw_row = dtw[i]
        dtw_prev_row = dtw[i - 1]

        for j in range(1, m + 1):
            cost = row_distances[j - 1]
            dtw_row[j] = cost + min(
                dtw_prev_row[j],      # insert
                dtw_row[j - 1],       # delete
                dtw_prev_row[j - 1],  # match
            )

    return dtw


def dtw_distance(seq1: np.ndarray, seq2: np.ndarray) -> float:
    """
    محاسبه فاصله DTW بین دو توالی.

    Args:
        seq1: آرایه [n, features]
        seq2: آرایه [m, features]

    Returns:
        float: فاصله DTW (هرچی کمتر بهتر)
    """
    n, m = len(seq1), len(seq2)
    dtw = _dtw_cost_matrix(seq1, seq2)
    return dtw[n, m]


def dtw_similarity(seq1: np.ndarray, seq2: np.ndarray,
                   max_distance: float = 5.0) -> float:
    """
    تبدیل فاصله DTW به شباهت (0 تا 1).
    1 یعنی کاملاً مشابه.
    """
    dist = dtw_distance(seq1, seq2)
    # نرمال‌سازی
    normalized_dist = min(dist, max_distance) / max_distance
    return max(0.0, 1.0 - normalized_dist)


def dtw_alignment(seq1: np.ndarray, seq2: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
    """
    مسیر alignment بین دو توالی (برای نمایش)
    """
    n, m = len(seq1), len(seq2)
    dtw = _dtw_cost_matrix(seq1, seq2)
    distances = _pairwise_distance_matrix(seq1, seq2)

    # Backtracking
    i, j = n, m
    path1, path2 = [], []

    while i > 0 and j > 0:
        path1.append(i - 1)
        path2.append(j - 1)

        if i == 1 and j == 1:
            break

        cost = dtw[i, j] - distances[i - 1, j - 1]

        if dtw[i - 1, j] == cost:
            i -= 1
        elif dtw[i, j - 1] == cost:
            j -= 1
        else:
            i -= 1
            j -= 1

    return np.array(path1[::-1]), np.array(path2[::-1])
