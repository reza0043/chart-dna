"""
AnalysisRunnerMixin: اجرای تحلیل Chart DNA در ترد جدا، دریافت پیشرفت/نتیجه
از طریق صف پیام و اتمام تحلیل.
"""

import queue
import threading
import tkinter as tk
from tkinter import messagebox

from engine_core import analyze_files


class AnalysisRunnerMixin:
    # =========================================================
    # نوار پیشرفت + درصد عددی کنار آن
    # =========================================================
    def _mirror_builder_progress_to_settings(self, text, percent):
        """
        اگر پنجره تنظیمات (تب داده و تایم‌فریم) باز باشد، نوار درصد محلی
        همان تب را هم به‌روز می‌کند — چون آن پنجره Modal است و نوار
        درصد صفحه اصلی پشت آن دیده نمی‌شود.
        """
        if not self._is_window_alive(self.settings_window):
            return

        data_frame = getattr(self.settings_window, "data_frame", None)

        if data_frame is None:
            return

        try:
            data_frame.update_builder_progress(text, percent)
        except tk.TclError:
            pass

    def _set_progress(self, value):
        try:
            value = max(0, min(100, float(value)))
        except (TypeError, ValueError):
            value = 0

        self.progress["value"] = value

        if getattr(self, "progress_percent_var", None) is not None:
            self.progress_percent_var.set(f"{value:.0f}٪")

    # =========================================================
    # ابزارهای مربوط به محدودیت شباهت
    # =========================================================
    @staticmethod
    def _get_similarity_percent(result):
        """
        مقدار similarity را از نتیجه موتور می‌خواند و همیشه به درصد
        در بازه 0 تا 100 تبدیل می‌کند.

        موتور ممکن است شباهت را به صورت 0.85 یا 85.0 برگرداند.
        """
        try:
            similarity = float(result.get("similarity", 0))
        except (AttributeError, TypeError, ValueError):
            return 0.0

        # اگر موتور مقدار را در بازه صفر تا یک برگردانده باشد.
        if 0 <= similarity <= 1:
            similarity *= 100

        return max(0.0, min(similarity, 100.0))

    def _filter_results_by_threshold(self, results, threshold):
        """
        یک فیلتر نهایی در رابط کاربری اعمال می‌کند تا هیچ نتیجه‌ای پایین‌تر
        از حداقل شباهت انتخاب‌شده نمایش داده نشود.
        """
        filtered_results = []

        if not results:
            return filtered_results

        for result in results:
            if not isinstance(result, dict):
                continue

            similarity_percent = self._get_similarity_percent(result)

            if similarity_percent >= threshold:
                normalized_result = dict(result)
                normalized_result["similarity"] = similarity_percent
                filtered_results.append(normalized_result)

        filtered_results.sort(
            key=lambda item: item.get("similarity", 0),
            reverse=True,
        )

        return filtered_results

    # =========================================================
    # تحلیل Chart DNA
    # =========================================================
    def start_analysis(self):
        if self.analysis_running:
            messagebox.showinfo(
                "در حال اجرا",
                "تحلیل Chart DNA هنوز در حال اجرا است.",
            )
            return

        if not self.csv_files:
            messagebox.showwarning(
                "فایل CSV لازم است",
                "ابتدا حداقل یک فایل CSV بازار انتخاب کنید.",
            )
            return

        if self.reference_pattern is None:
            messagebox.showwarning(
                "الگوی مرجع لازم است",
                "ابتدا عکس را انتخاب، Crop و مسیر الگو را استخراج کنید.",
            )
            return

        try:
            threshold_text = str(self.threshold_var.get()).strip()
            future_text = str(self.future_var.get()).strip()
            pattern_length_text = str(self.pattern_length_var.get()).strip()

            if not threshold_text:
                raise ValueError("مقدار حداقل شباهت را وارد کنید.")

            if not future_text:
                raise ValueError("تعداد کندل‌های آینده را وارد کنید.")

            if not pattern_length_text:
                raise ValueError("تعداد نقاط شباهت را وارد کنید.")

            threshold = int(float(threshold_text))
            future = int(float(future_text))
            pattern_length = int(float(pattern_length_text))

            if threshold < 70 or threshold > 100:
                raise ValueError("حداقل شباهت باید بین 70 تا 100 درصد باشد.")

            if future < 1:
                raise ValueError("تعداد کندل آینده باید حداقل 1 باشد.")

            if pattern_length < 10 or pattern_length > 300:
                raise ValueError(
                    "تعداد نقاط شباهت باید بین 10 تا 300 باشد."
                )

        except (TypeError, ValueError) as error:
            messagebox.showerror(
                "تنظیمات نامعتبر",
                str(error),
            )
            return

        self.analysis_running = True
        self.start_analysis_button.config(state="disabled")

        self.results_table.delete(*self.results_table.get_children())
        self.results = []

        self._set_progress(0)
        self.status_var.set("وضعیت: تحلیل در حال اجرا است...")
        self.detail_var.set(
            f"داده‌ها در حال خواندن و مقایسه هستند. "
            f"حداقل شباهت انتخاب‌شده: {threshold}%"
        )

        thread = threading.Thread(
            target=self.analysis_worker,
            args=(threshold, future, pattern_length),
            daemon=True,
        )
        thread.start()

    def analysis_worker(self, threshold, future, pattern_length):
        try:
            results = analyze_files(
                reference_pattern=self.reference_pattern,
                csv_files=self.csv_files,
                threshold=threshold,
                future_candles=future,
                pattern_length=pattern_length,
                top_results=50,
                progress_callback=self.engine_progress,
            )

            # فیلتر نهایی و قطعی بر اساس حداقل شباهت انتخاب‌شده در UI
            filtered_results = self._filter_results_by_threshold(
                results=results,
                threshold=threshold,
            )

            self.message_queue.put(("done", filtered_results, threshold))

        except Exception as error:
            self.message_queue.put(("error", str(error)))

    def engine_progress(self, text, percent):
        try:
            percent = float(percent)
        except (TypeError, ValueError):
            percent = 0

        percent = max(0, min(percent, 100))
        self.message_queue.put(("progress", text, percent))

    # =========================================================
    # دریافت پیام Thread ها
    # =========================================================
    def process_messages(self):
        try:
            while True:
                message = self.message_queue.get_nowait()
                kind = message[0]

                if kind == "progress":
                    _, text, percent = message
                    self.detail_var.set(str(text))
                    self._set_progress(percent)

                elif kind == "done":
                    _, results, threshold = message

                    self.analysis_running = False
                    self.start_analysis_button.config(state="normal")

                    self.analysis_finished(
                        results=results,
                        threshold=threshold,
                    )

                elif kind == "error":
                    _, error_text = message

                    self.analysis_running = False
                    self.start_analysis_button.config(state="normal")

                    self.status_var.set("وضعیت: خطا در تحلیل")
                    self.detail_var.set(error_text)
                    self._set_progress(0)

                    messagebox.showerror(
                        "خطا در تحلیل",
                        error_text,
                    )

                elif kind == "builder_progress":
                    _, text, percent = message

                    self.status_var.set(
                        "وضعیت: ساخت تایم‌فریم‌ها در حال اجرا است..."
                    )
                    self.detail_var.set(text)
                    self._set_progress(percent)
                    self._mirror_builder_progress_to_settings(text, percent)

                elif kind == "builder_done":
                    _, symbol, symbol_dir = message

                    self.timeframe_builder_running = False

                    if hasattr(self, "build_timeframes_button"):
                        self.build_timeframes_button.config(state="normal")

                    self._set_progress(100)
                    self._mirror_builder_progress_to_settings(
                        "ساخت تایم‌فریم‌ها با موفقیت تمام شد.",
                        100,
                    )

                    self.status_var.set(
                        f"وضعیت: ساخت تایم‌فریم‌های نماد {symbol} با موفقیت تمام شد."
                    )

                    self.detail_var.set(
                        f"فایل‌های نماد {symbol} در این مسیر قرار دارند:\n{symbol_dir}"
                    )

                    # بروزرسانی خودکار جدول تنظیمات پس از پایان ساخت
                    if self.settings_window is not None:
                        try:
                            self.refresh_timeframe_table()
                        except tk.TclError:
                            pass

                    messagebox.showinfo(
                        "ساخت تایم‌فریم‌ها تمام شد",
                        f"فایل‌های M1 نماد {symbol} ذخیره شدند و "
                        "timeframe_builder.py با موفقیت اجرا شد.\n\n"
                        f"مسیر داده نماد:\n{symbol_dir}",
                    )

                elif kind == "builder_error":
                    _, error_text = message

                    self.timeframe_builder_running = False

                    if hasattr(self, "build_timeframes_button"):
                        self.build_timeframes_button.config(state="normal")

                    self.status_var.set("وضعیت: خطا در ساخت تایم‌فریم‌ها")
                    self.detail_var.set(error_text)
                    self._set_progress(0)
                    self._mirror_builder_progress_to_settings(error_text, 0)

                    messagebox.showerror(
                        "خطا در ساخت تایم‌فریم‌ها",
                        error_text,
                    )

        except queue.Empty:
            pass

        self.root.after(100, self.process_messages)

    def analysis_finished(self, results, threshold):
        self.results = results
        self._set_progress(100)

        if not results:
            self.status_var.set(
                "وضعیت: تحلیل تمام شد؛ نتیجه معتبر پیدا نشد."
            )
            self.detail_var.set(
                f"هیچ الگویی با حداقل شباهت {threshold}% پیدا نشد. "
                "حداقل شباهت را کاهش دهید یا Crop دقیق‌تری انتخاب کنید."
            )
            return

        for rank, result in enumerate(results, start=1):
            if rank == 1:
                row_tag = "top_result"
            elif rank % 2 == 0:
                row_tag = "even_row"
            else:
                row_tag = "odd_row"

            self.results_table.insert(
                "",
                "end",
                iid=str(rank - 1),
                tags=(row_tag,),
                values=(
                    rank,
                    f"{result.get('similarity', 0):.2f}%",
                    result.get("file_name", "-"),
                    result.get("start_time", "-"),
                    result.get("start_index", "-"),
                ),
            )

        self.status_var.set(
            f"وضعیت: تحلیل با موفقیت تمام شد. "
            f"{len(results)} نتیجه با شباهت حداقل {threshold}% پیدا شد."
        )

        self.detail_var.set(
            "نتایج در output/chart_dna_matches.csv ذخیره می‌شوند. "
            "برای مشاهده هر نتیجه روی سطر آن کلیک کنید."
        )

        first_item = self.results_table.get_children()[0]
        self.results_table.selection_set(first_item)
        self.show_selected_result()