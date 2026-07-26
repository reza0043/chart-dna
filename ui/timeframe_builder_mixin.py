"""
TimeframeBuilderMixin: انتخاب فایل‌های M1 و اجرای اسکریپت timeframe_builder.py
به صورت subprocess/thread، و باز کردن پوشه‌ی data.
"""
import os
import re
import shutil
import subprocess
import sys
import threading
from pathlib import Path
from tkinter import filedialog, messagebox

from core.config import DATA_DIR, TIMEFRAME_BUILDER_PATH, BASE_DIR


class TimeframeBuilderMixin:

    # =========================================================
    # فایل‌های M1 و اجرای timeframe_builder
    # =========================================================
    def select_m1_files(self):
        paths = filedialog.askopenfilenames(
            title="انتخاب فایل یا فایل‌های یک دقیقه‌ای M1",
            filetypes=[
                ("CSV files", "*.csv"),
                ("Text files", "*.txt"),
                ("All files", "*.*"),
            ],
        )

        if not paths:
            return

        self.m1_upload_files = list(paths)

        names = [Path(path).name for path in self.m1_upload_files]
        preview = " | ".join(names[:4])

        if len(names) > 4:
            preview += f" | ... ({len(names)} فایل)"

        if hasattr(self, "m1_files_label"):
            self.m1_files_label.config(
                text=f"فایل‌های M1 انتخاب‌شده: {preview}"
            )

        self.status_var.set("وضعیت: فایل‌های M1 انتخاب شدند.")
        self.detail_var.set(
            "نام نماد را وارد کنید؛ سپس دکمه «ذخیره و ساخت تایم‌فریم‌ها» را بزنید."
        )

    def get_clean_symbol(self):
        symbol = self.symbol_var.get().strip().upper()
        symbol = re.sub(r"\s+", "", symbol)
        symbol = re.sub(r"[^A-Z0-9_.-]", "", symbol)

        if not symbol:
            raise ValueError(
                "نام نماد را وارد کنید. نمونه: XAUUSD ، XAGUSD ، EURUSD یا BITUSD"
            )

        return symbol

    def start_timeframe_builder(self):
        if self.timeframe_builder_running:
            messagebox.showinfo(
                "در حال اجرا",
                "فرآیند ساخت تایم‌فریم‌ها هنوز در حال اجرا است.",
            )
            return

        if not self.m1_upload_files:
            messagebox.showwarning(
                "فایل M1 لازم است",
                "ابتدا یک یا چند فایل یک دقیقه‌ای را انتخاب کنید.",
            )
            return

        try:
            symbol = self.get_clean_symbol()
        except ValueError as error:
            messagebox.showwarning("نام نماد نامعتبر", str(error))
            return

        if not TIMEFRAME_BUILDER_PATH.exists():
            messagebox.showerror(
                "فایل timeframe_builder.py پیدا نشد",
                "فایل موردنیاز پیدا نشد:\n\n"
                f"{TIMEFRAME_BUILDER_PATH}\n\n"
                "مسیر صحیح مورد انتظار:\n"
                "E:\\chart_dna\\scripts\\timeframe_builder.py",
            )
            return

        self.timeframe_builder_running = True

        if hasattr(self, "build_timeframes_button"):
            self.build_timeframes_button.config(state="disabled")

        self.progress["value"] = 0
        self._mirror_builder_progress_to_settings(
            f"آماده‌سازی فایل‌های M1 برای {symbol}...",
            0,
        )
        self.status_var.set(f"وضعیت: آماده‌سازی فایل‌های M1 برای {symbol}...")
        self.detail_var.set("در حال کپی فایل‌ها به پوشه data نماد...")

        thread = threading.Thread(
            target=self.timeframe_builder_worker,
            args=(symbol, list(self.m1_upload_files)),
            daemon=True,
        )
        thread.start()

    def timeframe_builder_worker(self, symbol, source_files):
        try:
            symbol_dir = DATA_DIR / symbol
            symbol_dir.mkdir(parents=True, exist_ok=True)

            total_files = len(source_files)

            for index, source_file in enumerate(source_files, start=1):
                source_path = Path(source_file)

                if not source_path.exists():
                    raise FileNotFoundError(
                        f"فایل انتخاب‌شده پیدا نشد:\n{source_path}"
                    )

                destination_path = symbol_dir / source_path.name

                progress_percent = int(((index - 1) / total_files) * 35)

                self.message_queue.put(
                    (
                        "builder_progress",
                        (
                            f"در حال ذخیره فایل {index} از {total_files}: "
                            f"{source_path.name}"
                        ),
                        progress_percent,
                    )
                )

                try:
                    same_file = (
                        source_path.resolve()
                        == destination_path.resolve()
                    )
                except OSError:
                    same_file = False

                if not same_file:
                    shutil.copy2(source_path, destination_path)

            self.message_queue.put(
                (
                    "builder_progress",
                    (
                        f"فایل‌های M1 در مسیر زیر ذخیره شدند:\n{symbol_dir}\n"
                        "در حال اجرای timeframe_builder.py ..."
                    ),
                    40,
                )
            )

            environment = os.environ.copy()

            environment["CHART_DNA_BASE_DIR"] = str(BASE_DIR)
            environment["CHART_DNA_DATA_DIR"] = str(DATA_DIR)
            environment["CHART_DNA_SYMBOL"] = symbol
            environment["CHART_DNA_SYMBOL_DIR"] = str(symbol_dir)

            command = [
                sys.executable,
                str(TIMEFRAME_BUILDER_PATH),
            ]

            process = subprocess.Popen(
                command,
                cwd=str(BASE_DIR),
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                stdin=subprocess.DEVNULL,
                text=True,
                encoding="utf-8",
                errors="replace",
                env=environment,
                creationflags=getattr(
                    subprocess,
                    "CREATE_NO_WINDOW",
                    0,
                ),
            )

            output_lines = []

            # تعداد مراحل شناخته‌شده‌ی ساخت تایم‌فریم (برای محاسبه‌ی درصد
            # واقعی پیشرفت در طول اجرای اسکریپت، به‌جای عدد ثابت).
            total_timeframe_stages = 8
            completed_stages = 0

            if process.stdout is not None:
                for line in process.stdout:
                    line = line.strip()

                    if not line:
                        continue

                    output_lines.append(line)

                    if len(output_lines) > 30:
                        output_lines.pop(0)

                    if "در حال ساخت تایم‌فریم" in line:
                        completed_stages = min(
                            completed_stages + 1,
                            total_timeframe_stages,
                        )

                    stage_percent = 40 + (
                        completed_stages
                        / total_timeframe_stages
                        * 55
                    )

                    self.message_queue.put(
                        (
                            "builder_progress",
                            f"timeframe_builder.py:\n{line}",
                            stage_percent,
                        )
                    )

            return_code = process.wait()

            if return_code != 0:
                last_messages = "\n".join(output_lines[-15:])

                error_message = (
                    "اجرای timeframe_builder.py با خطا متوقف شد.\n\n"
                    f"کد خروج: {return_code}\n\n"
                    f"مسیر فایل اجراشده:\n{TIMEFRAME_BUILDER_PATH}"
                )

                if last_messages:
                    error_message += (
                        "\n\nآخرین پیام‌های برنامه:\n"
                        f"{last_messages}"
                    )

                raise RuntimeError(error_message)

            self.message_queue.put(
                (
                    "builder_done",
                    symbol,
                    symbol_dir,
                )
            )

        except Exception as error:
            self.message_queue.put(("builder_error", str(error)))

    def open_data_folder(self):
        try:
            DATA_DIR.mkdir(parents=True, exist_ok=True)
            os.startfile(DATA_DIR)
        except Exception as error:
            messagebox.showerror("خطا در باز کردن پوشه Data", str(error))