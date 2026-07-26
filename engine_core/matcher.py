"""
PatternMatcher: جستجوی الگوهای مشابه در سری قیمت.

نسخه بهینه‌شده:
- جلوگیری از اسکن میلیون‌ها پنجره با گام ۱
- پشتیبانی از progress_callback در طول تحلیل هر فایل
- محدود کردن تعداد نتایج نگهداری‌شده برای کاهش مصرف حافظه
"""

import heapq
import math
from typing import Callable, Dict, List, Optional

import numpy as np

from core.config import get_similarity_weights

from .normalizer import zscore_normalize
from .similarity import combined_similarity


class PatternMatcher:
    """
    موتور جستجوی الگوهای مشابه.

    وزن‌های معیارها در زمان ساخت موتور خوانده می‌شوند؛ بنابراین در
    تمام پنجره‌های یک تحلیل ثابت می‌مانند و فایل تنظیمات برای هر کندل
    مجدداً خوانده نمی‌شود.
    """

    def __init__(
        self,
        min_similarity: float = 0.7,
        future_candles: int = 5,
        weights: Optional[Dict] = None,
    ):
        self.min_similarity = float(min_similarity)
        self.future_candles = int(future_candles)

        if not 0.0 <= self.min_similarity <= 1.0:
            raise ValueError(
                "حداقل شباهت موتور باید بین صفر و یک باشد."
            )

        if self.future_candles < 0:
            raise ValueError(
                "تعداد کندل‌های آینده نمی‌تواند منفی باشد."
            )

        # اگر وزن‌ها داده نشده باشند، یک‌بار از app_settings.json
        # خوانده می‌شوند. این Snapshot در تمام جریان تحلیل استفاده می‌شود.
        if weights is None:
            weights = get_similarity_weights()

        if not isinstance(weights, dict):
            raise ValueError(
                "وزن معیارهای شباهت باید به‌صورت دیکشنری باشد."
            )

        self.weights = dict(weights)

    @staticmethod
    def _safe_progress(
        progress_callback: Optional[Callable[[str, float], None]],
        text: str,
        percent: float,
    ):
        """
        ارسال امن پیام پیشرفت.
        """
        if progress_callback is None:
            return

        try:
            percent = float(percent)
        except (TypeError, ValueError):
            percent = 0.0

        percent = max(0.0, min(percent, 100.0))

        try:
            progress_callback(text, percent)
        except Exception:
            # نباید خطای UI باعث توقف موتور تحلیل شود.
            pass

    @staticmethod
    def _calculate_scan_step(
        total_windows: int,
        max_windows: Optional[int],
        scan_step: Optional[int],
    ) -> int:
        """
        تعیین گام اسکن.

        اگر scan_step دستی داده شده باشد، همان استفاده می‌شود.
        اگر max_windows داده شده باشد و تعداد پنجره‌ها زیاد باشد،
        گام خودکار طوری تعیین می‌شود که تعداد بررسی‌ها از max_windows
        بیشتر نشود.
        """
        if total_windows <= 0:
            return 1

        if scan_step is not None:
            try:
                scan_step = int(scan_step)
            except (TypeError, ValueError):
                scan_step = 1

            return max(1, scan_step)

        if max_windows is None:
            return 1

        try:
            max_windows = int(max_windows)
        except (TypeError, ValueError):
            max_windows = 0

        if max_windows <= 0:
            return 1

        if total_windows <= max_windows:
            return 1

        return max(1, int(math.ceil(total_windows / max_windows)))

    def find_matches(
        self,
        reference_prices: np.ndarray,
        all_prices: np.ndarray,
        timestamps: Optional[np.ndarray] = None,
        progress_callback: Optional[Callable[[str, float], None]] = None,
        file_name: str = "",
        max_windows: Optional[int] = 20000,
        scan_step: Optional[int] = None,
        top_k: Optional[int] = 500,
        progress_every: int = 500,
    ) -> List[Dict]:
        """
        جستجوی پنجره‌های مشابه الگوی مرجع در داده‌های قیمت.

        پارامترها:
            reference_prices:
                الگوی مرجع با شکل (length,) یا (length, features).

            all_prices:
                کل سری قیمت با شکل (rows,) یا (rows, features).

            timestamps:
                زمان‌های متناظر با داده‌ها، در صورت نیاز.

            progress_callback:
                تابع دریافت پیشرفت داخلی همین فایل.
                درصد از 0 تا 100 است.

            file_name:
                نام فایل برای نمایش در متن پیشرفت.

            max_windows:
                حداکثر تعداد پنجره‌ای که از هر فایل بررسی می‌شود.
                برای جلوگیری از هنگ روی فایل‌های خیلی بزرگ.

            scan_step:
                گام اسکن دستی. اگر None باشد، بر اساس max_windows
                خودکار محاسبه می‌شود.

            top_k:
                حداکثر تعداد نتیجه‌ای که داخل همین فایل نگهداری می‌شود.
                اگر None باشد همه نتایج معتبر نگهداری می‌شوند.

            progress_every:
                هر چند پنجره یک‌بار پیام پیشرفت ارسال شود.

        خروجی:
            لیستی مرتب‌شده از بیشترین شباهت به کمترین شباهت.
        """
        reference = np.asarray(
            reference_prices,
            dtype=float,
        )

        prices = np.asarray(
            all_prices,
            dtype=float,
        )

        if reference.ndim == 1:
            reference = reference.reshape(-1, 1)

        if prices.ndim == 1:
            prices = prices.reshape(-1, 1)

        if reference.ndim != 2 or prices.ndim != 2:
            raise ValueError(
                "الگوی مرجع و داده قیمت باید یک یا دو بعدی باشند."
            )

        if reference.shape[1] != prices.shape[1]:
            raise ValueError(
                "تعداد ویژگی‌های الگوی مرجع با داده بازار یکسان نیست."
            )

        if not np.all(np.isfinite(reference)):
            raise ValueError(
                "الگوی مرجع شامل مقدار نامعتبر است."
            )

        ref_len = reference.shape[0]
        total_len = prices.shape[0]

        if ref_len < 2:
            raise ValueError(
                "طول الگوی مرجع باید حداقل دو نقطه باشد."
            )

        last_start = (
            total_len
            - ref_len
            - self.future_candles
        )

        if last_start < 0:
            return []

        total_windows = last_start + 1

        step = self._calculate_scan_step(
            total_windows=total_windows,
            max_windows=max_windows,
            scan_step=scan_step,
        )

        total_checks = ((last_start // step) + 1)

        if progress_every is None:
            progress_every = 500

        try:
            progress_every = int(progress_every)
        except (TypeError, ValueError):
            progress_every = 500

        progress_every = max(1, progress_every)

        # الگوی مرجع فقط یک‌بار نرمال می‌شود.
        reference_normalized = zscore_normalize(
            reference
        )

        timestamp_values = None

        if timestamps is not None:
            timestamp_values = np.asarray(
                timestamps,
            ).reshape(-1)

        title = file_name or "فایل جاری"

        self._safe_progress(
            progress_callback,
            (
                f"{title}: شروع مقایسه "
                f"{total_checks:,} پنجره از مجموع {total_windows:,} پنجره "
                f"(گام اسکن: {step})"
            ),
            0,
        )

        # اگر top_k فعال باشد، به جای ذخیره همه نتایج، فقط بهترین‌ها نگهداری می‌شوند.
        use_heap = top_k is not None

        if use_heap:
            try:
                top_k = int(top_k)
            except (TypeError, ValueError):
                top_k = 500

            top_k = max(1, top_k)
            results_heap = []
            results = None
        else:
            results_heap = None
            results = []

        checked = 0
        matched_count = 0

        for start in range(0, last_start + 1, step):
            checked += 1
            end = start + ref_len

            window = prices[start:end]

            if not np.all(np.isfinite(window)):
                if checked % progress_every == 0:
                    percent = checked / total_checks * 100.0
                    self._safe_progress(
                        progress_callback,
                        (
                            f"{title}: بررسی {checked:,} از "
                            f"{total_checks:,} پنجره..."
                        ),
                        percent,
                    )
                continue

            window_normalized = zscore_normalize(
                window
            )

            similarity = float(
                combined_similarity(
                    reference_normalized,
                    window_normalized,
                    weights=self.weights,
                )
            )

            if not np.isfinite(similarity):
                if checked % progress_every == 0:
                    percent = checked / total_checks * 100.0
                    self._safe_progress(
                        progress_callback,
                        (
                            f"{title}: بررسی {checked:,} از "
                            f"{total_checks:,} پنجره..."
                        ),
                        percent,
                    )
                continue

            similarity = float(
                np.clip(similarity, 0.0, 1.0)
            )

            if similarity >= self.min_similarity:
                matched_count += 1

                entry = {
                    "index": start,
                    "similarity": similarity,
                    "future_start": end,
                    "future_end": end + self.future_candles,
                }

                if (
                    timestamp_values is not None
                    and start < len(timestamp_values)
                ):
                    entry["time"] = str(
                        timestamp_values[start]
                    )

                if use_heap:
                    item = (
                        similarity,
                        start,
                        entry,
                    )

                    if len(results_heap) < top_k:
                        heapq.heappush(
                            results_heap,
                            item,
                        )
                    else:
                        if item[0] > results_heap[0][0]:
                            heapq.heapreplace(
                                results_heap,
                                item,
                            )
                else:
                    results.append(
                        entry
                    )

            if checked % progress_every == 0:
                percent = checked / total_checks * 100.0
                self._safe_progress(
                    progress_callback,
                    (
                        f"{title}: بررسی {checked:,} از {total_checks:,} "
                        f"پنجره | نتایج معتبر: {matched_count:,}"
                    ),
                    percent,
                )

        self._safe_progress(
            progress_callback,
            (
                f"{title}: مقایسه کامل شد. "
                f"{matched_count:,} نتیجه معتبر پیدا شد."
            ),
            100,
        )

        if use_heap:
            final_results = [
                item[2]
                for item in sorted(
                    results_heap,
                    key=lambda value: value[0],
                    reverse=True,
                )
            ]
        else:
            final_results = results

            final_results.sort(
                key=lambda item: item["similarity"],
                reverse=True,
            )

        return final_results