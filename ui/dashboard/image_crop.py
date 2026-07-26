"""
ImageCropMixin: انتخاب تصویر نمودار، نمایش آن روی Canvas و کراپ کردن
ناحیه‌ی مورد نظر با ماوس.
"""
from pathlib import Path
from tkinter import filedialog, messagebox

from PIL import Image, ImageTk


class ImageCropMixin:
    def select_image(self):
        path = filedialog.askopenfilename(
            title="انتخاب تصویر نمودار",
            filetypes=[
                ("Image files", "*.png *.jpg *.jpeg *.bmp *.webp"),
                ("All files", "*.*"),
            ],
        )

        if not path:
            return

        try:
            self.image_path = path
            self.original_image = Image.open(path).convert("RGB")

            self.image_label.config(
                text=(
                    f"تصویر انتخاب‌شده: {Path(path).name} | "
                    f"{self.original_image.width} × {self.original_image.height}"
                )
            )

            self.reference_pattern = None
            self.display_image()

            self.status_var.set("وضعیت: تصویر نمودار آماده است.")
            self.detail_var.set(
                "با نگه داشتن کلیک چپ ماوس، محدوده الگو را روی تصویر انتخاب کنید."
            )

        except Exception as error:
            messagebox.showerror("خطا در تصویر", str(error))

    def display_image(self):
        if self.original_image is None:
            return

        self.root.update_idletasks()

        canvas_width = max(self.canvas.winfo_width(), 700)
        canvas_height = max(self.canvas.winfo_height(), 450)

        image_width, image_height = self.original_image.size

        self.display_scale = min(
            canvas_width / image_width,
            canvas_height / image_height,
        )

        self.display_scale = min(self.display_scale, 1.0)

        new_width = max(1, int(image_width * self.display_scale))
        new_height = max(1, int(image_height * self.display_scale))

        displayed = self.original_image.resize(
            (new_width, new_height),
            Image.LANCZOS,
        )

        self.tk_image = ImageTk.PhotoImage(displayed)

        self.canvas.delete("all")

        self.image_offset_x = (canvas_width - new_width) // 2
        self.image_offset_y = (canvas_height - new_height) // 2

        self.canvas.create_image(
            self.image_offset_x,
            self.image_offset_y,
            image=self.tk_image,
            anchor="nw",
            tags="chart_image",
        )

    # =========================================================
    # Crop تصویر
    # =========================================================
    def crop_begin(self, event):
        if self.original_image is None:
            return

        self.crop_start = (event.x, event.y)

        if self.crop_rect:
            self.canvas.delete(self.crop_rect)

        self.crop_rect = self.canvas.create_rectangle(
            event.x,
            event.y,
            event.x,
            event.y,
            outline="#00ff88",
            width=2,
        )

    def crop_move(self, event):
        if self.crop_start is None or self.crop_rect is None:
            return

        x0, y0 = self.crop_start
        self.canvas.coords(self.crop_rect, x0, y0, event.x, event.y)

    def crop_finish(self, event):
        if self.crop_start is None:
            return

        x0, y0 = self.crop_start
        x1, y1 = event.x, event.y

        if abs(x1 - x0) < 20 or abs(y1 - y0) < 20:
            self.detail_var.set(
                "محدوده Crop خیلی کوچک است. محدوده بزرگ‌تری انتخاب کنید."
            )
            return

        self.detail_var.set(
            "محدوده انتخاب شد. اکنون دکمه استخراج مسیر را بزنید."
        )

    def get_crop_box_original(self):
        if self.crop_start is None or self.crop_rect is None:
            raise ValueError("ابتدا با ماوس محدوده الگو را روی تصویر انتخاب کنید.")

        coords = self.canvas.coords(self.crop_rect)

        if len(coords) != 4:
            raise ValueError("مختصات Crop نامعتبر است.")

        x0, y0, x1, y1 = coords

        left = min(x0, x1)
        top = min(y0, y1)
        right = max(x0, x1)
        bottom = max(y0, y1)

        left = int((left - self.image_offset_x) / self.display_scale)
        top = int((top - self.image_offset_y) / self.display_scale)
        right = int((right - self.image_offset_x) / self.display_scale)
        bottom = int((bottom - self.image_offset_y) / self.display_scale)

        image_width, image_height = self.original_image.size

        left = max(0, min(left, image_width - 1))
        top = max(0, min(top, image_height - 1))
        right = max(left + 1, min(right, image_width))
        bottom = max(top + 1, min(bottom, image_height))

        if right - left < 30 or bottom - top < 30:
            raise ValueError("محدوده انتخابی معتبر نیست.")

        return left, top, right, bottom
