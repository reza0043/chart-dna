import csv
import os
import re
import sys
from collections import OrderedDict
from datetime import datetime, timedelta
from pathlib import Path


# =========================================================
# تنظیم UTF-8 برای جلوگیری از خطای UnicodeEncodeError در ویندوز
# =========================================================
def configure_console_encoding():
    """
    جلوگیری از خطای زیر در ویندوز:
    UnicodeEncodeError: 'charmap' codec can't encode characters

    برنامه ممکن است توسط app.py اجرا شود و خروجی آن به pipe هدایت شود.
    در این شرایط encoding پیش‌فرض ویندوز گاهی cp1252 است و متن فارسی
    را پشتیبانی نمی‌کند.
    """
    for stream in (sys.stdout, sys.stderr):
        try:
            if hasattr(stream, "reconfigure"):
                stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass


configure_console_encoding()


# =========================================================
# تنظیمات مسیرها
# =========================================================
SCRIPT_DIR = Path(__file__).resolve().parent

BASE_DIR = Path(
    os.environ.get(
        "CHART_DNA_BASE_DIR",
        SCRIPT_DIR.parent,
    )
).resolve()

DATA_DIR = Path(
    os.environ.get(
        "CHART_DNA_DATA_DIR",
        BASE_DIR / "data",
    )
).resolve()

SELECTED_SYMBOL = os.environ.get("CHART_DNA_SYMBOL", "").strip().upper()

VALID_EXTENSIONS = {".csv", ".txt"}

TIMEFRAMES = OrderedDict(
    [
        ("M1", 1),
        ("M5", 5),
        ("M15", 15),
        ("M30", 30),
        ("H1", 60),
        ("H4", 240),
        ("D1", 1440),
        ("W1", 10080),
    ]
)


# =========================================================
# ابزارهای عمومی
# =========================================================
def print_status(message):
    """
    نمایش پیام در ترمینال و قابل دریافت توسط app.py.

    حتی اگر encoding خروجی ویندوز مناسب فارسی نباشد، باعث توقف برنامه
    نخواهد شد.
    """
    text = str(message)

    try:
        sys.stdout.write(text + "\n")
        sys.stdout.flush()
        return
    except UnicodeEncodeError:
        pass
    except Exception:
        pass

    # حالت اضطراری: اگر stdout فارسی را پشتیبانی نکرد، برنامه کرش نکند.
    try:
        safe_text = text.encode("ascii", errors="backslashreplace").decode("ascii")
        sys.stdout.write(safe_text + "\n")
        sys.stdout.flush()
    except Exception:
        pass


def clean_symbol_name(symbol):
    """پاک‌سازی نام نماد؛ مثال: XAUUSD"""
    symbol = str(symbol).strip().upper()
    symbol = re.sub(r"\s+", "", symbol)
    symbol = re.sub(r"[^A-Z0-9_.-]", "", symbol)
    return symbol


def safe_float(value):
    """
    تبدیل مقدار به عدد اعشاری.

    نمونه‌های قابل قبول:
    2350.25
    2350,25
    1,234.56
    1.234,56
    """
    if value is None:
        return None

    text = str(value).strip()

    if not text:
        return None

    text = text.replace("\ufeff", "")
    text = text.replace("\u00a0", "")
    text = text.replace(" ", "")
    text = text.replace("'", "")

    # فرمت 1.234,56
    if "," in text and "." in text:
        if text.rfind(",") > text.rfind("."):
            text = text.replace(".", "")
            text = text.replace(",", ".")
        else:
            # فرمت 1,234.56
            text = text.replace(",", "")

    # فرمت 1234,56
    elif "," in text and "." not in text:
        text = text.replace(",", ".")

    try:
        return float(text)
    except (TypeError, ValueError):
        return None


def format_price(value):
    """فرمت مناسب برای قیمت خروجی"""
    try:
        text = f"{float(value):.10f}".rstrip("0").rstrip(".")
        return "0" if text in {"", "-0"} else text
    except (TypeError, ValueError):
        return "0"


def normalize_header(value):
    """
    تبدیل نام ستون به حالت استاندارد.

    مثال:
    <DATE> -> date
    Date Time -> datetime
    """
    text = str(value).strip().lower()
    return re.sub(r"[^a-z0-9]", "", text)


# =========================================================
# تشخیص encoding و جداکننده فایل
# =========================================================
def detect_file_encoding(file_path):
    """
    تشخیص encoding مناسب برای CSV/TXT.

    در صورت نامشخص بودن فایل، latin1 انتخاب می‌شود؛ چون تقریباً هیچ‌وقت
    خطای decode نمی‌دهد.
    """
    encodings_to_try = [
        "utf-8-sig",
        "utf-8",
        "utf-16",
        "utf-16-le",
        "utf-16-be",
        "cp1256",
        "cp1252",
        "latin1",
    ]

    try:
        raw_data = Path(file_path).read_bytes()[:50000]

        if not raw_data:
            return "utf-8-sig"

        # BOM های شناخته‌شده
        if raw_data.startswith(b"\xef\xbb\xbf"):
            return "utf-8-sig"

        if raw_data.startswith(b"\xff\xfe") or raw_data.startswith(b"\xfe\xff"):
            return "utf-16"

        for encoding in encodings_to_try:
            try:
                raw_data.decode(encoding)
                return encoding
            except (UnicodeDecodeError, UnicodeError):
                continue

    except Exception:
        pass

    return "latin1"


def detect_delimiter(file_path, encoding=None):
    """
    تشخیص جداکننده فایل:
    comma , | semicolon ; | tab | pipe |
    """
    delimiters = [",", ";", "\t", "|"]

    if encoding is None:
        encoding = detect_file_encoding(file_path)

    try:
        with open(
            file_path,
            "r",
            encoding=encoding,
            errors="replace",
            newline="",
        ) as file:
            sample = file.read(20000)

        if not sample.strip():
            return ","

        try:
            dialect = csv.Sniffer().sniff(sample, delimiters=delimiters)
            return dialect.delimiter
        except csv.Error:
            pass

        lines = [line for line in sample.splitlines() if line.strip()][:20]

        if not lines:
            return ","

        scores = {}

        for delimiter in delimiters:
            scores[delimiter] = sum(line.count(delimiter) for line in lines)

        selected = max(scores, key=scores.get)

        if scores[selected] == 0:
            return ","

        return selected

    except Exception:
        return ","


def looks_like_header(row):
    """تشخیص اینکه ردیف اول، هدر ستون‌ها است یا خیر"""
    if not row:
        return False

    joined = " ".join(str(item).lower() for item in row)

    words = [
        "date",
        "time",
        "datetime",
        "timestamp",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "tick",
        "<date>",
        "<time>",
        "<open>",
        "<high>",
        "<low>",
        "<close>",
    ]

    return any(word in joined for word in words)


def parse_datetime(date_value, time_value=None):
    """
    تبدیل تاریخ و زمان به datetime.

    نمونه‌های قابل قبول:
    2024.01.15 12:30
    2024.01.15 12:30:00
    2024-01-15 12:30
    2024/01/15 12:30:00
    15.01.2024 12:30
    20240115 123000
    20240115 1230
    """
    if date_value is None:
        return None

    date_text = str(date_value).strip()

    if not date_text:
        return None

    date_text = date_text.replace("\ufeff", "")
    date_text = date_text.replace("T", " ")
    date_text = date_text.replace("Z", "")
    date_text = re.sub(r"\s+", " ", date_text).strip()

    if time_value is not None:
        time_text = str(time_value).strip()

        if time_text:
            date_text = f"{date_text} {time_text}"

    # اگر زمان دارای میلی‌ثانیه باشد، حذف می‌شود.
    date_text = re.sub(r"(\d{2}:\d{2}:\d{2})\.\d+", r"\1", date_text)

    formats = [
        "%Y.%m.%d %H:%M:%S",
        "%Y.%m.%d %H:%M",
        "%Y.%m.%d",
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M",
        "%Y-%m-%d",
        "%Y/%m/%d %H:%M:%S",
        "%Y/%m/%d %H:%M",
        "%Y/%m/%d",
        "%d.%m.%Y %H:%M:%S",
        "%d.%m.%Y %H:%M",
        "%d.%m.%Y",
        "%d-%m-%Y %H:%M:%S",
        "%d-%m-%Y %H:%M",
        "%d-%m-%Y",
        "%d/%m/%Y %H:%M:%S",
        "%d/%m/%Y %H:%M",
        "%d/%m/%Y",
        "%Y%m%d %H%M%S",
        "%Y%m%d %H%M",
        "%Y%m%d%H%M%S",
        "%Y%m%d%H%M",
        "%Y%m%d",
    ]

    for date_format in formats:
        try:
            return datetime.strptime(date_text, date_format)
        except ValueError:
            continue

    # احتمال Unix Timestamp؛ فقط برای اعداد بزرگ
    try:
        if re.fullmatch(r"\d{10,13}", date_text):
            timestamp = int(date_text)

            if len(date_text) == 13:
                timestamp = timestamp / 1000

            return datetime.fromtimestamp(timestamp)
    except (ValueError, OSError, OverflowError):
        pass

    return None


# =========================================================
# خواندن فایل داده
# =========================================================
def get_columns_from_header(header):
    """تعیین شماره ستون‌ها در فایل دارای سربرگ."""
    positions = {}

    for index, column_name in enumerate(header):
        normalized = normalize_header(column_name)

        if normalized in {
            "datetime",
            "dateandtime",
            "timestamp",
            "timestampvalue",
        }:
            positions["datetime"] = index

        elif normalized in {"date", "dt"}:
            positions["date"] = index

        elif normalized in {"time", "tm"}:
            positions["time"] = index

        elif normalized in {"open", "o", "openprice"}:
            positions["open"] = index

        elif normalized in {"high", "h", "highprice"}:
            positions["high"] = index

        elif normalized in {"low", "l", "lowprice"}:
            positions["low"] = index

        elif normalized in {"close", "c", "last", "closeprice"}:
            positions["close"] = index

    required = {"open", "high", "low", "close"}

    if not required.issubset(positions):
        return None

    if "datetime" not in positions and "date" not in positions:
        return None

    return positions


def parse_row_with_header(row, positions):
    """خواندن یک ردیف از فایل دارای سربرگ"""
    try:
        if "datetime" in positions:
            timestamp = parse_datetime(row[positions["datetime"]])
        else:
            date_value = row[positions["date"]]
            time_value = row[positions["time"]] if "time" in positions else None
            timestamp = parse_datetime(date_value, time_value)

        open_price = safe_float(row[positions["open"]])
        high_price = safe_float(row[positions["high"]])
        low_price = safe_float(row[positions["low"]])
        close_price = safe_float(row[positions["close"]])

        if (
            timestamp is None
            or open_price is None
            or high_price is None
            or low_price is None
            or close_price is None
        ):
            return None

        return (
            timestamp,
            open_price,
            high_price,
            low_price,
            close_price,
        )

    except (IndexError, KeyError, TypeError):
        return None


def parse_row_without_header(row):
    """
    خواندن فایل بدون هدر.

    فرمت پنج ستونی:
    DateTime,Open,High,Low,Close

    فرمت شش ستونی:
    Date,Time,Open,High,Low,Close
    """
    row = [str(item).strip() for item in row if str(item).strip() != ""]

    if len(row) < 5:
        return None

    # حالت شش ستونی:
    # Date | Time | Open | High | Low | Close
    if len(row) >= 6:
        timestamp = parse_datetime(row[0], row[1])
        open_price = safe_float(row[2])
        high_price = safe_float(row[3])
        low_price = safe_float(row[4])
        close_price = safe_float(row[5])

        if (
            timestamp is not None
            and open_price is not None
            and high_price is not None
            and low_price is not None
            and close_price is not None
        ):
            return (
                timestamp,
                open_price,
                high_price,
                low_price,
                close_price,
            )

    # حالت پنج ستونی:
    # DateTime | Open | High | Low | Close
    timestamp = parse_datetime(row[0])
    open_price = safe_float(row[1])
    high_price = safe_float(row[2])
    low_price = safe_float(row[3])
    close_price = safe_float(row[4])

    if (
        timestamp is None
        or open_price is None
        or high_price is None
        or low_price is None
        or close_price is None
    ):
        return None

    return (
        timestamp,
        open_price,
        high_price,
        low_price,
        close_price,
    )


def read_market_file(file_path):
    """
    خواندن یک فایل بازار.

    خروجی:
    [
        (datetime, open, high, low, close),
        ...
    ]
    """
    candles = []
    encoding = detect_file_encoding(file_path)
    delimiter = detect_delimiter(file_path, encoding)

    try:
        with open(
            file_path,
            "r",
            encoding=encoding,
            errors="replace",
            newline="",
        ) as file:
            reader = csv.reader(file, delimiter=delimiter)

            first_row = None

            for row in reader:
                cleaned = [str(value).strip() for value in row]

                if any(cleaned):
                    first_row = cleaned
                    break

            if first_row is None:
                return candles

            positions = None

            if looks_like_header(first_row):
                positions = get_columns_from_header(first_row)

            # فایل دارای سربرگ معتبر
            if positions is not None:
                for row in reader:
                    parsed = parse_row_with_header(row, positions)

                    if parsed is not None:
                        candles.append(parsed)

            # فایل بدون سربرگ
            else:
                parsed = parse_row_without_header(first_row)

                if parsed is not None:
                    candles.append(parsed)

                for row in reader:
                    parsed = parse_row_without_header(row)

                    if parsed is not None:
                        candles.append(parsed)

    except PermissionError:
        print_status(
            f"خطا: فایل باز است و قابل خواندن نیست. ابتدا آن را ببندید: {file_path}"
        )

    except FileNotFoundError:
        print_status(f"خطا: فایل پیدا نشد: {file_path}")

    except Exception as error:
        print_status(
            f"خطا در خواندن فایل {file_path.name}: "
            f"{type(error).__name__} | {error}"
        )

    return candles


# =========================================================
# مدیریت فایل‌های ورودی
# =========================================================
def is_generated_output_file(file_path, symbol):
    """تشخیص خروجی‌های خود برنامه تا دوباره به عنوان ورودی خوانده نشوند."""
    name = file_path.name.upper()

    for timeframe_name in TIMEFRAMES:
        expected_name = f"{symbol}_{timeframe_name}.CSV"

        if name == expected_name:
            return True

    return False


def find_source_files(symbol_dir):
    """
    فایل‌های M1 اصلی را پیدا می‌کند.

    اولویت:
    1) فایل CSV/TXT مستقیم داخل پوشه نماد
    2) فایل‌های CSV/TXT داخل پوشه M1
    """
    symbol = clean_symbol_name(symbol_dir.name)

    direct_files = []

    for item in symbol_dir.iterdir():
        if not item.is_file():
            continue

        if item.suffix.lower() not in VALID_EXTENSIONS:
            continue

        if is_generated_output_file(item, symbol):
            continue

        direct_files.append(item)

    if direct_files:
        return sorted(direct_files)

    m1_dir = symbol_dir / "M1"

    if not m1_dir.exists() or not m1_dir.is_dir():
        return []

    m1_files = []

    for item in m1_dir.iterdir():
        if not item.is_file():
            continue

        if item.suffix.lower() not in VALID_EXTENSIONS:
            continue

        if is_generated_output_file(item, symbol):
            continue

        m1_files.append(item)

    return sorted(m1_files)


def merge_and_deduplicate(candles):
    """
    مرتب‌سازی داده‌ها و حذف زمان‌های تکراری.

    اگر چند کندل با یک تاریخ و ساعت وجود داشته باشد،
    آخرین کندل خوانده‌شده نگه داشته می‌شود.
    """
    unique_candles = {}

    for timestamp, open_price, high_price, low_price, close_price in candles:
        timestamp = timestamp.replace(second=0, microsecond=0)

        unique_candles[timestamp] = (
            timestamp,
            open_price,
            high_price,
            low_price,
            close_price,
        )

    return [unique_candles[key] for key in sorted(unique_candles)]


# =========================================================
# ساخت تایم‌فریم‌ها
# =========================================================
def get_bucket_time(timestamp, timeframe_name, minutes):
    """تعیین شروع کندل تایم‌فریم موردنظر"""
    timestamp = timestamp.replace(second=0, microsecond=0)

    if timeframe_name == "W1":
        # شروع هفته: دوشنبه ساعت 00:00
        return timestamp - timedelta(
            days=timestamp.weekday(),
            hours=timestamp.hour,
            minutes=timestamp.minute,
        )

    if timeframe_name == "D1":
        return timestamp.replace(hour=0, minute=0)

    if minutes >= 60:
        hours = minutes // 60
        bucket_hour = (timestamp.hour // hours) * hours

        return timestamp.replace(hour=bucket_hour, minute=0)

    bucket_minute = (timestamp.minute // minutes) * minutes

    return timestamp.replace(minute=bucket_minute)


def resample_ohlc(candles, timeframe_name, minutes):
    """تبدیل کندل‌های M1 به تایم‌فریم بالاتر با ساختار OHLC."""
    if not candles:
        return []

    grouped = OrderedDict()

    for timestamp, open_price, high_price, low_price, close_price in candles:
        bucket = get_bucket_time(timestamp, timeframe_name, minutes)

        if bucket not in grouped:
            grouped[bucket] = {
                "time": bucket,
                "open": open_price,
                "high": high_price,
                "low": low_price,
                "close": close_price,
            }
        else:
            current = grouped[bucket]

            current["high"] = max(current["high"], high_price)
            current["low"] = min(current["low"], low_price)
            current["close"] = close_price

    result = []

    for candle in grouped.values():
        result.append(
            (
                candle["time"],
                candle["open"],
                candle["high"],
                candle["low"],
                candle["close"],
            )
        )

    return result


def write_ohlc_file(file_path, candles):
    """
    ذخیره خروجی استاندارد.

    ستون‌ها:
    Date,Time,Open,High,Low,Close
    """
    file_path.parent.mkdir(parents=True, exist_ok=True)

    with open(
        file_path,
        "w",
        encoding="utf-8-sig",
        newline="",
    ) as file:
        writer = csv.writer(file)

        writer.writerow(
            [
                "Date",
                "Time",
                "Open",
                "High",
                "Low",
                "Close",
            ]
        )

        for timestamp, open_price, high_price, low_price, close_price in candles:
            writer.writerow(
                [
                    timestamp.strftime("%Y.%m.%d"),
                    timestamp.strftime("%H:%M"),
                    format_price(open_price),
                    format_price(high_price),
                    format_price(low_price),
                    format_price(close_price),
                ]
            )


# =========================================================
# پردازش هر نماد
# =========================================================
def build_symbol_timeframes(symbol_dir):
    """ساخت تمام تایم‌فریم‌ها برای یک نماد"""
    symbol = clean_symbol_name(symbol_dir.name)

    if not symbol:
        raise ValueError("نام پوشه نماد نامعتبر است.")

    source_files = find_source_files(symbol_dir)

    if not source_files:
        raise FileNotFoundError(
            f"برای نماد {symbol} هیچ فایل CSV یا TXT پیدا نشد.\n"
            f"مسیر بررسی‌شده:\n{symbol_dir}"
        )

    print_status("=" * 70)
    print_status(f"شروع پردازش نماد: {symbol}")
    print_status(f"مسیر نماد: {symbol_dir}")
    print_status(f"تعداد فایل‌های M1 ورودی: {len(source_files)}")

    all_candles = []

    for index, source_file in enumerate(source_files, start=1):
        print_status(
            f"[{index}/{len(source_files)}] در حال خواندن فایل: {source_file.name}"
        )

        candles = read_market_file(source_file)

        if not candles:
            print_status(
                f"هشدار: هیچ داده معتبر در این فایل خوانده نشد: {source_file.name}"
            )
            continue

        print_status(f"تعداد کندل خوانده‌شده: {len(candles):,}")
        all_candles.extend(candles)

    if not all_candles:
        raise ValueError(
            f"هیچ کندل معتبر M1 برای نماد {symbol} پیدا نشد.\n\n"
            "ساختار قابل قبول فایل پنج ستونی:\n"
            "DateTime,Open,High,Low,Close\n\n"
            "یا ساختار شش ستونی:\n"
            "Date,Time,Open,High,Low,Close"
        )

    print_status("در حال مرتب‌سازی و حذف داده‌های تکراری...")

    m1_candles = merge_and_deduplicate(all_candles)

    if not m1_candles:
        raise ValueError("پس از مرتب‌سازی، هیچ کندل معتبری باقی نماند.")

    first_date = m1_candles[0][0].strftime("%Y.%m.%d %H:%M")
    last_date = m1_candles[-1][0].strftime("%Y.%m.%d %H:%M")

    print_status(f"تعداد نهایی کندل‌های M1: {len(m1_candles):,}")
    print_status(f"بازه داده: {first_date} تا {last_date}")

    for timeframe_name, minutes in TIMEFRAMES.items():
        print_status(f"در حال ساخت تایم‌فریم {timeframe_name}...")

        if timeframe_name == "M1":
            timeframe_candles = m1_candles
        else:
            timeframe_candles = resample_ohlc(
                m1_candles,
                timeframe_name,
                minutes,
            )

        output_dir = symbol_dir / timeframe_name
        output_file = output_dir / f"{symbol}_{timeframe_name}.csv"

        write_ohlc_file(output_file, timeframe_candles)

        print_status(
            f"تایم‌فریم {timeframe_name} ذخیره شد | "
            f"تعداد کندل: {len(timeframe_candles):,}"
        )
        print_status(f"مسیر خروجی: {output_file}")

    print_status(f"پردازش نماد {symbol} با موفقیت تمام شد.")
    print_status("=" * 70)


# =========================================================
# اجرای اصلی
# =========================================================
def get_symbol_directories():
    """
    اگر app.py متغیر CHART_DNA_SYMBOL را ارسال کرده باشد،
    فقط همان نماد پردازش می‌شود.

    در غیر این صورت همه پوشه‌های نماد داخل data پردازش می‌شوند.
    """
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    if SELECTED_SYMBOL:
        symbol_name = clean_symbol_name(SELECTED_SYMBOL)

        if not symbol_name:
            raise ValueError("نام نماد ارسالی نامعتبر است.")

        symbol_dir = DATA_DIR / symbol_name

        if not symbol_dir.exists():
            raise FileNotFoundError(f"پوشه نماد پیدا نشد:\n{symbol_dir}")

        if not symbol_dir.is_dir():
            raise NotADirectoryError(f"مسیر نماد پوشه نیست:\n{symbol_dir}")

        return [symbol_dir]

    directories = []

    for item in DATA_DIR.iterdir():
        if item.is_dir():
            directories.append(item)

    return sorted(directories)


def main():
    print_status("Chart DNA - Timeframe Builder")
    print_status(f"مسیر پروژه: {BASE_DIR}")
    print_status(f"مسیر data: {DATA_DIR}")

    try:
        symbol_directories = get_symbol_directories()

        if not symbol_directories:
            raise FileNotFoundError(
                "هیچ پوشه نمادی داخل data پیدا نشد.\n"
                f"مسیر data:\n{DATA_DIR}"
            )

        for symbol_dir in symbol_directories:
            build_symbol_timeframes(symbol_dir)

        print_status("تمام تایم‌فریم‌ها با موفقیت ساخته شدند.")
        return 0

    except Exception as error:
        print_status("")
        print_status("=" * 70)
        print_status("خطا در اجرای Timeframe Builder")
        print_status(f"نوع خطا: {type(error).__name__}")
        print_status(f"شرح خطا: {error}")
        print_status("=" * 70)
        return 1


if __name__ == "__main__":
    sys.exit(main())