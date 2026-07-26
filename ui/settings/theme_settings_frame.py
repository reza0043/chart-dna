"""
فریم انتخاب تم رنگی برنامه.
"""

import tkinter as tk
from tkinter import messagebox, ttk

from core.themes import THEMES, THEME_ORDER


class ThemeSettingsFrame(ttk.Frame):
    def __init__(
        self,
        parent,
        settings_window,
    ):
        super().__init__(parent)

        self.settings_window = settings_window
        self.main_window = settings_window.main_window

        self.theme_status_var = tk.StringVar()

        ttk.Label(
            self,
            text="انتخاب تم رنگی نرم‌افزار",
            font=("Segoe UI", 14, "bold"),
        ).pack(
            pady=(20, 8),
        )

        ttk.Label(
            self,
            textvariable=self.theme_status_var,
            font=("Segoe UI", 11),
        ).pack(
            pady=(0, 10),
        )

        container = ttk.Frame(self)
        container.pack(
            fill="both",
            expand=True,
            padx=20,
            pady=10,
        )

        canvas = tk.Canvas(
            container,
            highlightthickness=0,
            borderwidth=0,
        )

        scrollbar = ttk.Scrollbar(
            container,
            orient="vertical",
            command=canvas.yview,
        )

        self.buttons_frame = ttk.Frame(
            canvas
        )

        canvas_window = canvas.create_window(
            (0, 0),
            window=self.buttons_frame,
            anchor="nw",
        )

        self.buttons_frame.bind(
            "<Configure>",
            lambda event: canvas.configure(
                scrollregion=canvas.bbox("all")
            ),
        )

        canvas.bind(
            "<Configure>",
            lambda event: canvas.itemconfigure(
                canvas_window,
                width=event.width,
            ),
        )

        canvas.configure(
            yscrollcommand=scrollbar.set
        )

        canvas.pack(
            side="left",
            fill="both",
            expand=True,
        )

        scrollbar.pack(
            side="right",
            fill="y",
        )

        for theme_key in THEME_ORDER:
            if theme_key not in THEMES:
                continue

            self._create_theme_row(
                theme_key,
                THEMES[theme_key],
            )

        ttk.Label(
            self,
            text=(
                "با انتخاب هر تم، ظاهر برنامه تغییر می‌کند "
                "و انتخاب شما در app_settings.json ذخیره می‌شود."
            ),
            foreground="#666666",
        ).pack(
            pady=(5, 15),
        )

        self.load_settings()

    def _create_theme_row(
        self,
        theme_key,
        palette,
    ):
        row = ttk.Frame(
            self.buttons_frame
        )
        row.pack(
            fill="x",
            padx=5,
            pady=4,
        )

        colors_frame = tk.Frame(row)
        colors_frame.pack(
            side="right",
            padx=8,
        )

        colors = [
            palette.get("bg", "#ffffff"),
            palette.get("surface", "#eeeeee"),
            palette.get("accent", "#3366cc"),
            palette.get("fg", "#000000"),
        ]

        for color in colors:
            sample = tk.Frame(
                colors_frame,
                width=22,
                height=22,
                bg=color,
                relief="solid",
                borderwidth=1,
            )
            sample.pack(
                side="right",
                padx=1,
            )
            sample.pack_propagate(False)

        ttk.Button(
            row,
            text=f"🎨 {palette.get('label', theme_key)}",
            command=lambda key=theme_key: self.select_theme(key),
        ).pack(
            side="right",
            fill="x",
            expand=True,
        )

    def select_theme(self, theme_key):
        if theme_key not in THEMES:
            messagebox.showerror(
                "خطا",
                "تم انتخاب‌شده معتبر نیست.",
                parent=self,
            )
            return

        main_window = self.main_window
        settings_window = self.settings_window

        if not hasattr(
            main_window,
            "apply_theme",
        ):
            messagebox.showerror(
                "خطا",
                "تابع apply_theme در پنجره اصلی یافت نشد.",
                parent=self,
            )
            return

        # ابتدا پنجره تنظیمات بسته می‌شود تا بازسازی رابط اصلی
        # باعث باقی‌ماندن پنجره خراب یا Grab نشود.
        try:
            settings_window.grab_release()
        except tk.TclError:
            pass

        try:
            settings_window.destroy()
        except tk.TclError:
            pass

        main_window.root.after(
            10,
            lambda: main_window.apply_theme(
                theme_key
            ),
        )

    def load_settings(self):
        current_theme = getattr(
            self.main_window,
            "current_theme",
            None,
        )

        if current_theme in THEMES:
            theme_name = THEMES[
                current_theme
            ].get(
                "label",
                current_theme,
            )

            self.theme_status_var.set(
                f"تم فعلی: {theme_name}"
            )
        else:
            self.theme_status_var.set(
                "تم فعلی: پیش‌فرض"
            )

    def save_settings(self):
        # تم در زمان انتخاب توسط apply_theme ذخیره می‌شود.
        return True