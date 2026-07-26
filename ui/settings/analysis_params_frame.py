"""
تنظیم حداقل شباهت و تعداد کندل‌های آینده.
"""

import tkinter as tk
from tkinter import ttk

from core.config import (
    load_app_settings,
    save_app_settings,
)


class AnalysisParamsFrame(ttk.Frame):
    def __init__(
        self,
        parent,
        settings_window,
    ):
        super().__init__(parent)

        self.settings_window = settings_window
        self.main_window = settings_window.main_window

        self.threshold_var = tk.IntVar(
            value=70
        )

        self.future_var = tk.IntVar(
            value=10
        )

        self.pattern_length_var = tk.IntVar(
            value=50
        )

        ttk.Label(
            self,
            text="پارامترهای تحلیل",
            font=("Segoe UI", 14, "bold"),
        ).pack(
            pady=(25, 15),
        )

        parameters_frame = ttk.LabelFrame(
            self,
            text="تنظیمات جستجوی الگو",
            padding=20,
        )
        parameters_frame.pack(
            fill="x",
            padx=30,
            pady=10,
        )

        threshold_row = ttk.Frame(
            parameters_frame
        )
        threshold_row.pack(
            fill="x",
            pady=10,
        )

        ttk.Label(
            threshold_row,
            text="حداقل درصد شباهت:",
            width=28,
        ).pack(
            side="left",
        )

        ttk.Spinbox(
            threshold_row,
            from_=0,
            to=100,
            increment=1,
            textvariable=self.threshold_var,
            width=10,
            justify="center",
        ).pack(
            side="left",
        )

        ttk.Label(
            threshold_row,
            text="درصد",
        ).pack(
            side="left",
            padx=8,
        )

        future_row = ttk.Frame(
            parameters_frame
        )
        future_row.pack(
            fill="x",
            pady=10,
        )

        ttk.Label(
            future_row,
            text="تعداد کندل‌های آینده:",
            width=28,
        ).pack(
            side="left",
        )

        ttk.Spinbox(
            future_row,
            from_=1,
            to=500,
            increment=1,
            textvariable=self.future_var,
            width=10,
            justify="center",
        ).pack(
            side="left",
        )

        ttk.Label(
            future_row,
            text="کندل",
        ).pack(
            side="left",
            padx=8,
        )

        pattern_length_row = ttk.Frame(
            parameters_frame
        )
        pattern_length_row.pack(
            fill="x",
            pady=10,
        )

        ttk.Label(
            pattern_length_row,
            text="تعداد نقاط شباهت:",
            width=28,
        ).pack(
            side="left",
        )

        ttk.Spinbox(
            pattern_length_row,
            from_=10,
            to=300,
            increment=1,
            textvariable=self.pattern_length_var,
            width=10,
            justify="center",
        ).pack(
            side="left",
        )

        ttk.Label(
            pattern_length_row,
            text="نقطه (کندل)",
        ).pack(
            side="left",
            padx=8,
        )

        ttk.Label(
            self,
            text=(
                "این مقادیر در اجرای بعدی برنامه نیز حفظ می‌شوند."
            ),
            foreground="#666666",
        ).pack(
            pady=15,
        )

    def load_settings(self):
        settings = load_app_settings()

        self.threshold_var.set(
            settings.get(
                "min_similarity",
                70,
            )
        )

        self.future_var.set(
            settings.get(
                "future_candles",
                10,
            )
        )

        self.pattern_length_var.set(
            settings.get(
                "pattern_length",
                50,
            )
        )

    def save_settings(self):
        threshold = int(
            self.threshold_var.get()
        )

        future = int(
            self.future_var.get()
        )

        pattern_length = int(
            self.pattern_length_var.get()
        )

        if threshold < 0 or threshold > 100:
            raise ValueError(
                "حداقل شباهت باید بین صفر تا صد باشد."
            )

        if future < 1:
            raise ValueError(
                "تعداد کندل‌های آینده باید حداقل یک باشد."
            )

        if pattern_length < 10 or pattern_length > 300:
            raise ValueError(
                "تعداد نقاط شباهت باید بین ۱۰ تا ۳۰۰ باشد."
            )

        save_app_settings(
            {
                "min_similarity": threshold,
                "future_candles": future,
                "pattern_length": pattern_length,
            }
        )

        if self.main_window is not None:
            if hasattr(
                self.main_window,
                "threshold_var",
            ):
                self.main_window.threshold_var.set(
                    threshold
                )

            if hasattr(
                self.main_window,
                "future_var",
            ):
                self.main_window.future_var.set(
                    future
                )

            if hasattr(
                self.main_window,
                "pattern_length_var",
            ):
                self.main_window.pattern_length_var.set(
                    pattern_length
                )

        return True