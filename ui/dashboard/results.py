"""
ResultsMixin: نمایش نتیجه انتخاب‌شده از جدول، نمایش فایل کراپ‌شده و پاک کردن همه چیز.
"""
import os
from tkinter import messagebox

import numpy as np

from core.config import OUTPUT_DIR
from engine_core import normalize_shape, resample_series

class ResultsMixin:
    def show_selected_result(self, event=None):
        selected = self.results_table.selection()

        if not selected:
            return

        index = int(selected[0])

        if index < 0 or index >= len(self.results):
            return

        result = self.results[index]

        reference = normalize_shape(
            resample_series(self.reference_pattern, 80)
        )

        found = normalize_shape(
            resample_series(result["pattern"], 80)
        )

        future = np.asarray(result["future"], dtype=float)

        self.axis.clear()

        self.axis.plot(
            np.arange(len(reference)),
            reference,
            color="#0066cc",
            linewidth=2.4,
            label="الگوی مرجع از تصویر",
        )

        self.axis.plot(
            np.arange(len(found)),
            found,
            color="#ff7a00",
            linewidth=2.2,
            label="الگوی پیدا شده در CSV",
        )

        if len(future) > 0:
            future_normalized = future - result["pattern"][0]

            scale = np.max(result["pattern"]) - np.min(result["pattern"])

            if scale == 0:
                scale = 1

            future_normalized = future_normalized / scale

            x_future = np.arange(
                len(found),
                len(found) + len(future_normalized),
            )

            self.axis.plot(
                x_future,
                future_normalized,
                color="#1b9e3e",
                linewidth=2.3,
                marker="o",
                markersize=3,
                label=f"حرکت {len(future)} کندل آینده",
            )

            self.axis.axvline(
                len(found) - 1,
                color="#555555",
                linestyle="--",
                alpha=0.7,
            )

        self.axis.set_title(
            f"شباهت: {result['similarity']:.2f}% | "
            f"{result['file_name']} | شروع: {result['start_time']}"
        )

        self.axis.set_xlabel("مسیر زمانی نرمال‌شده")
        self.axis.set_ylabel("حرکت نسبی قیمت")
        self.axis.grid(True, alpha=0.3)
        self.axis.legend(fontsize=8)

        self.figure.tight_layout()
        self.figure_canvas.draw()

    def show_crop_file(self):
        crop_path = OUTPUT_DIR / "selected_pattern_crop.png"

        if not crop_path.exists():
            messagebox.showinfo(
                "Crop وجود ندارد",
                "هنوز محدوده‌ای استخراج و ذخیره نشده است.",
            )
            return

        try:
            os.startfile(crop_path)
        except Exception as error:
            messagebox.showerror("خطا", str(error))

    def clear_all(self):
        self.csv_files = []

        self.image_path = None
        self.original_image = None
        self.tk_image = None

        self.reference_pattern = None
        self.results = []

        self.crop_start = None
        self.crop_rect = None

        self.canvas.delete("all")
        self.results_table.delete(*self.results_table.get_children())

        self.csv_label.config(text="CSV انتخاب نشده است.")
        self.image_label.config(text="تصویر انتخاب نشده است.")

        self.axis.clear()
        self.axis.set_title("هنوز الگویی استخراج نشده است.")
        self.axis.grid(True, alpha=0.3)

        self.figure_canvas.draw()

        self.progress["value"] = 0
        self.status_var.set("وضعیت: انتخاب‌های تحلیل پاک شدند.")

        self.detail_var.set(
            "فایل‌های M1 و نام نماد پاک نشده‌اند تا بتوانید ساخت تایم‌فریم را ادامه دهید."
        )

