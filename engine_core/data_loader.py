"""
بارگذاری فایل‌های CSV بازار.
"""

from pathlib import Path
from typing import Tuple

import numpy as np
import pandas as pd


def _normalize_column_name(column) -> str:
    """
    یکسان‌سازی نام ستون‌ها، از جمله فرمت‌های MetaTrader مانند <CLOSE>.
    """
    return (
        str(column)
        .strip()
        .replace("\ufeff", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("_", "")
        .lower()
    )


def _read_csv(file_path: Path, header="infer") -> pd.DataFrame:
    """
    خواندن CSV با تشخیص جداکننده و چند encoding متداول.
    """
    encodings = (
        "utf-8-sig",
        "utf-8",
        "cp1256",
        "cp1252",
        "latin1",
    )

    last_error = None

    for encoding in encodings:
        try:
            return pd.read_csv(
                file_path,
                sep=None,
                engine="python",
                encoding=encoding,
                header=header,
            )
        except UnicodeDecodeError as error:
            last_error = error
        except Exception as error:
            last_error = error

    raise ValueError(
        f"خواندن فایل امکان‌پذیر نیست:\n{file_path}\n\n{last_error}"
    )


def _find_column(df: pd.DataFrame, possible_names) -> str | None:
    normalized = {
        _normalize_column_name(column): column
        for column in df.columns
    }

    for name in possible_names:
        key = _normalize_column_name(name)

        if key in normalized:
            return normalized[key]

    return None


def _load_headerless_csv(file_path: Path) -> pd.DataFrame:
    """
    خواندن فایل بدون سرستون.

    فرمت‌های پشتیبانی‌شده:
    datetime, open, high, low, close

    date, time, open, high, low, close
    """
    raw_df = _read_csv(file_path, header=None)

    if raw_df.shape[1] < 5:
        raise ValueError(
            "فایل CSV باید حداقل پنج ستون شامل زمان و قیمت‌های OHLC داشته باشد."
        )

    if raw_df.shape[1] == 5:
        raw_df = raw_df.iloc[:, :5].copy()
        raw_df.columns = [
            "DateTime",
            "Open",
            "High",
            "Low",
            "Close",
        ]
    else:
        selected = raw_df.iloc[:, :6].copy()
        selected.columns = [
            "Date",
            "Time",
            "Open",
            "High",
            "Low",
            "Close",
        ]
        raw_df = selected

    return raw_df


def load_csv_data(file_path: str) -> Tuple[np.ndarray, pd.DataFrame]:
    """
    بارگذاری فایل داده بازار.

    خروجی:
        prices:
            آرایه ستونی قیمت Close با شکل (تعداد ردیف‌ها، 1)

        df:
            DataFrame پاک‌سازی‌شده برای استخراج زمان و سایر اطلاعات
    """
    path = Path(file_path)

    if not path.exists():
        raise FileNotFoundError(f"فایل پیدا نشد:\n{path}")

    if not path.is_file():
        raise ValueError(f"مسیر انتخاب‌شده فایل نیست:\n{path}")

    if path.stat().st_size == 0:
        raise ValueError(f"فایل خالی است:\n{path.name}")

    df = _read_csv(path)

    close_column = _find_column(
        df,
        ("Close", "ClosingPrice", "Last"),
    )

    if close_column is None:
        df = _load_headerless_csv(path)
        close_column = "Close"

    rename_map = {}

    column_aliases = {
        "Open": ("Open",),
        "High": ("High",),
        "Low": ("Low",),
        "Close": ("Close", "ClosingPrice", "Last"),
        "Date": ("Date",),
        "Time": ("Time",),
        "DateTime": ("DateTime", "Datetime", "Timestamp"),
    }

    for standard_name, aliases in column_aliases.items():
        found = _find_column(df, aliases)

        if found is not None:
            rename_map[found] = standard_name

    df = df.rename(columns=rename_map)

    if "Close" not in df.columns:
        raise ValueError(
            f"ستون Close در فایل {path.name} پیدا نشد."
        )

    df["Close"] = pd.to_numeric(
        df["Close"],
        errors="coerce",
    )

    df = df.replace(
        [np.inf, -np.inf],
        np.nan,
    )

    df = df.dropna(
        subset=["Close"],
    ).reset_index(drop=True)

    if len(df) < 10:
        raise ValueError(
            f"داده معتبر کافی در فایل وجود ندارد:\n"
            f"{path.name}\n"
            f"تعداد قیمت Close معتبر: {len(df)}"
        )

    prices = df["Close"].to_numpy(
        dtype=np.float64,
    ).reshape(-1, 1)

    return prices, df