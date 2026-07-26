"""
ReferenceMixin: انتخاب فایل‌های CSV تحلیل، استخراج الگوی مرجع از ناحیه کراپ‌شده
و رسم آن روی نمودار.
"""
from pathlib import Path
from tkinter import filedialog, messagebox

import numpy as np

from core.config import OUTPUT_DIR
from engine_core.normalizer import normalize_shape, resample_series


class ReferenceMixin:
    def select_csv_files(self):
        paths = filedialog.askopenfilenames(
            title="انتخاب فایل یا فایل‌های CSV بازار برای تحلیل",
            filetypes=[
                ("CSV files", "*.csv"),
                ("Text files", "*.txt"),
                ("All files", "*.*"),
            ],
        )

        if not paths:
            return

        self.csv_files = list(paths)

        names = [Path(path).name for path in self.csv_files]
        preview = " | ".join(names[:4])

        if len(names) > 4:
            preview += f" | ... ({len(names)} فایل)"

        self.csv_label.config(text=f"CSV انتخاب‌شده برای تحلیل: {preview}")

    def extract_reference(self):
        if self.original_image is None:
            messagebox.showwarning(
                "تصویر لازم است",
                "ابتدا تصویر نمودار را انتخاب کنید.",
            )
            return

        try:
            box = self.get_crop_box_original()
            crop = self.original_image.crop(box)

            crop_path = OUTPUT_DIR / "selected_pattern_crop.png"
            crop.save(crop_path)

            pattern = self.extract_path_from_image(crop)

            if pattern is None or len(pattern) < 10:
                raise ValueError(
                    "مسیر قابل اعتمادی از تصویر پیدا نشد.\n\n"
                    "فقط خود نمودار را Crop کنید و نوشته‌ها، محور قیمت، "
                    "دکمه‌ها و اندیکاتورها را خارج از محدوده بگذارید."
                )

            self.reference_pattern = pattern
            self.plot_reference_only()

            self.status_var.set("وضعیت: الگوی مرجع با موفقیت استخراج شد.")
            self.detail_var.set(
                f"مسیر الگو استخراج شد و در این مسیر ذخیره شد:\n{crop_path}"
            )

        except Exception as error:
            messagebox.showerror("خطا در استخراج الگو", str(error))

    def extract_path_from_image(self, crop_image):
        image = np.asarray(crop_image.convert("RGB"), dtype=np.uint8)

        height, width, _ = image.shape

        r = image[:, :, 0].astype(int)
        g = image[:, :, 1].astype(int)
        b = image[:, :, 2].astype(int)

        maximum = np.maximum(np.maximum(r, g), b)
        minimum = np.minimum(np.minimum(r, g), b)

        saturation = maximum - minimum

        colored = (saturation > 55) & (maximum > 70)
        bright = (r > 190) & (g > 190) & (b > 190)

        dark_background = np.mean(image) < 150

        if dark_background:
            mask = colored | bright
        else:
            mask = colored

        y_positions = np.full(width, np.nan)

        for x in range(width):
            ys = np.where(mask[:, x])[0]

            if len(ys) > 0:
                y_positions[x] = float(np.median(ys))

        valid = ~np.isnan(y_positions)

        if valid.sum() < max(15, width * 0.08):
            return None

        x_all = np.arange(width)

        y_positions = np.interp(
            x_all,
            x_all[valid],
            y_positions[valid],
        )

        price_like_path = -y_positions

        window = max(3, min(15, width // 25))
        kernel = np.ones(window) / window

        smoothed = np.convolve(price_like_path, kernel, mode="same")

        return normalize_shape(resample_series(smoothed, 80))

    def plot_reference_only(self):
        self.axis.clear()

        self.axis.plot(
            self.reference_pattern,
            color="#0066cc",
            linewidth=2.2,
            label="الگوی استخراج‌شده از تصویر",
        )

        self.axis.set_title("الگوی مرجع استخراج‌شده")
        self.axis.set_xlabel("مسیر نرمال‌شده در زمان")
        self.axis.set_ylabel("حرکت نسبی قیمت")
        self.axis.grid(True, alpha=0.3)
        self.axis.legend()

        self.figure.tight_layout()
        self.figure_canvas.draw()
