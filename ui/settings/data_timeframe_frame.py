"""
DataTimeframeFrame: تنظیمات داده و تایم‌فریم
ورود فایل‌های M1، ساخت تایم‌فریم‌ها و انتخاب نماد/تایم‌فریم برای تحلیل.
"""

import tkinter as tk
from tkinter import ttk


class DataTimeframeFrame(ttk.Frame):
    def __init__(self, parent, settings_window):
        super().__init__(parent)

        self.settings_window = settings_window
        self.main_window = None

        # ==============================
        # بخش بالایی: ورود M1 و ساخت تایم‌فریم
        # ==============================
        upload_frame = ttk.LabelFrame(
            self,
            text="ورود فایل‌های یک دقیقه‌ای M1 و ساخت تایم‌فریم‌ها",
            padding=12,
        )
        upload_frame.pack(fill="x", padx=12, pady=(8, 4))

        row0 = ttk.Frame(upload_frame)
        row0.pack(fill="x", pady=3)

        ttk.Label(row0, text="نام نماد:").pack(side="left", padx=(0, 5))

        self.symbol_entry = ttk.Entry(row0, width=18, justify="center")
        self.symbol_entry.pack(side="left", padx=(0, 14))

        ttk.Button(
            row0,
            text="📂 انتخاب فایل‌های M1",
            command=self.select_m1,
        ).pack(side="left", padx=5)

        self.build_timeframes_button = ttk.Button(
            row0,
            text="🔨 ذخیره و ساخت تایم‌فریم‌ها",
            command=self.build_timeframes,
        )
        self.build_timeframes_button.pack(side="left", padx=(12, 5))

        self.m1_files_label = ttk.Label(
            upload_frame,
            text="فایل یک دقیقه‌ای انتخاب نشده است.",
            foreground="#444444",
        )
        self.m1_files_label.pack(anchor="w", padx=2, pady=(6, 0))

        builder_progress_row = ttk.Frame(upload_frame)
        builder_progress_row.pack(fill="x", padx=2, pady=(8, 0))

        self.builder_progress = ttk.Progressbar(
            builder_progress_row,
            mode="determinate",
            maximum=100,
        )
        self.builder_progress.pack(
            side="left",
            fill="x",
            expand=True,
        )

        self.builder_progress_percent_var = tk.StringVar(value="۰٪")

        ttk.Label(
            builder_progress_row,
            textvariable=self.builder_progress_percent_var,
            font=("Segoe UI", 9, "bold"),
            width=6,
            anchor="center",
        ).pack(side="left", padx=(8, 0))

        self.builder_status_var = tk.StringVar(value="")

        ttk.Label(
            upload_frame,
            textvariable=self.builder_status_var,
            foreground="#444444",
            wraplength=720,
            justify="right",
        ).pack(anchor="w", padx=2, pady=(4, 0))

        ttk.Label(
            upload_frame,
            text="فایل‌ها در مسیر data\\نام_نماد ذخیره می‌شوند و سپس timeframe_builder.py اجرا می‌شود.",
            foreground="#444444",
        ).pack(anchor="w", padx=2, pady=(5, 0))

        ttk.Button(
            upload_frame,
            text="📁 باز کردن پوشه Data",
            command=self.open_data,
        ).pack(anchor="w", padx=2, pady=(5, 0))

        # ==============================
        # بخش پایینی: جدول انتخاب نماد و تایم‌فریم
        # ==============================
        selection_frame = ttk.LabelFrame(
            self,
            text="انتخاب نماد و تایم‌فریم‌های مورد استفاده در تحلیل",
            padding=10,
        )
        selection_frame.pack(fill="both", expand=True, padx=12, pady=(4, 8))

        button_bar = ttk.Frame(selection_frame)
        button_bar.pack(fill="x", pady=(0, 8))

        ttk.Button(
            button_bar,
            text="✓ انتخاب همه فایل‌های موجود",
            command=self.select_all,
        ).pack(side="left", padx=3)

        ttk.Button(
            button_bar,
            text="✗ لغو همه انتخاب‌ها",
            command=self.clear_all,
        ).pack(side="left", padx=3)

        ttk.Button(
            button_bar,
            text="🔄 بروزرسانی جدول",
            command=self.refresh_data,
        ).pack(side="left", padx=3)

        self.settings_selection_summary = tk.StringVar(
            value="فایل انتخاب‌شده برای تحلیل: 0"
        )

        ttk.Label(
            button_bar,
            textvariable=self.settings_selection_summary,
            foreground="#0a7a37",
            font=("Segoe UI", 9, "bold"),
        ).pack(side="right", padx=5)

        # ==============================
        # کانتینر جدول با اسکرول
        # ==============================
        table_container = ttk.Frame(selection_frame)
        table_container.pack(fill="both", expand=True)

        self.settings_canvas = tk.Canvas(
            table_container,
            highlightthickness=0,
            borderwidth=0,
        )

        h_scroll = ttk.Scrollbar(
            table_container,
            orient="horizontal",
            command=self.settings_canvas.xview,
        )

        v_scroll = ttk.Scrollbar(
            table_container,
            orient="vertical",
            command=self.settings_canvas.yview,
        )

        self.settings_canvas.configure(
            xscrollcommand=h_scroll.set,
            yscrollcommand=v_scroll.set,
        )

        h_scroll.pack(side="bottom", fill="x")
        v_scroll.pack(side="right", fill="y")
        self.settings_canvas.pack(side="left", fill="both", expand=True)

        self.settings_table_inner = ttk.Frame(self.settings_canvas)

        self.settings_canvas_window = self.settings_canvas.create_window(
            (0, 0),
            window=self.settings_table_inner,
            anchor="nw",
        )

        self.settings_table_inner.bind(
            "<Configure>",
            self._update_scroll_region,
        )

        self.settings_canvas.bind(
            "<Configure>",
            self._on_canvas_configure,
        )

        # ==============================
        # دکمه‌های پایین
        # ==============================
        bottom_frame = ttk.Frame(self)
        bottom_frame.pack(fill="x", padx=12, pady=(4, 12))

        ttk.Button(
            bottom_frame,
            text="✅ تأیید و استفاده برای تحلیل",
            command=self.confirm_and_close,
        ).pack(side="right", padx=4)

    # =========================================================
    # اتصال ویجت‌های این فرم به main_window
    # =========================================================
    def bind_main_window(self, main_window):
        """
        اگر در جایی از برنامه خواستیم صریحاً main_window را وصل کنیم،
        از این متد استفاده می‌شود.
        """
        self.main_window = main_window
        self._sync_widgets_to_main_window()

    def _sync_widgets_to_main_window(self):
        """
        متدهای TimeframeSelectionMixin روی main_window اجرا می‌شوند.
        بنابراین باید ویجت‌های ساخته‌شده در این Frame را به main_window معرفی کنیم.
        """
        if self.main_window is None:
            return False

        self.main_window.settings_canvas = self.settings_canvas
        self.main_window.settings_table_inner = self.settings_table_inner
        self.main_window.settings_canvas_window = self.settings_canvas_window
        self.main_window.settings_selection_summary = self.settings_selection_summary

        self.main_window.m1_files_label = self.m1_files_label
        self.main_window.build_timeframes_button = self.build_timeframes_button

        return True

    # =========================================================
    # اسکرول جدول
    # =========================================================
    def _update_scroll_region(self, event=None):
        try:
            self.settings_canvas.configure(
                scrollregion=self.settings_canvas.bbox("all")
            )
        except tk.TclError:
            pass

    def _on_canvas_configure(self, event=None):
        """
        هنگام تغییر اندازه Canvas، ناحیه اسکرول به‌روزرسانی می‌شود.
        """
        try:
            self.settings_canvas.configure(
                scrollregion=self.settings_canvas.bbox("all")
            )
        except tk.TclError:
            pass

    # =========================================================
    # بارگذاری اولیه داده‌ها
    # =========================================================
    def load_data(self):
        """
        بارگذاری داده‌های اولیه و پر کردن جدول از پوشه data.
        """
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        # انتقال نماد فعلی از main_window به Entry
        if hasattr(self.main_window, "symbol_var"):
            current_symbol = self.main_window.symbol_var.get()
            self.symbol_entry.delete(0, tk.END)
            self.symbol_entry.insert(0, current_symbol)

        # پر کردن جدول از data
        if hasattr(self.main_window, "refresh_timeframe_table"):
            self.main_window.refresh_timeframe_table()

        self._update_summary()

    def _update_summary(self):
        """
        بروزرسانی خلاصه انتخاب.
        چون StringVar به main_window وصل شده، معمولاً خود main_window آن را آپدیت می‌کند.
        """
        if self.main_window is None:
            return

        if hasattr(self.main_window, "update_timeframe_selection_summary"):
            self.main_window.update_timeframe_selection_summary()

    # =========================================================
    # متدهای ساخت تایم‌فریم
    # =========================================================
    def update_builder_progress(self, text, percent):
        """
        به‌روزرسانی نوار درصد و متن وضعیت داخل همین تب تنظیمات، تا وقتی
        پنجره تنظیمات باز (و روی صفحه اصلی) است، پیشرفت ساخت تایم‌فریم‌ها
        بدون نیاز به بستن این پنجره دیده شود.
        """
        try:
            value = max(0, min(100, float(percent)))
        except (TypeError, ValueError):
            value = 0

        self.builder_progress["value"] = value
        self.builder_progress_percent_var.set(f"{value:.0f}٪")

        if text:
            self.builder_status_var.set(text)

    def select_m1(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        if hasattr(self.main_window, "select_m1_files"):
            self.main_window.select_m1_files()

    def build_timeframes(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        # انتقال نماد واردشده به main_window
        if hasattr(self.main_window, "symbol_var"):
            self.main_window.symbol_var.set(self.symbol_entry.get().strip())

        if hasattr(self.main_window, "start_timeframe_builder"):
            self.main_window.start_timeframe_builder()

    def open_data(self):
        if self.main_window is None:
            return

        if hasattr(self.main_window, "open_data_folder"):
            self.main_window.open_data_folder()

    # =========================================================
    # متدهای جدول انتخاب
    # =========================================================
    def select_all(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        if hasattr(self.main_window, "select_all_timeframes"):
            self.main_window.select_all_timeframes()

        self._update_summary()

    def clear_all(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        if hasattr(self.main_window, "clear_all_timeframes"):
            self.main_window.clear_all_timeframes()

        self._update_summary()

    def refresh_data(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        if hasattr(self.main_window, "refresh_timeframe_table"):
            self.main_window.refresh_timeframe_table()

        self._update_summary()

    def confirm_and_close(self):
        if self.main_window is None:
            return

        self._sync_widgets_to_main_window()

        # انتقال نماد واردشده
        if hasattr(self.main_window, "symbol_var"):
            self.main_window.symbol_var.set(self.symbol_entry.get().strip())

        if hasattr(self.main_window, "confirm_timeframe_selection"):
            self.main_window.confirm_timeframe_selection()