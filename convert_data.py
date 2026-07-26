from pathlib import Path
import csv
from datetime import datetime


# ------------------------------------------------------------
# مسیرهای پروژه
# ------------------------------------------------------------
BASE_DIR = Path(__file__).resolve().parent
INPUT_DIR = BASE_DIR / "data"
OUTPUT_DIR = BASE_DIR / "processed_data"

# فایل‌هایی که نباید دوباره به‌عنوان ورودی پردازش شوند
IGNORE_FILES = {
    "source.txt",
}

# ------------------------------------------------------------
# تشخیص نام نماد و تایم‌فریم از نام فایل
# مثال:
# XAUUSD_M1_2024.csv  -> symbol=XAUUSD , timeframe=M1
# DAT_ASCII_XAUUSD_M1_2024.csv -> symbol=XAUUSD , timeframe=M1
# ------------------------------------------------------------
def get_symbol_and_timeframe(file_name: str):
    upper_name = file_name.upper().replace(".CSV", "")
    parts = upper_name.split("_")

    timeframe = "UNKNOWN"
    symbol = "UNKNOWN"

    timeframes = {
        "M1", "M5", "M15", "M30",
        "H1", "H4", "D1", "W1", "MN1"
    }

    for part in parts:
        if part in timeframes:
            timeframe = part
            break

    # برای نام ساده مثل XAUUSD_M1_2024.csv
    if len(parts) >= 2 and parts[1] in timeframes:
        symbol = parts[0]

    # برای نام Dukascopy مانند DAT_ASCII_XAUUSD_M1_2024.csv
    elif "ASCII" in parts:
        ascii_index = parts.index("ASCII")
        if ascii_index + 1 < len(parts):
            symbol = parts[ascii_index + 1]

    return symbol, timeframe


# ------------------------------------------------------------
# تبدیل یک فایل DAT_ASCII به CSV استاندارد Chart DNA
# فرمت ورودی:
# 20240101 180000;2062.598000;2064.525000;2062.405000;2064.235000;0
#
# ترتیب:
# datetime ; open ; high ; low ; close ; volume
# ------------------------------------------------------------
def convert_file(input_file: Path):
    symbol, timeframe = get_symbol_and_timeframe(input_file.name)

    output_file = OUTPUT_DIR / f"{symbol}_{timeframe}_{input_file.stem}_clean.csv"

    total_rows = 0
    valid_rows = 0
    skipped_rows = 0

    print("\n" + "=" * 70)
    print(f"در حال تبدیل فایل: {input_file.name}")
    print(f"نماد تشخیص داده‌شده: {symbol}")
    print(f"تایم‌فریم تشخیص داده‌شده: {timeframe}")

    with open(input_file, "r", encoding="utf-8-sig", errors="replace") as source, \
         open(output_file, "w", newline="", encoding="utf-8") as destination:

        writer = csv.writer(destination)

        # هدر استانداردی که از این مرحله به بعد موتور استفاده می‌کند
        writer.writerow([
            "datetime",
            "open",
            "high",
            "low",
            "close",
            "volume",
            "symbol",
            "timeframe"
        ])

        for line_number, line in enumerate(source, start=1):
            total_rows += 1
            line = line.strip()

            if not line:
                skipped_rows += 1
                continue

            parts = line.split(";")

            # باید حداقل 6 ستون وجود داشته باشد
            if len(parts) < 6:
                skipped_rows += 1
                continue

            try:
                raw_datetime = parts[0].strip()

                # فرمت واقعی فایل شما: 20240101 180000
                dt = datetime.strptime(raw_datetime, "%Y%m%d %H%M%S")

                open_price = float(parts[1].strip())
                high_price = float(parts[2].strip())
                low_price = float(parts[3].strip())
                close_price = float(parts[4].strip())
                volume = float(parts[5].strip())

                # بررسی اولیه معتبر بودن OHLC
                if high_price < low_price:
                    skipped_rows += 1
                    continue

                writer.writerow([
                    dt.strftime("%Y-%m-%d %H:%M:%S"),
                    open_price,
                    high_price,
                    low_price,
                    close_price,
                    volume,
                    symbol,
                    timeframe
                ])

                valid_rows += 1

            except (ValueError, IndexError):
                skipped_rows += 1

    print(f"تعداد کل خطوط: {total_rows:,}")
    print(f"تعداد رکوردهای صحیح: {valid_rows:,}")
    print(f"تعداد خطوط ردشده: {skipped_rows:,}")
    print(f"فایل خروجی ساخته شد:")
    print(output_file)

    return valid_rows


def main():
    print("=" * 70)
    print("Chart DNA | تبدیل داده‌های خام بازار")
    print("=" * 70)

    if not INPUT_DIR.exists():
        print(f"\nخطا: پوشه data پیدا نشد:")
        print(INPUT_DIR)
        return

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    csv_files = [
        file for file in INPUT_DIR.glob("*.csv")
        if file.name.lower() not in IGNORE_FILES
    ]

    if not csv_files:
        print("\nخطا: هیچ فایل CSV در پوشه data پیدا نشد.")
        print(f"مسیر پوشه: {INPUT_DIR}")
        return

    total_valid_rows = 0

    for csv_file in csv_files:
        total_valid_rows += convert_file(csv_file)

    print("\n" + "=" * 70)
    print("تبدیل تمام فایل‌ها با موفقیت تمام شد.")
    print(f"مجموع رکوردهای صحیح: {total_valid_rows:,}")
    print(f"پوشه خروجی: {OUTPUT_DIR}")
    print("=" * 70)


if __name__ == "__main__":
    main()