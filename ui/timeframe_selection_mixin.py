"""
TimeframeSelectionMixin: اسکن پوشه data، ساخت جدول تایم‌فریم‌ها و مدیریت
انتخاب نماد/تایم‌فریم‌های مورد استفاده در تحلیل.
"""
import tkinter as tk
from tkinter import messagebox, ttk

from core.config import BASE_TIMEFRAMES, DATA_DIR, TIMEFRAME_ORDER, TIMEFRAME_PATTERN


class TimeframeSelectionMixin:
    # =========================================================
    # اسکن فایل‌های data و نمایش جدول تایم‌فریم
    # =========================================================
    def detect_timeframe_from_filename(self, filename):
        match = TIMEFRAME_PATTERN.search(filename.upper())

        if match:
            return match.group(1).upper()

        return None

    def scan_data_directory(self):
        result = {}

        if not DATA_DIR.exists():
            return result

        for symbol_dir in DATA_DIR.iterdir():
            if not symbol_dir.is_dir():
                continue

            symbol = symbol_dir.name.upper()
            timeframe_files = {}

            for file_path in symbol_dir.rglob("*"):
                if not file_path.is_file():
                    continue

                if file_path.suffix.lower() not in [".csv", ".txt"]:
                    continue

                timeframe = self.detect_timeframe_from_filename(file_path.name)

                if timeframe:
                    timeframe_files.setdefault(timeframe, []).append(file_path)

            if timeframe_files:
                result[symbol] = timeframe_files

        return result

    def sort_timeframes(self, timeframes):
        return sorted(
            timeframes,
            key=lambda item: TIMEFRAME_ORDER.get(item, 999999),
        )

    def refresh_timeframe_table(self):
        if self.settings_table_inner is None:
            return

        previous_selection = {}

        for symbol, tf_vars in self.timeframe_selection_vars.items():
            previous_selection[symbol] = {}

            for timeframe, variable in tf_vars.items():
                previous_selection[symbol][timeframe] = variable.get()

        self.data_map = self.scan_data_directory()

        all_timeframes = set(BASE_TIMEFRAMES)

        for symbol_files in self.data_map.values():
            all_timeframes.update(symbol_files.keys())

        self.available_timeframes = self.sort_timeframes(all_timeframes)

        self.timeframe_selection_vars = {}
        self.symbol_selection_vars = {}

        for symbol, symbol_files in self.data_map.items():
            self.timeframe_selection_vars[symbol] = {}

            for timeframe in self.available_timeframes:
                if timeframe in symbol_files:
                    old_value = previous_selection.get(symbol, {}).get(
                        timeframe,
                        1,
                    )

                    self.timeframe_selection_vars[symbol][timeframe] = (
                        tk.IntVar(value=old_value)
                    )

            variables = list(
                self.timeframe_selection_vars[symbol].values()
            )

            all_selected = bool(variables) and all(
                variable.get() == 1
                for variable in variables
            )

            self.symbol_selection_vars[symbol] = tk.IntVar(
                value=1 if all_selected else 0
            )

        self.build_timeframe_table()
        self.update_timeframe_selection_summary()

    def build_timeframe_table(self):
        if self.settings_table_inner is None:
            return

        for child in self.settings_table_inner.winfo_children():
            child.destroy()

        headers = ["نماد"] + self.available_timeframes + ["تعداد فایل انتخاب‌شده"]

        for column_index, title in enumerate(headers):
            width = 18 if title == "نماد" else 9

            if title == "تعداد فایل انتخاب‌شده":
                width = 18

            label = ttk.Label(
                self.settings_table_inner,
                text=title,
                anchor="center",
                font=("Segoe UI", 9, "bold"),
                relief="groove",
                padding=6,
                width=width,
            )
            label.grid(
                row=0,
                column=column_index,
                sticky="nsew",
                padx=1,
                pady=1,
            )

        if not self.data_map:
            ttk.Label(
                self.settings_table_inner,
                text=(
                    "هنوز هیچ فایل تایم‌فریمی در پوشه data وجود ندارد. "
                    "ابتدا فایل M1 را انتخاب و تایم‌فریم‌ها را بسازید."
                ),
                anchor="center",
                padding=25,
            ).grid(
                row=1,
                column=0,
                columnspan=len(headers),
                sticky="ew",
            )

            self.update_settings_table_scroll()
            return

        for row_index, symbol in enumerate(
            sorted(self.data_map.keys()),
            start=1,
        ):
            symbol_check = ttk.Checkbutton(
                self.settings_table_inner,
                text=symbol,
                variable=self.symbol_selection_vars[symbol],
                command=lambda current_symbol=symbol: (
                    self.toggle_all_symbol_timeframes(current_symbol)
                ),
            )

            symbol_check.grid(
                row=row_index,
                column=0,
                sticky="nsew",
                padx=1,
                pady=1,
            )

            for column_index, timeframe in enumerate(
                self.available_timeframes,
                start=1,
            ):
                if timeframe in self.timeframe_selection_vars[symbol]:
                    check = ttk.Checkbutton(
                        self.settings_table_inner,
                        variable=self.timeframe_selection_vars[symbol][timeframe],
                        command=lambda current_symbol=symbol: (
                            self.update_symbol_selection_state(current_symbol)
                        ),
                    )

                    check.grid(
                        row=row_index,
                        column=column_index,
                        sticky="nsew",
                        padx=1,
                        pady=1,
                    )

                else:
                    ttk.Label(
                        self.settings_table_inner,
                        text="—",
                        anchor="center",
                        foreground="#777777",
                        relief="groove",
                        padding=6,
                    ).grid(
                        row=row_index,
                        column=column_index,
                        sticky="nsew",
                        padx=1,
                        pady=1,
                    )

            selected_count = self.get_symbol_selected_file_count(symbol)

            ttk.Label(
                self.settings_table_inner,
                text=f"{selected_count} فایل",
                anchor="center",
                relief="groove",
                padding=6,
            ).grid(
                row=row_index,
                column=len(self.available_timeframes) + 1,
                sticky="nsew",
                padx=1,
                pady=1,
            )

        self.update_settings_table_scroll()

    def update_settings_table_scroll(self, event=None):
        if self.settings_canvas is None:
            return

        self.settings_canvas.configure(
            scrollregion=self.settings_canvas.bbox("all")
        )

    def get_symbol_selected_file_count(self, symbol):
        total = 0

        for timeframe, variable in self.timeframe_selection_vars.get(
            symbol,
            {},
        ).items():
            if variable.get() == 1:
                total += len(
                    self.data_map.get(symbol, {}).get(timeframe, [])
                )

        return total

    def toggle_all_symbol_timeframes(self, symbol):
        checked = self.symbol_selection_vars[symbol].get()

        for variable in self.timeframe_selection_vars[symbol].values():
            variable.set(checked)

        self.build_timeframe_table()
        self.update_timeframe_selection_summary()

    def update_symbol_selection_state(self, symbol):
        variables = list(
            self.timeframe_selection_vars[symbol].values()
        )

        all_selected = bool(variables) and all(
            variable.get() == 1
            for variable in variables
        )

        self.symbol_selection_vars[symbol].set(
            1 if all_selected else 0
        )

        self.build_timeframe_table()
        self.update_timeframe_selection_summary()

    def select_all_timeframes(self):
        for symbol, tf_vars in self.timeframe_selection_vars.items():
            for variable in tf_vars.values():
                variable.set(1)

            self.symbol_selection_vars[symbol].set(1)

        self.build_timeframe_table()
        self.update_timeframe_selection_summary()

    def clear_all_timeframes(self):
        for symbol, tf_vars in self.timeframe_selection_vars.items():
            for variable in tf_vars.values():
                variable.set(0)

            self.symbol_selection_vars[symbol].set(0)

        self.build_timeframe_table()
        self.update_timeframe_selection_summary()

    def update_timeframe_selection_summary(self):
        if not hasattr(self, "settings_selection_summary"):
            return

        selected_file_count = 0
        selected_symbols = set()
        selected_timeframes = set()

        for symbol, tf_vars in self.timeframe_selection_vars.items():
            for timeframe, variable in tf_vars.items():
                if variable.get() == 1:
                    files = self.data_map.get(symbol, {}).get(
                        timeframe,
                        [],
                    )

                    if files:
                        selected_file_count += len(files)
                        selected_symbols.add(symbol)
                        selected_timeframes.add(timeframe)

        self.settings_selection_summary.set(
            f"انتخاب‌شده: {selected_file_count} فایل | "
            f"{len(selected_symbols)} نماد | "
            f"{len(selected_timeframes)} تایم‌فریم"
        )

    def sync_selected_files_from_settings(self, show_warning=True):
        """
        بازسازی self.csv_files از روی چک‌باکس‌های تایم‌فریم انتخاب‌شده در
        تنظیمات. این تابع بدون نمایش پیام موفقیت و بدون بستن پنجره است تا
        هم از دکمه‌ی «تایید و بستن» تب داده و هم از دکمه‌ی عمومی «ذخیره
        تنظیمات» قابل استفاده باشد.

        Returns:
            list | None: لیست فایل‌های انتخاب‌شده، یا None اگر چیزی
            انتخاب نشده باشد.
        """
        selected_files = []

        for symbol, tf_vars in self.timeframe_selection_vars.items():
            for timeframe, variable in tf_vars.items():
                if variable.get() == 1:
                    files = self.data_map.get(symbol, {}).get(
                        timeframe,
                        [],
                    )
                    selected_files.extend(files)

        if not selected_files:
            if show_warning:
                messagebox.showwarning(
                    "فایلی انتخاب نشده است",
                    "حداقل یک تایم‌فریم را برای تحلیل انتخاب کنید.",
                )
            return None

        self.csv_files = [str(file_path) for file_path in selected_files]

        names = [file_path.name for file_path in selected_files]
        preview = " | ".join(names[:4])

        if len(names) > 4:
            preview += f" | ... ({len(names)} فایل)"

        if getattr(self, "csv_label", None) is not None:
            self.csv_label.config(
                text=f"CSV انتخاب‌شده برای تحلیل: {preview}"
            )

        self.status_var.set(
            f"وضعیت: {len(selected_files)} فایل برای تحلیل انتخاب شد."
        )

        self.detail_var.set(
            "فایل‌های انتخاب‌شده از بخش تنظیمات آماده تحلیل هستند. "
            "اکنون تصویر نمودار را انتخاب و Crop کنید."
        )

        return selected_files

    def confirm_timeframe_selection(self):
        selected_files = self.sync_selected_files_from_settings(
            show_warning=True,
        )

        if selected_files is None:
            return

        messagebox.showinfo(
            "انتخاب‌ها ثبت شد",
            f"{len(selected_files)} فایل برای تحلیل Chart DNA انتخاب شد.",
        )

        if self._is_window_alive(self.settings_window):
            try:
                self.settings_window.destroy()
            except tk.TclError:
                pass

        self.settings_window = None