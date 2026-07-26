"""
StyleMixin: ساخت Style های ttk بر اساس تم انتخاب‌شده و اعمال زنده تم جدید
(بدون نیاز به بستن و باز کردن مجدد برنامه).
"""
import tkinter as tk
from tkinter import ttk

from core.themes import DEFAULT_THEME, THEMES


class StyleMixin:
    # =========================================================
    # ساخت Style های ttk بر اساس تم انتخاب‌شده (قابل فراخوانی مجدد)
    # =========================================================
    def configure_style(self, theme_key):
        style = self.ttk_style

        try:
            style.theme_use("clam")
        except tk.TclError:
            pass

        # -----------------------------------------------------
        # رنگ‌های تم انتخاب‌شده
        # -----------------------------------------------------
        palette = THEMES.get(theme_key, THEMES[DEFAULT_THEME])

        BG = palette["bg"]
        PANEL = palette["panel"]
        PANEL_DARK = palette["panel_dark"]
        NEON_BLUE = palette["accent"]
        NEON_BLUE_BRIGHT = palette["accent_bright"]
        TEXT = palette["text"]
        MUTED = palette["muted"]
        BORDER = palette["border"]
        SELECTED = palette["selected"]

        BUTTON_BG = palette["button_bg"]
        BUTTON_PRESSED = palette["button_pressed"]
        BUTTON_ACTIVE = palette["button_active"]
        BUTTON_DISABLED = palette["button_disabled"]
        BUTTON_DISABLED_FG = palette["button_disabled_fg"]
        FIELD_BG = palette["field_bg"]
        FIELD_DARK = palette["field_dark"]
        TREEVIEW_HEADING_BG = palette["treeview_heading_bg"]
        SCROLLBAR_BG = palette["scrollbar_bg"]

        self.root.configure(bg=BG)

        # -----------------------------------------------------
        # تنظیم Style عمومی برنامه
        # -----------------------------------------------------
        style.configure(
            ".",
            background=BG,
            foreground=TEXT,
            font=("Segoe UI", 10),
        )

        style.configure(
            "TFrame",
            background=BG,
        )

        style.configure(
            "Neon.TFrame",
            background=PANEL,
        )

        style.configure(
            "TLabel",
            background=BG,
            foreground=TEXT,
        )

        style.configure(
            "Muted.TLabel",
            background=BG,
            foreground=MUTED,
        )

        style.configure(
            "Title.TLabel",
            background=BG,
            foreground=NEON_BLUE_BRIGHT,
            font=("Segoe UI", 23, "bold"),
        )

        style.configure(
            "Subtitle.TLabel",
            background=BG,
            foreground=NEON_BLUE,
            font=("Segoe UI", 10),
        )

        style.configure(
            "TLabelframe",
            background=PANEL,
            foreground=NEON_BLUE_BRIGHT,
            bordercolor=BORDER,
            lightcolor=BORDER,
            darkcolor=FIELD_DARK,
            borderwidth=2,
            relief="groove",
            padding=12,
        )

        style.configure(
            "TLabelframe.Label",
            background=PANEL,
            foreground=NEON_BLUE_BRIGHT,
            font=("Segoe UI", 11, "bold"),
        )

        style.configure(
            "TButton",
            background=BUTTON_BG,
            foreground=TEXT,
            bordercolor=NEON_BLUE,
            lightcolor=NEON_BLUE,
            darkcolor=FIELD_DARK,
            padding=(16, 9),
            font=("Segoe UI", 9, "bold"),
        )

        style.map(
            "TButton",
            background=[
                ("pressed", BUTTON_PRESSED),
                ("active", BUTTON_ACTIVE),
                ("disabled", BUTTON_DISABLED),
            ],
            foreground=[
                ("disabled", BUTTON_DISABLED_FG),
                ("active", "#ffffff"),
            ],
            bordercolor=[
                ("active", NEON_BLUE_BRIGHT),
                ("pressed", NEON_BLUE_BRIGHT),
            ],
        )

        style.configure(
            "TEntry",
            fieldbackground=FIELD_BG,
            foreground=TEXT,
            insertcolor=NEON_BLUE_BRIGHT,
            bordercolor=NEON_BLUE,
            lightcolor=NEON_BLUE,
            darkcolor=FIELD_DARK,
            padding=5,
        )

        style.configure(
            "TSpinbox",
            fieldbackground=FIELD_BG,
            foreground=TEXT,
            arrowcolor=NEON_BLUE_BRIGHT,
            bordercolor=NEON_BLUE,
            lightcolor=NEON_BLUE,
            darkcolor=FIELD_DARK,
            padding=4,
        )

        style.configure(
            "TCheckbutton",
            background=PANEL,
            foreground=TEXT,
        )

        style.map(
            "TCheckbutton",
            background=[
                ("active", PANEL),
                ("selected", PANEL),
            ],
            foreground=[
                ("active", NEON_BLUE_BRIGHT),
                ("selected", NEON_BLUE_BRIGHT),
            ],
        )

        style.configure(
            "Horizontal.TProgressbar",
            troughcolor=FIELD_BG,
            background=NEON_BLUE_BRIGHT,
            bordercolor=NEON_BLUE,
            lightcolor=NEON_BLUE_BRIGHT,
            darkcolor=FIELD_DARK,
            thickness=16,
        )

        style.configure(
            "Treeview",
            background=FIELD_BG,
            fieldbackground=FIELD_BG,
            foreground=TEXT,
            bordercolor=BORDER,
            lightcolor=BORDER,
            darkcolor=FIELD_DARK,
            rowheight=32,
            font=("Segoe UI", 10),
        )

        style.configure(
            "Treeview.Heading",
            background=TREEVIEW_HEADING_BG,
            foreground=NEON_BLUE_BRIGHT,
            bordercolor=NEON_BLUE,
            font=("Segoe UI", 9, "bold"),
        )

        style.map(
            "Treeview",
            background=[
                ("selected", SELECTED),
            ],
            foreground=[
                ("selected", "#ffffff"),
            ],
        )

        style.configure(
            "TScrollbar",
            background=SCROLLBAR_BG,
            troughcolor=FIELD_BG,
            arrowcolor=NEON_BLUE_BRIGHT,
            bordercolor=BORDER,
        )
        return palette
