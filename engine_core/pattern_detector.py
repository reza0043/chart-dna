"""
تشخیص نقاط برگشت (قله و کف) در الگوها
برای مقایسه ساختاری بهتر بین الگوها
"""
import numpy as np
from typing import List, Tuple, Dict


def find_peaks_and_valleys(prices: np.ndarray,
                           order: int = 2) -> Tuple[np.ndarray, np.ndarray]:
    """
    تشخیص قله‌ها و کف‌های محلی با روش مقایسه همسایگی.
    
    Args:
        prices: آرایه یک بعدی یا [n, features] (Close رو می‌گیره)
        order: تعداد کندل‌های قبل و بعد برای مقایسه (پیش‌فرض 2)
    
    Returns:
        peaks: ایندکس قله‌ها
        valleys: ایندکس کف‌ها
    """
    if prices.ndim == 2:
        # اگر چندبعدی بود، ستون Close رو می‌گیره
        if prices.shape[1] >= 4:
            prices = prices[:, 3]  # فرض: Close ستون چهارمه
        else:
            prices = prices[:, 0]
    
    n = len(prices)
    peaks = []
    valleys = []
    
    for i in range(order, n - order):
        window = prices[i - order : i + order + 1]
        center = prices[i]
        
        # قله: مرکز بزرگتر از همه همسایه‌ها
        if center == np.max(window) and center != np.min(window):
            peaks.append(i)
        
        # کف: مرکز کوچکتر از همه همسایه‌ها
        if center == np.min(window) and center != np.max(window):
            valleys.append(i)
    
    return np.array(peaks), np.array(valleys)


def structural_similarity(pattern1: np.ndarray,
                          pattern2: np.ndarray,
                          order: int = 2) -> float:
    """
    مقایسه ساختاری دو الگو بر اساس موقعیت قله‌ها و کف‌ها.
    هرچی تعداد و موقعیت قله/کف شبیه‌تر باشه، شباهت بیشتره.
    """
    if pattern1.ndim == 2:
        p1 = pattern1[:, 3] if pattern1.shape[1] >= 4 else pattern1[:, 0]
    else:
        p1 = pattern1
    
    if pattern2.ndim == 2:
        p2 = pattern2[:, 3] if pattern2.shape[1] >= 4 else pattern2[:, 0]
    else:
        p2 = pattern2
    
    peaks1, valleys1 = find_peaks_and_valleys(p1, order)
    peaks2, valleys2 = find_peaks_and_valleys(p2, order)
    
    # تعداد نقاط
    len1 = len(peaks1) + len(valleys1)
    len2 = len(peaks2) + len(valleys2)
    
    if len1 == 0 and len2 == 0:
        return 1.0  # هر دو بدون نقطه برگشت → کاملاً مشابه
    
    if len1 == 0 or len2 == 0:
        return 0.0  # یکی نقطه داره، یکی نداره → کاملاً متفاوت
    
    # نرمال‌سازی موقعیت‌ها
    n1 = len(p1)
    n2 = len(p2)
    
    # ترکیب قله‌ها و کف‌ها با برچسب
    points1 = [(idx / n1, 'peak') for idx in peaks1] + [(idx / n1, 'valley') for idx in valleys1]
    points2 = [(idx / n2, 'peak') for idx in peaks2] + [(idx / n2, 'valley') for idx in valleys2]
    
    # مرتب‌سازی
    points1.sort(key=lambda x: x[0])
    points2.sort(key=lambda x: x[0])
    
    # مقایسه
    max_len = max(len(points1), len(points2))
    min_len = min(len(points1), len(points2))
    
    if max_len == 0:
        return 1.0
    
    # امتیاز: نسبت تطابق نوع نقطه
    match_score = 0.0
    for i in range(min_len):
        if points1[i][1] == points2[i][1]:
            match_score += 1.0 / max_len
    
    # جریمه برای تفاوت تعداد
    count_penalty = 1.0 - abs(len1 - len2) / max(len1, len2 + 1)
    
    return match_score * 0.7 + count_penalty * 0.3


def key_turning_points(prices: np.ndarray,
                       num_points: int = 5) -> np.ndarray:
    """
    استخراج نقاط کلیدی (نقاط برگشت اصلی) با روش حذف تدریجی.
    
    Args:
        prices: آرایه قیمت
        num_points: تعداد نقاط برگشت مورد نظر
    
    Returns:
        ایندکس نقاط کلیدی
    """
    if prices.ndim == 2:
        if prices.shape[1] >= 4:
            p = prices[:, 3].copy()
        else:
            p = prices[:, 0].copy()
    else:
        p = prices.copy()
    
    n = len(p)
    indices = np.array([0, n - 1])
    
    while len(indices) < num_points + 2:
        max_dist = -1
        max_idx = -1
        
        for i in range(1, n - 1):
            # فاصله عمودی تا خط بین دو نقطه مجاور
            left = indices[indices <= i][-1]
            right = indices[indices >= i][0]
            
            if left == right:
                continue
            
            # خط بین left و right
            x1, y1 = left, p[left]
            x2, y2 = right, p[right]
            
            if x2 == x1:
                continue
            
            # فاصله عمودی
            y_line = y1 + (y2 - y1) * (i - x1) / (x2 - x1)
            dist = abs(p[i] - y_line)
            
            if dist > max_dist:
                max_dist = dist
                max_idx = i
        
        if max_idx == -1:
            break
        
        indices = np.sort(np.append(indices, max_idx))
        
        if max_dist < 0.001:  # threshold
            break
    
    return indices