import json
import re
from pathlib import Path


# =========================================================
# تنظیمات اصلی پروژه
# =========================================================
APP_TITLE = "Chart DNA - تشخیص الگوهای مشابه"

# مقدار پیش‌فرض «تعداد نقاط شباهت» — کاربر می‌تواند این مقدار را از
# صفحه تنظیمات > پارامترهای تحلیل تغییر دهد؛ این فقط مقدار اولیه است.
PATTERN_LENGTH = 50

BASE_DIR = Path(__file__).resolve().parent.parent

DATA_DIR = BASE_DIR / "data"
OUTPUT_DIR = BASE_DIR / "output"
SCRIPTS_DIR = BASE_DIR / "scripts"

TIMEFRAME_BUILDER_PATH = SCRIPTS_DIR / "timeframe_builder.py"

DATA_DIR.mkdir(parents=True, exist_ok=True)
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
SCRIPTS_DIR.mkdir(parents=True, exist_ok=True)


# ترتیب نمایش تایم‌فریم‌ها در جدول تنظیمات
BASE_TIMEFRAMES = [
    "M1",
    "M5",
    "M15",
    "M30",
    "H1",
    "H4",
    "D1",
    "W1",
]

TIMEFRAME_ORDER = {
    "M1": 1,
    "M2": 2,
    "M3": 3,
    "M5": 5,
    "M10": 10,
    "M15": 15,
    "M30": 30,
    "H1": 60,
    "H2": 120,
    "H3": 180,
    "H4": 240,
    "H6": 360,
    "H8": 480,
    "H12": 720,
    "D1": 1440,
    "W1": 10080,
    "MN1": 43200,
}

TIMEFRAME_PATTERN = re.compile(
    r"(?<![A-Z0-9])"
    r"(MN1|W1|D1|H12|H8|H6|H4|H3|H2|H1|M30|M15|M10|M5|M3|M2|M1)"
    r"(?![A-Z0-9])",
    re.IGNORECASE,
)

# =========================================================
# تنظیمات معیارهای شباهت (ذخیره در app_settings)
# =========================================================

DEFAULT_SIMILARITY_WEIGHTS = {
    "pearson": {"enabled": True, "weight": 0.35},
    "mean_abs_diff": {"enabled": True, "weight": 0.20},
    "slope": {"enabled": True, "weight": 0.25},
    "dtw": {"enabled": True, "weight": 0.10},
    "structural": {"enabled": True, "weight": 0.10},
}


def get_similarity_weights() -> dict:
    """دریافت وزن‌های نرمال‌شده معیارهای فعال (مجموع = 1.0)"""
    settings = load_app_settings()
    weights_data = settings.get("similarity_weights", DEFAULT_SIMILARITY_WEIGHTS.copy())
    
    # استخراج وزن‌های معیارهای فعال
    active = {k: v["weight"] for k, v in weights_data.items() if v["enabled"]}
    
    if not active:
        # اگر همه غیرفعال هستند، همه را با وزن مساوی فعال کن
        n = len(DEFAULT_SIMILARITY_WEIGHTS)
        return {k: 1.0 / n for k in DEFAULT_SIMILARITY_WEIGHTS}
    
    total = sum(active.values())
    if total == 0:
        return {k: 1.0 / len(active) for k in active}
    
    # نرمال‌سازی
    return {k: v / total for k, v in active.items()}


def update_similarity_weights(new_weights: dict):
    """بروزرسانی تنظیمات معیارهای شباهت در app_settings"""
    settings = load_app_settings()
    settings["similarity_weights"] = new_weights
    save_app_settings(settings)

# =========================================================
# تنظیمات نرم‌افزار (مسیر ذخیره + خواندن/نوشتن)
# =========================================================
APP_SETTINGS_PATH = BASE_DIR / "app_settings.json"


def load_app_settings():
    """خواندن تنظیمات نرم‌افزار از فایل app_settings.json"""
    try:
        with open(APP_SETTINGS_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
            if isinstance(data, dict):
                return data
    except (OSError, ValueError):
        pass
    return {}


def save_app_settings(data):
    """ذخیره تنظیمات نرم‌افزار در فایل app_settings.json"""
    try:
        current = load_app_settings()
        current.update(data)
        with open(APP_SETTINGS_PATH, "w", encoding="utf-8") as f:
            json.dump(current, f, ensure_ascii=False, indent=4)
        return True
    except OSError:
        return False
