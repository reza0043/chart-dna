"""
پنجره تنظیمات برنامه Chart DNA
"""

import tkinter as tk
from tkinter import messagebox, ttk

from ui.settings.analysis_params_frame import AnalysisParamsFrame
from ui.settings.data_timeframe_frame import DataTimeframeFrame
from ui.settings.similarity_settings import SimilaritySettingsFrame
from ui.settings.theme_settings_frame import ThemeSettingsFrame


class SettingsWindow(tk.Toplevel):
    def __init__(
        self,
        parent,
        main_window,
        initial_tab=0,
    ):
        super().__init__(parent)

        self.main_window = main_window

        self.title("تنظیمات Chart DNA")
        self.geometry("850x680")
        self.minsize(760, 580)
        self.resizable(True, True)

        self.protocol(
            "WM_DELETE_WINDOW",
            self.close_window,
        )

        self.notebook = ttk.Notebook(self)
        self.notebook.pack(
            fill="both",
            expand=True,
            padx=10,
            pady=10,
        )

        # تب صفر: داده و تایم‌فریم
        self.data_frame = DataTimeframeFrame(
            self.notebook,
            self,
        )
        self.data_frame.main_window = self.main_window

        self.notebook.add(
            self.data_frame,
            text="داده و تایم‌فریم",
        )

        # تب یک: معیارهای شباهت
        self.similarity_frame = SimilaritySettingsFrame(
            self.notebook,
            self,
        )
        self.similarity_frame.main_window = self.main_window

        self.notebook.add(
            self.similarity_frame,
            text="معیارهای شباهت",
        )

        # تب دو: پارامترهای تحلیل
        self.analysis_frame = AnalysisParamsFrame(
            self.notebook,
            self,
        )
        self.analysis_frame.main_window = self.main_window

        self.notebook.add(
            self.analysis_frame,
            text="پارامترهای تحلیل",
        )

        # تب سه: تنظیمات نرم‌افزار
        self.theme_frame = ThemeSettingsFrame(
            self.notebook,
            self,
        )
        self.theme_frame.main_window = self.main_window

        self.notebook.add(
            self.theme_frame,
            text="تنظیمات نرم‌افزار",
        )

        try:
            initial_tab = int(initial_tab)

            if initial_tab < 0 or initial_tab > 3:
                initial_tab = 0
        except (TypeError, ValueError):
            initial_tab = 0

        self.notebook.select(initial_tab)

        button_frame = ttk.Frame(self)
        button_frame.pack(
            fill="x",
            padx=10,
            pady=(0, 10),
        )

        ttk.Button(
            button_frame,
            text="ذخیره تنظیمات",
            command=self.save_settings,
        ).pack(
            side="right",
            padx=5,
        )

        ttk.Button(
            button_frame,
            text="بستن",
            command=self.close_window,
        ).pack(
            side="right",
            padx=5,
        )

        self.load_current_settings()

        self.transient(parent)
        self.grab_set()

    def load_current_settings(self):
        """
        بارگذاری تنظیمات همه تب‌ها.
        """
        self.similarity_frame.load_settings()
        self.analysis_frame.load_settings()
        self.theme_frame.load_settings()

        try:
            self.data_frame.load_data()
        except Exception:
            pass

    def save_settings(self):
        """
        ذخیره تنظیمات همه تب‌ها.
        """
        try:
            # هماهنگ کردن انتخاب فایل/تایم‌فریم تب «داده و تایم‌فریم» با
            # self.csv_files، حتی اگر کاربر دکمه‌ی مخصوص آن تب را نزده و
            # مستقیم از همین‌جا «ذخیره تنظیمات» را زده باشد.
            try:
                self.data_frame._sync_widgets_to_main_window()
                self.main_window.sync_selected_files_from_settings(
                    show_warning=False,
                )
            except Exception:
                pass

            similarity_result = (
                self.similarity_frame.save_settings()
            )

            if similarity_result is False:
                self.notebook.select(
                    self.similarity_frame
                )
                return

            self.analysis_frame.save_settings()
            self.theme_frame.save_settings()

            messagebox.showinfo(
                "تنظیمات",
                "تنظیمات با موفقیت ذخیره شد.",
                parent=self,
            )

            self.close_window()

        except Exception as error:
            messagebox.showerror(
                "خطا در ذخیره تنظیمات",
                str(error),
                parent=self,
            )

    def close_window(self):
        try:
            self.grab_release()
        except tk.TclError:
            pass

        try:
            self.destroy()
        except tk.TclError:
            pass