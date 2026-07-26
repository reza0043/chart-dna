"""
تنظیمات معیارهای شباهت:
- فعال/غیرفعال کردن هر معیار
- تعیین وزن درصدی هر معیار
- اعتبارسنجی مجموع وزن معیارهای فعال
- ذخیره در app_settings.json
"""

import tkinter as tk
from tkinter import ttk
from typing import Any, Dict, Tuple

from core.config import (
    DEFAULT_SIMILARITY_WEIGHTS,
    load_app_settings,
    save_app_settings,
)


# عنوان قابل نمایش هر معیار و کلید مورد استفاده در موتور تحلیل.
CRITERIA_INFO: Tuple[Tuple[str, str], ...] = (
    ("پیرسون (همبستگی شکلی)", "pearson"),
    ("میانگین اختلاف مطلق", "mean_abs_diff"),
    ("شیب (تغییرات نسبی)", "slope"),
    ("DTW (جابجایی زمانی)", "dtw"),
    ("ساختاری (قله و کف)", "structural"),
)


class SimilaritySettingsFrame(ttk.Frame):
    """
    صفحه تنظیم معیارهای شباهت.

    ساختار ذخیره‌شده در app_settings.json:

    {
        "similarity_weights": {
            "pearson": {
                "enabled": true,
                "weight": 0.35
            },
            "mean_abs_diff": {
                "enabled": true,
                "weight": 0.20
            },
            ...
        }
    }

    مقدار weight در فایل تنظیمات بین 0 و 1 ذخیره می‌شود،
    اما در رابط کاربری به‌صورت درصد بین 0 و 100 نمایش داده می‌شود.
    """

    def __init__(self, parent, settings_window=None):
        super().__init__(parent)

        self.settings_window = settings_window
        self.main_window = None

        self.criteria_vars: Dict[str, Dict[str, Any]] = {}

        self.total_label = None
        self.warning_label = None

        self._create_widgets()
        self.load_settings()

    # =================================================================
    # ساخت رابط کاربری
    # =================================================================

    def _create_widgets(self):
        """ساخت کنترل‌های ظاهری صفحه معیارهای شباهت."""

        title_label = ttk.Label(
            self,
            text=(
                "معیارهای فعال و درصد وزن هر معیار را مشخص کنید.\n"
                "مجموع درصد معیارهای فعال باید دقیقاً ۱۰۰ باشد."
            ),
            font=("Tahoma", 10, "bold"),
            justify="center",
            anchor="center",
        )
        title_label.pack(
            pady=(15, 8),
        )

        criteria_frame = ttk.LabelFrame(
            self,
            text="معیارهای محاسبه شباهت",
            padding=12,
        )
        criteria_frame.pack(
            padx=20,
            pady=8,
            fill="x",
        )

        criteria_frame.columnconfigure(0, weight=1)
        criteria_frame.columnconfigure(1, weight=0)
        criteria_frame.columnconfigure(2, weight=0)

        ttk.Label(
            criteria_frame,
            text="معیار",
            anchor="center",
            font=("Tahoma", 9, "bold"),
        ).grid(
            row=0,
            column=0,
            padx=8,
            pady=(0, 8),
            sticky="ew",
        )

        ttk.Label(
            criteria_frame,
            text="فعال",
            width=8,
            anchor="center",
            font=("Tahoma", 9, "bold"),
        ).grid(
            row=0,
            column=1,
            padx=8,
            pady=(0, 8),
        )

        ttk.Label(
            criteria_frame,
            text="درصد (۰ تا ۱۰۰)",
            width=15,
            anchor="center",
            font=("Tahoma", 9, "bold"),
        ).grid(
            row=0,
            column=2,
            padx=8,
            pady=(0, 8),
        )

        for row, (label, key) in enumerate(
            CRITERIA_INFO,
            start=1,
        ):
            enabled_var = tk.BooleanVar(value=True)
            weight_var = tk.StringVar(value="20")

            ttk.Label(
                criteria_frame,
                text=label,
                anchor="w",
            ).grid(
                row=row,
                column=0,
                padx=8,
                pady=5,
                sticky="w",
            )

            enabled_check = ttk.Checkbutton(
                criteria_frame,
                variable=enabled_var,
                command=lambda current_key=key: self._on_enabled_changed(
                    current_key
                ),
            )
            enabled_check.grid(
                row=row,
                column=1,
                padx=8,
                pady=5,
            )

            weight_spin = ttk.Spinbox(
                criteria_frame,
                from_=0,
                to=100,
                increment=1,
                textvariable=weight_var,
                width=10,
                justify="center",
            )
            weight_spin.grid(
                row=row,
                column=2,
                padx=8,
                pady=5,
            )

            weight_spin.bind(
                "<KeyRelease>",
                lambda _event: self.update_total_label(),
            )
            weight_spin.bind(
                "<FocusOut>",
                lambda _event: self.update_total_label(),
            )
            weight_spin.bind(
                "<<Increment>>",
                lambda _event: self.update_total_label(),
            )
            weight_spin.bind(
                "<<Decrement>>",
                lambda _event: self.update_total_label(),
            )

            weight_var.trace_add(
                "write",
                lambda *_args: self.update_total_label(),
            )

            self.criteria_vars[key] = {
                "enabled": enabled_var,
                "weight": weight_var,
                "spin": weight_spin,
                "check": enabled_check,
            }

        buttons_frame = ttk.Frame(self)
        buttons_frame.pack(
            pady=(10, 5),
        )

        ttk.Button(
            buttons_frame,
            text="توزیع مساوی بین فعال‌ها",
            command=self.distribute_equal,
        ).pack(
            side="right",
            padx=5,
        )

        ttk.Button(
            buttons_frame,
            text="بازنشانی به مقادیر پیش‌فرض",
            command=self.reset_to_defaults,
        ).pack(
            side="right",
            padx=5,
        )

        self.total_label = ttk.Label(
            self,
            text="",
            font=("Tahoma", 9, "bold"),
            anchor="center",
        )
        self.total_label.pack(
            pady=(5, 2),
        )

        self.warning_label = ttk.Label(
            self,
            text="",
            foreground="#C62828",
            font=("Tahoma", 9),
            justify="center",
            anchor="center",
        )
        self.warning_label.pack(
            pady=(0, 12),
        )

    # =================================================================
    # خواندن و تبدیل داده‌ها
    # =================================================================

    @staticmethod
    def _read_weight_item(
        item,
        default_enabled=True,
        default_weight=0.0,
    ):
        """
        خواندن امن مقدار یک معیار از تنظیمات.

        از هر دو ساختار زیر پشتیبانی می‌کند:

        ساختار جدید:
        {
            "enabled": true,
            "weight": 0.35
        }

        ساختار قدیمی:
        {
            "pearson": 0.35
        }
        """

        if isinstance(item, dict):
            enabled = bool(
                item.get(
                    "enabled",
                    default_enabled,
                )
            )
            raw_weight = item.get(
                "weight",
                default_weight,
            )
        else:
            enabled = default_enabled
            raw_weight = item

        try:
            weight = float(raw_weight)
        except (TypeError, ValueError):
            weight = float(default_weight)

        weight = max(0.0, min(1.0, weight))

        return enabled, weight

    def _get_default_criterion_values(self, key):
        """دریافت مقدار پیش‌فرض امن برای هر معیار."""

        default_item = DEFAULT_SIMILARITY_WEIGHTS.get(
            key,
            {
                "enabled": True,
                "weight": 0.0,
            },
        )

        return self._read_weight_item(
            default_item,
            default_enabled=True,
            default_weight=0.0,
        )

    def _get_current_settings(self):
        """
        دریافت تنظیمات جاری.

        در صورت وجود main_window ابتدا همان تنظیمات موجود در حافظه
        استفاده می‌شود تا تنظیمات بخش‌های دیگر ناخواسته از بین نروند.
        """

        main_window = getattr(
            self,
            "main_window",
            None,
        )

        if main_window is not None:
            app_settings = getattr(
                main_window,
                "app_settings",
                None,
            )

            if isinstance(app_settings, dict):
                return dict(app_settings)

        try:
            settings = load_app_settings()
        except Exception:
            settings = {}

        if not isinstance(settings, dict):
            settings = {}

        return settings

    # =================================================================
    # مدیریت رابط کاربری
    # =================================================================

    def _on_enabled_changed(self, key):
        """اعمال وضعیت فعال یا غیرفعال شدن یک معیار."""
        self._update_spin_state(key)
        self.update_total_label()

    def _update_spin_state(self, key):
        """فعال یا غیرفعال کردن کادر وزن معیار."""

        item = self.criteria_vars.get(key)

        if item is None:
            return

        if item["enabled"].get():
            item["spin"].configure(state="normal")
        else:
            item["spin"].configure(state="disabled")

    def _get_weight_percent(self, key) -> float:
        """خواندن امن درصد یک معیار از رابط کاربری."""

        item = self.criteria_vars[key]

        try:
            value = float(
                str(item["weight"].get()).strip()
            )
        except (TypeError, ValueError, tk.TclError):
            return 0.0

        return max(0.0, min(100.0, value))

    def get_active_total(self) -> float:
        """محاسبه مجموع وزن معیارهای فعال."""

        total = 0.0

        for key, item in self.criteria_vars.items():
            if item["enabled"].get():
                total += self._get_weight_percent(key)

        return total

    def update_total_label(self):
        """نمایش مجموع درصدها و وضعیت اعتبار وزن‌ها."""

        if not self.criteria_vars:
            return

        if self.total_label is None or self.warning_label is None:
            return

        active_keys = [
            key
            for key, item in self.criteria_vars.items()
            if item["enabled"].get()
        ]

        total = self.get_active_total()

        self.total_label.configure(
            text=f"مجموع درصد معیارهای فعال: {total:.2f}٪"
        )

        if not active_keys:
            self.total_label.configure(
                foreground="#C62828",
            )
            self.warning_label.configure(
                text="حداقل باید یک معیار شباهت فعال باشد.",
                foreground="#C62828",
            )
            return

        if abs(total - 100.0) < 0.01:
            self.total_label.configure(
                foreground="#198754",
            )
            self.warning_label.configure(
                text="✓ تنظیم وزن‌ها معتبر است.",
                foreground="#198754",
            )
            return

        self.total_label.configure(
            foreground="#C62828",
        )

        if total < 100.0:
            difference = 100.0 - total
            message = (
                f"مجموع وزن‌ها باید ۱۰۰٪ باشد. "
                f"{difference:.2f}٪ دیگر اضافه کنید."
            )
        else:
            difference = total - 100.0
            message = (
                f"مجموع وزن‌ها باید ۱۰۰٪ باشد. "
                f"{difference:.2f}٪ از وزن‌ها کم کنید."
            )

        self.warning_label.configure(
            text=message,
            foreground="#C62828",
        )

    # =================================================================
    # بارگذاری و اعتبارسنجی
    # =================================================================

    def load_settings(self):
        """بارگذاری تنظیمات ذخیره‌شده از app_settings.json."""

        settings = self._get_current_settings()

        weights = settings.get(
            "similarity_weights",
            {},
        )

        if not isinstance(weights, dict):
            weights = {}

        for key, item in self.criteria_vars.items():
            default_enabled, default_weight = (
                self._get_default_criterion_values(key)
            )

            saved_item = weights.get(
                key,
                {
                    "enabled": default_enabled,
                    "weight": default_weight,
                },
            )

            enabled, weight = self._read_weight_item(
                saved_item,
                default_enabled=default_enabled,
                default_weight=default_weight,
            )

            item["enabled"].set(enabled)

            percent_text = (
                f"{weight * 100.0:.2f}"
                .rstrip("0")
                .rstrip(".")
            )

            item["weight"].set(percent_text or "0")

            self._update_spin_state(key)

        self.update_total_label()

    def validate_settings(self) -> bool:
        """
        بررسی معتبر بودن تنظیمات.

        تنظیمات فقط زمانی معتبر است که:
        - حداقل یک معیار فعال باشد.
        - وزن تمام معیارهای فعال عددی بین ۰ و ۱۰۰ باشد.
        - مجموع وزن معیارهای فعال دقیقاً ۱۰۰ درصد باشد.
        """

        active_keys = [
            key
            for key, item in self.criteria_vars.items()
            if item["enabled"].get()
        ]

        if not active_keys:
            self.warning_label.configure(
                text="حداقل باید یک معیار شباهت فعال باشد.",
                foreground="#C62828",
            )
            return False

        for key in active_keys:
            raw_value = str(
                self.criteria_vars[key]["weight"].get()
            ).strip()

            try:
                value = float(raw_value)
            except (TypeError, ValueError, tk.TclError):
                self.warning_label.configure(
                    text=(
                        "درصد تمام معیارهای فعال باید یک عدد "
                        "بین ۰ تا ۱۰۰ باشد."
                    ),
                    foreground="#C62828",
                )
                return False

            if value < 0.0 or value > 100.0:
                self.warning_label.configure(
                    text=(
                        "درصد تمام معیارهای فعال باید بین "
                        "۰ تا ۱۰۰ باشد."
                    ),
                    foreground="#C62828",
                )
                return False

        total = self.get_active_total()

        if abs(total - 100.0) >= 0.01:
            self.update_total_label()
            return False

        return True

    # =================================================================
    # ذخیره
    # =================================================================

    def save_settings(self) -> bool:
        """
        اعتبارسنجی و ذخیره تنظیمات در app_settings.json.

        خروجی:
            True: ذخیره با موفقیت انجام شد.
            False: وزن‌ها نامعتبر هستند.
        """

        if not self.validate_settings():
            return False

        new_weights = {}

        for key, item in self.criteria_vars.items():
            enabled = bool(item["enabled"].get())

            if enabled:
                weight = self._get_weight_percent(key) / 100.0
            else:
                # معیار غیرفعال در موتور تحلیل هیچ اثری ندارد.
                weight = 0.0

            new_weights[key] = {
                "enabled": enabled,
                "weight": round(weight, 6),
            }

        settings = self._get_current_settings()
        settings["similarity_weights"] = new_weights

        # بسیار مهم:
        # تنظیمات حافظه پنجره اصلی هم همزمان به‌روزرسانی می‌شود.
        # این کار مانع حذف وزن‌های ذخیره‌شده هنگام تغییر تم می‌شود.
        main_window = getattr(
            self,
            "main_window",
            None,
        )

        if main_window is not None:
            try:
                main_window.app_settings = settings
            except Exception:
                pass

        save_app_settings(settings)

        self.warning_label.configure(
            text="✓ تنظیمات معیارهای شباهت با موفقیت ذخیره شد.",
            foreground="#198754",
        )

        return True

    # =================================================================
    # عملیات کمکی
    # =================================================================

    def distribute_equal(self):
        """
        تقسیم دقیق ۱۰۰ درصد بین تمام معیارهای فعال.

        نمونه:
        - ۳ معیار فعال: 33.34 ،33.33 ،33.33
        - ۵ معیار فعال: 20 ،20 ،20 ،20 ،20
        """

        enabled_keys = [
            key
            for key, item in self.criteria_vars.items()
            if item["enabled"].get()
        ]

        if not enabled_keys:
            self.warning_label.configure(
                text=(
                    "برای توزیع وزن، ابتدا حداقل یک معیار را فعال کنید."
                ),
                foreground="#C62828",
            )
            return

        count = len(enabled_keys)
        base_weight = round(100.0 / count, 2)

        distributed_total = 0.0

        for index, key in enumerate(enabled_keys):
            if index == count - 1:
                weight = round(
                    100.0 - distributed_total,
                    2,
                )
            else:
                weight = base_weight
                distributed_total += weight

            weight_text = (
                f"{weight:.2f}"
                .rstrip("0")
                .rstrip(".")
            )

            self.criteria_vars[key]["weight"].set(
                weight_text or "0"
            )

        self.update_total_label()

    def reset_to_defaults(self):
        """بازنشانی تمام معیارها به مقادیر پیش‌فرض برنامه."""

        for key, item in self.criteria_vars.items():
            enabled, weight = (
                self._get_default_criterion_values(key)
            )

            item["enabled"].set(enabled)

            percent_text = (
                f"{weight * 100.0:.2f}"
                .rstrip("0")
                .rstrip(".")
            )

            item["weight"].set(percent_text or "0")

            self._update_spin_state(key)

        self.update_total_label()