"""
engine_core package
هسته اصلی موتور تشخیص الگوهای Chart DNA
"""

import csv
from pathlib import Path
from typing import Callable, Dict, List, Optional

import numpy as np

from core.config import OUTPUT_DIR

from .data_loader import load_csv_data
from .dtw import dtw_distance, dtw_similarity
from .matcher import PatternMatcher
from .normalizer import (
    normalize_shape,
    resample_series,
    zscore_normalize,
)
from .pattern_detector import (
    find_peaks_and_valleys,
    key_turning_points,
    structural_similarity,
)
from .similarity import (
    combined_similarity,
    mean_abs_diff_similarity,
    pearson_similarity,
    slope_similarity,
)


# =========================================================
# تنظیمات موقت برای جلوگیری از هنگ روی فایل‌های خیلی بزرگ
# بعداً می‌توانیم این‌ها را از صفحه تنظیمات بخوانیم.
# =========================================================
DEFAULT_MAX_WINDOWS_PER_FILE = 20000
DEFAULT_MATCHER_PROGRESS_EVERY = 500
DEFAULT_INTERNAL_TOP_PER_FILE = 500


def _build_timestamps(df) -> np.ndarray:
    """
    استخراج زمان از DataFrame.
    """
    if "DateTime" in df.columns:
        return df["DateTime"].astype(str).to_numpy()

    if "Timestamp" in df.columns:
        return df["Timestamp"].astype(str).to_numpy()

    if "Date" in df.columns and "Time" in df.columns:
        return (
            df["Date"].astype(str)
            + " "
            + df["Time"].astype(str)
        ).to_numpy()

    if "Time" in df.columns:
        return df["Time"].astype(str).to_numpy()

    if "Date" in df.columns:
        return df["Date"].astype(str).to_numpy()

    return np.arange(
        len(df),
    ).astype(str)


def _safe_progress(
    progress_callback: Optional[Callable[[str, float], None]],
    text: str,
    percent: float,
):
    """
    ارسال امن پیام پیشرفت به UI.
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
        pass


def save_results_csv(
    results: List[Dict],
) -> Path:
    """
    ذخیره نتایج تحلیل در فایل CSV.
    """
    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    output_path = (
        OUTPUT_DIR
        / "chart_dna_matches.csv"
    )

    headers = [
        "rank",
        "file_name",
        "start_index",
        "end_index",
        "start_time",
        "end_time",
        "similarity_percent",
        "future_prices",
    ]

    with open(
        output_path,
        "w",
        newline="",
        encoding="utf-8-sig",
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=headers,
        )

        writer.writeheader()

        for rank, result in enumerate(
            results,
            start=1,
        ):
            future_text = "|".join(
                f"{float(value):.8f}"
                for value in result.get(
                    "future",
                    [],
                )
            )

            writer.writerow(
                {
                    "rank": rank,
                    "file_name": result.get(
                        "file_name",
                        "",
                    ),
                    "start_index": result.get(
                        "start_index",
                        "",
                    ),
                    "end_index": result.get(
                        "end_index",
                        "",
                    ),
                    "start_time": result.get(
                        "start_time",
                        "",
                    ),
                    "end_time": result.get(
                        "end_time",
                        "",
                    ),
                    "similarity_percent": result.get(
                        "similarity",
                        0,
                    ),
                    "future_prices": future_text,
                }
            )

    return output_path


def analyze_files(
    reference_pattern: np.ndarray,
    csv_files: List[str],
    threshold: float,
    future_candles: int,
    pattern_length: int,
    top_results: int = 50,
    progress_callback: Optional[
        Callable[[str, float], None]
    ] = None,
) -> List[Dict]:
    """
    اجرای تحلیل روی یک یا چند فایل CSV.

    نسخه بهینه‌شده:
    - ارسال پیشرفت داخل هر فایل
    - محدود کردن تعداد پنجره‌های بررسی‌شده برای جلوگیری از هنگ
    - محدود کردن تعداد نتایج داخلی هر فایل
    """
    reference = np.asarray(
        reference_pattern,
        dtype=float,
    ).reshape(-1)

    csv_files = list(
        csv_files or []
    )

    threshold = float(threshold)
    future_candles = int(future_candles)
    pattern_length = int(pattern_length)
    top_results = int(top_results)

    if reference.size < 10:
        raise ValueError(
            "الگوی استخراج‌شده از تصویر بسیار کوتاه است."
        )

    if not np.all(np.isfinite(reference)):
        raise ValueError(
            "الگوی مرجع شامل مقدار نامعتبر است."
        )

    if not csv_files:
        raise ValueError(
            "حداقل یک فایل CSV انتخاب کنید."
        )

    if not 0 <= threshold <= 100:
        raise ValueError(
            "حداقل شباهت باید بین صفر تا صد باشد."
        )

    if future_candles < 0:
        raise ValueError(
            "تعداد کندل‌های آینده نمی‌تواند منفی باشد."
        )

    if pattern_length < 2:
        raise ValueError(
            "طول الگو باید حداقل دو کندل باشد."
        )

    if top_results < 1:
        raise ValueError(
            "تعداد نتایج باید حداقل یک باشد."
        )

    # تبدیل الگوی تصویری به طول مورد استفاده در داده بازار
    reference = resample_series(
        reference,
        pattern_length,
    )

    reference = normalize_shape(
        reference,
    ).reshape(-1, 1)

    matcher = PatternMatcher(
        min_similarity=threshold / 100.0,
        future_candles=future_candles,
    )

    all_results = []
    errors = []
    total_files = len(csv_files)

    _safe_progress(
        progress_callback,
        "شروع تحلیل...",
        0,
    )

    # تعداد نتیجه‌ای که از هر فایل به صورت داخلی نگهداری می‌شود.
    # بهتر است از top_results بیشتر باشد تا بعد از ترکیب فایل‌ها بهترین‌ها حذف نشوند.
    internal_top_per_file = max(
        DEFAULT_INTERNAL_TOP_PER_FILE,
        top_results * 10,
    )

    for file_index, csv_file in enumerate(
        csv_files,
        start=1,
    ):
        csv_path = Path(csv_file)

        file_start_percent = (
            (file_index - 1)
            / total_files
            * 100
        )

        file_end_percent = (
            file_index
            / total_files
            * 100
        )

        _safe_progress(
            progress_callback,
            (
                f"خواندن فایل {file_index} از "
                f"{total_files}: {csv_path.name}"
            ),
            file_start_percent,
        )

        try:
            prices, df = load_csv_data(
                str(csv_path)
            )

            required_length = (
                pattern_length
                + future_candles
            )

            if len(prices) < required_length:
                raise ValueError(
                    f"تعداد داده‌های فایل کمتر از "
                    f"{required_length} ردیف است."
                )

            timestamps = _build_timestamps(
                df
            )

            def file_progress(message: str, inner_percent: float):
                """
                تبدیل پیشرفت داخلی matcher از 0..100
                به بازه درصدی همین فایل در کل تحلیل.
                """
                try:
                    inner_percent_value = float(inner_percent)
                except (TypeError, ValueError):
                    inner_percent_value = 0.0

                inner_percent_value = max(
                    0.0,
                    min(inner_percent_value, 100.0),
                )

                mapped_percent = (
                    file_start_percent
                    + (
                        file_end_percent
                        - file_start_percent
                    )
                    * inner_percent_value
                    / 100.0
                )

                _safe_progress(
                    progress_callback,
                    message,
                    mapped_percent,
                )

            matches = matcher.find_matches(
                reference_prices=reference,
                all_prices=prices,
                timestamps=timestamps,
                progress_callback=file_progress,
                file_name=csv_path.name,
                max_windows=DEFAULT_MAX_WINDOWS_PER_FILE,
                scan_step=None,
                top_k=internal_top_per_file,
                progress_every=DEFAULT_MATCHER_PROGRESS_EVERY,
            )

            close_prices = prices.reshape(-1)

            for match in matches:
                start = int(
                    match["index"]
                )

                end_exclusive = (
                    start
                    + pattern_length
                )

                end_index = (
                    end_exclusive - 1
                )

                future_end = (
                    end_exclusive
                    + future_candles
                )

                pattern_values = close_prices[
                    start:end_exclusive
                ]

                future_values = close_prices[
                    end_exclusive:future_end
                ]

                result = {
                    "file": str(csv_path),
                    "file_name": csv_path.name,
                    "start_index": start,
                    "end_index": end_index,
                    "start_time": (
                        str(timestamps[start])
                        if start < len(timestamps)
                        else str(start)
                    ),
                    "end_time": (
                        str(timestamps[end_index])
                        if end_index < len(timestamps)
                        else str(end_index)
                    ),
                    "similarity": (
                        float(match["similarity"])
                        * 100.0
                    ),
                    "pattern": pattern_values.tolist(),
                    "future": future_values.tolist(),
                }

                all_results.append(
                    result
                )

            _safe_progress(
                progress_callback,
                (
                    f"فایل {csv_path.name}: "
                    f"{len(matches)} الگو برای بررسی نهایی نگهداری شد."
                ),
                file_end_percent,
            )

        except Exception as error:
            error_text = (
                f"{csv_path.name}: {error}"
            )

            errors.append(
                error_text
            )

            _safe_progress(
                progress_callback,
                f"خطا در فایل {error_text}",
                file_end_percent,
            )

    all_results.sort(
        key=lambda item: item["similarity"],
        reverse=True,
    )

    final_results = all_results[
        :top_results
    ]

    save_results_csv(
        final_results
    )

    if final_results:
        _safe_progress(
            progress_callback,
            (
                f"تحلیل کامل شد. "
                f"{len(final_results)} نتیجه نمایش داده می‌شود."
            ),
            100,
        )
    elif errors:
        _safe_progress(
            progress_callback,
            (
                "تحلیل تمام شد، اما نتیجه‌ای پیدا نشد. "
                + " | ".join(errors[:3])
            ),
            100,
        )
    else:
        _safe_progress(
            progress_callback,
            "تحلیل کامل شد؛ الگویی با شباهت انتخابی پیدا نشد.",
            100,
        )

    return final_results


__all__ = [
    "analyze_files",
    "save_results_csv",
    "zscore_normalize",
    "normalize_shape",
    "resample_series",
    "pearson_similarity",
    "combined_similarity",
    "slope_similarity",
    "mean_abs_diff_similarity",
    "load_csv_data",
    "PatternMatcher",
    "dtw_similarity",
    "dtw_distance",
    "find_peaks_and_valleys",
    "structural_similarity",
    "key_turning_points",
]