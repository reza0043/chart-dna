"""
ChartDNAApp:
کلاس اصلی برنامه — سربرگ، داشبورد و اتصال همه Mixinها
"""

import queue
import tkinter as tk
from tkinter import ttk

from core.config import (
    APP_TITLE,
    BASE_TIMEFRAMES,
    PATTERN_LENGTH,
    load_app_settings,
    save_app_settings,
)
from core.themes import DEFAULT_THEME, THEMES

from ui.style_mixin import StyleMixin
from ui.dashboard import DashboardMixin
from ui.timeframe_selection_mixin import TimeframeSelectionMixin
from ui.timeframe_builder_mixin import TimeframeBuilderMixin
from ui.settings import SettingsWindow


class ChartDNAApp(
    StyleMixin,
    DashboardMixin,
    TimeframeSelectionMixin,
    TimeframeBuilderMixin,
):
    """
    پنجره اصلی نرم‌افزار Chart DNA
    """

    def __init__(self, root):
        self.root = root
        self.main_window = self

        self.root.title(APP_TITLE)
        self.root.geometry("1450x900")
        self.root.minsize(1100, 700)

        # ---------------------------------------------------------
        # تنظیمات نرم‌افزار
        # ---------------------------------------------------------
        self.app_settings = load_app_settings()

        self.current_theme = self.app_settings.get(
            "theme",
            DEFAULT_THEME,
        )

        if self.current_theme not in THEMES:
            self.current_theme = DEFAULT_THEME

        self.current_palette = None
        self.ttk_style = None

        self.settings_window = None
        self.app_settings_window = None
        self.theme_status_var = None

        # ---------------------------------------------------------
        # فایل‌های تحلیل
        # ---------------------------------------------------------
        self.csv_files = []
        self.m1_upload_files = []

        # ---------------------------------------------------------
        # تنظیمات داده و تایم‌فریم
        # ---------------------------------------------------------
        self.data_map = {}
        self.timeframe_selection_vars = {}
        self.symbol_selection_vars = {}
        self.available_timeframes = list(BASE_TIMEFRAMES)

        self.settings_table_inner = None
        self.settings_canvas = None

        # ---------------------------------------------------------
        # تصویر و تحلیل
        # ---------------------------------------------------------
        self.image_path = None
        self.original_image = None
        self.tk_image = None

        self.display_scale = 1.0
        self.image_offset_x = 0
        self.image_offset_y = 0

        self.crop_start = None
        self.crop_rect = None
        self.reference_pattern = None

        self.results = []
        self.analysis_running = False
        self.timeframe_builder_running = False

        self.message_queue = queue.Queue()

        # ---------------------------------------------------------
        # متغیرهای عمومی رابط کاربری
        # ---------------------------------------------------------
        self.symbol_var = tk.StringVar()

        self.threshold_var = tk.IntVar(
            value=self.app_settings.get(
                "min_similarity",
                70,
            )
        )

        self.future_var = tk.IntVar(
            value=self.app_settings.get(
                "future_candles",
                10,
            )
        )

        self.pattern_length_var = tk.IntVar(
            value=self.app_settings.get(
                "pattern_length",
                PATTERN_LENGTH,
            )
        )

        self.status_var = tk.StringVar(
            value="وضعیت: آماده"
        )

        self.detail_var = tk.StringVar(
            value=(
                "ابتدا فایل M1 را وارد کنید یا فایل CSV "
                "و تصویر نمودار را انتخاب کنید."
            )
        )

        # این ویجت‌ها در create_interface ساخته یا بازسازی می‌شوند.
        self.csv_label = None
        self.progress = None
        self.dashboard_container = None

        # ساخت رابط کاربری
        self.create_interface()

        # پردازش پیام‌های ترد تحلیل
        self.root.after(
            100,
            self.process_messages,
        )

    # =================================================================
    # ساخت رابط کاربری
    # =================================================================

    def create_interface(self):
        """
        ساخت یا بازسازی کامل رابط کاربری.
        این تابع هنگام تغییر تم دوباره اجرا می‌شود.
        """

        self.ttk_style = ttk.Style(self.root)

        palette = self.configure_style(
            self.current_theme
        )

        # حذف اجزای قبلی رابط کاربری
        for child in self.root.winfo_children():
            child.destroy()

        # ---------------------------------------------------------
        # سربرگ
        # ---------------------------------------------------------
        top = ttk.Frame(
            self.root,
            padding=10,
        )
        top.pack(fill="x")

        ttk.Label(
            top,
            text="CHART DNA",
            style="Title.TLabel",
        ).pack(side="left")

        ttk.Label(
            top,
            text=(
                "تشخیص ساختار و حرکت‌های مشابه قیمت "
                "از روی تصویر نمودار"
            ),
            style="Subtitle.TLabel",
        ).pack(
            side="left",
            padx=18,
        )

        # دکمه تنظیمات (شامل داده/تایم‌فریم، معیارهای شباهت، پارامترهای
        # تحلیل و تنظیمات نرم‌افزار — همه در یک پنجره‌ی تب‌دار)
        ttk.Button(
            top,
            text="⚙ تنظیمات",
            command=self.open_data_settings,
        ).pack(
            side="right",
            padx=5,
        )

        # ---------------------------------------------------------
        # خط جداکننده‌ی نازک زیر سربرگ، هم‌رنگ با تم فعال
        # ---------------------------------------------------------
        divider = tk.Frame(
            self.root,
            height=2,
            bg=palette["accent"],
        )
        divider.pack(fill="x")

        # ---------------------------------------------------------
        # داشبورد
        # ---------------------------------------------------------
        self.dashboard_container = ttk.Frame(self.root)
        self.dashboard_container.pack(
            fill="both",
            expand=True,
        )

        self.create_dashboard(palette)

        # ---------------------------------------------------------
        # نوار پیشرفت
        # ---------------------------------------------------------
        self.progress = ttk.Progressbar(
            self.root,
            mode="determinate",
            maximum=100,
        )

        self.progress.pack(
            fill="x",
            padx=10,
            pady=(0, 5),
        )

        # ذخیره پالت فعال
        self.current_palette = palette

    # =================================================================
    # مدیریت پنجره تنظیمات
    # =================================================================

    def _is_window_alive(self, window):
        """
        بررسی اینکه پنجره Toplevel هنوز باز است یا خیر.
        """

        if window is None:
            return False

        try:
            return bool(window.winfo_exists())
        except tk.TclError:
            return False

    def _focus_existing_settings_window(self, window):
        """
        فعال‌کردن پنجره تنظیمات موجود، برای جلوگیری از باز شدن
        چند پنجره تنظیمات هم‌زمان.
        """

        try:
            window.deiconify()
            window.lift()
            window.focus_force()
        except tk.TclError:
            pass

    def _select_settings_tab(self, settings_window, initial_tab):
        """
        انتخاب تب مورد نظر در پنجره تنظیمات موجود.

        این متد باعث می‌شود اگر پنجره تنظیمات از قبل باز باشد،
        با فشردن دکمه‌های سربرگ، کاربر مستقیماً به تب مرتبط منتقل شود.
        """

        try:
            initial_tab = int(initial_tab)
        except (TypeError, ValueError):
            initial_tab = 0

        try:
            notebook = getattr(
                settings_window,
                "notebook",
                None,
            )

            if notebook is None:
                return

            tab_count = len(notebook.tabs())

            if tab_count <= 0:
                return

            if initial_tab < 0 or initial_tab >= tab_count:
                initial_tab = 0

            notebook.select(initial_tab)

        except (tk.TclError, AttributeError):
            pass

    def _register_settings_window(self, settings_window):
        """
        ثبت پنجره تنظیمات و پاک‌کردن مراجع آن پس از بسته‌شدن پنجره.
        """

        self.settings_window = settings_window
        self.app_settings_window = settings_window

        def on_destroy(event):
            if event.widget is not settings_window:
                return

            if self.settings_window is settings_window:
                self.settings_window = None

            if self.app_settings_window is settings_window:
                self.app_settings_window = None

        try:
            settings_window.bind(
                "<Destroy>",
                on_destroy,
                add="+",
            )
        except tk.TclError:
            pass

    def _attach_main_window_reference(self, settings_window):
        """
        اتصال مرجع ChartDNAApp به پنجره تنظیمات و تمام فریم‌های
        داخلی آن.

        فریم‌های تنظیمات برای اعمال تم، ساخت تایم‌فریم، خواندن
        داده‌ها و ذخیره تنظیمات به main_window نیاز دارند.
        """

        if settings_window is None:
            return

        try:
            settings_window.main_window = self
        except Exception:
            pass

        def attach_to_children(widget):
            try:
                widget.main_window = self
            except Exception:
                pass

            try:
                children = widget.winfo_children()
            except Exception:
                children = []

            for child in children:
                attach_to_children(child)

        try:
            attach_to_children(settings_window)
        except Exception:
            pass

        possible_frame_names = (
            "data_frame",
            "app_frame",
            "software_frame",
            "appearance_frame",
            "theme_frame",
            "similarity_frame",
            "similarity_settings_frame",
            "analysis_frame",
        )

        for frame_name in possible_frame_names:
            try:
                frame = getattr(
                    settings_window,
                    frame_name,
                    None,
                )

                if frame is not None:
                    frame.main_window = self
            except Exception:
                pass

    def _open_settings_window(self, initial_tab):
        """
        ایجاد پنجره تنظیمات یا فعال‌کردن پنجره باز موجود.

        اگر پنجره تنظیمات از قبل باز باشد، پنجره جدید ساخته نمی‌شود
        و تب مورد نظر در همان پنجره انتخاب خواهد شد.
        """

        if self._is_window_alive(self.settings_window):
            self._focus_existing_settings_window(
                self.settings_window
            )

            self._select_settings_tab(
                self.settings_window,
                initial_tab,
            )

            return self.settings_window

        settings_window = SettingsWindow(
            self.root,
            main_window=self,
            initial_tab=initial_tab,
        )

        self._register_settings_window(settings_window)
        self._attach_main_window_reference(settings_window)

        return settings_window

    # =================================================================
    # تغییر تم
    # =================================================================

    def apply_theme(self, theme_key):
        """
        اعمال تم انتخاب‌شده و ذخیره آن برای اجرای بعدی برنامه.
        """

        if theme_key not in THEMES:
            return

        self.current_theme = theme_key
        self.app_settings["theme"] = theme_key

        save_app_settings(self.app_settings)

        # پس از تغییر تم، پنجره تنظیمات فعلی بسته می‌شود تا تمام
        # ویجت‌ها با تم جدید در اجرای بعدی ساخته شوند.
        if self._is_window_alive(self.settings_window):
            try:
                self.settings_window.destroy()
            except tk.TclError:
                pass

        self.settings_window = None
        self.app_settings_window = None

        # بازسازی کامل رابط کاربری با تم جدید
        self.create_interface()

        # به‌روزرسانی متن وضعیت تم در صورت وجود
        if self.theme_status_var is not None:
            try:
                theme_label = THEMES[theme_key].get(
                    "label",
                    theme_key,
                )

                self.theme_status_var.set(
                    f"تم فعال: {theme_label}"
                )
            except tk.TclError:
                pass

    # =================================================================
    # پنجره تنظیمات داده و تایم‌فریم
    # =================================================================

    def open_data_settings(self):
        """
        باز کردن تب تنظیمات داده و تایم‌فریم.
        """

        settings_window = self._open_settings_window(
            initial_tab=0,
        )

        try:
            data_frame = getattr(
                settings_window,
                "data_frame",
                None,
            )

            if data_frame is not None:
                data_frame.main_window = self
                data_frame.load_data()

        except Exception:
            pass

    # =================================================================
    # پنجره تنظیمات نرم‌افزار و تم
    # =================================================================

    def open_app_settings(self):
        """
        باز کردن تب تنظیمات نرم‌افزار و انتخاب تم.
        """

        self._open_settings_window(
            initial_tab=3,
        )

    # =================================================================
    # پنجره تنظیمات معیارهای شباهت
    # =================================================================

    def open_similarity_settings(self):
        """
        باز کردن تب تنظیمات معیارهای شباهت.
        """

        self._open_settings_window(
            initial_tab=1,
        )