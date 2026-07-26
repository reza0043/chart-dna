"""
LayoutMixin: ساخت ویجت‌های صفحه اصلی (داشبورد تحلیل) — پنل کنترل‌ها،
ناحیه تصویر/کراپ، جدول نتایج و نمودار.
"""
import tkinter as tk
from tkinter import ttk

from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from matplotlib.figure import Figure


class LayoutMixin:
    # =========================================================
    # ساخت ویجت‌های صفحه اصلی (داشبورد تحلیل)
    # =========================================================
    def create_dashboard(self, palette):

        # -----------------------------------------------------
        # تنظیمات تحلیل
        # کلید «انتخاب فایل CSV برای تحلیل» حذف شده است.
        # -----------------------------------------------------
        controls = ttk.LabelFrame(
            self.root,
            text="۲) تنظیمات تحلیل Chart DNA",
            padding=10,
        )
        controls.pack(fill="x", padx=10, pady=(0, 8))

        ttk.Button(
            controls,
            text="انتخاب تصویر نمودار",
            command=self.select_image,
        ).grid(row=0, column=0, padx=5, pady=4)

        ttk.Button(
            controls,
            text="پاک کردن انتخاب‌های تحلیل",
            command=self.clear_all,
        ).grid(row=0, column=1, padx=5, pady=4)

        ttk.Button(
            controls,
            text="استخراج مسیر از محدوده انتخاب‌شده",
            command=self.extract_reference,
        ).grid(row=0, column=2, padx=5, pady=4)

        ttk.Button(
            controls,
            text="نمایش Crop ذخیره‌شده",
            command=self.show_crop_file,
        ).grid(row=0, column=3, padx=5, pady=4)

        self.start_analysis_button = ttk.Button(
            controls,
            text="شروع تحلیل Chart DNA",
            command=self.start_analysis,
        )
        self.start_analysis_button.grid(
            row=0,
            column=4,
            padx=(25, 5),
        )

        # نوار درصد پیشرفت تحلیل، متناسب با کندل‌های بررسی‌شده از فایل‌های
        # انتخاب‌شده — درست زیر دکمه‌ی «شروع تحلیل».
        progress_row = ttk.Frame(controls)
        progress_row.grid(
            row=1,
            column=0,
            columnspan=5,
            sticky="ew",
            pady=(10, 0),
        )

        self.progress = ttk.Progressbar(
            progress_row,
            mode="determinate",
            maximum=100,
            style="Horizontal.TProgressbar",
        )
        self.progress.pack(
            side="left",
            fill="x",
            expand=True,
        )

        self.progress_percent_var = tk.StringVar(value="۰٪")

        ttk.Label(
            progress_row,
            textvariable=self.progress_percent_var,
            font=("Segoe UI", 10, "bold"),
            width=6,
            anchor="center",
        ).pack(side="left", padx=(8, 0))

        files_frame = ttk.Frame(self.root, padding=(10, 0))
        files_frame.pack(fill="x")

        self.csv_label = ttk.Label(
            files_frame,
            text="فایل‌های تحلیل از جدول تایم‌فریم‌ها انتخاب می‌شوند.",
            style="Muted.TLabel",
        )
        self.csv_label.pack(anchor="w")

        self.image_label = ttk.Label(
            files_frame,
            text="تصویر انتخاب نشده است.",
            style="Muted.TLabel",
        )
        self.image_label.pack(anchor="w", pady=(2, 5))

        # -----------------------------------------------------
        # پنل اصلی
        # -----------------------------------------------------
        main = ttk.PanedWindow(
            self.root,
            orient="horizontal",
        )
        main.pack(
            fill="both",
            expand=True,
            padx=10,
            pady=5,
        )

        left = ttk.Frame(main, padding=5)
        right = ttk.Frame(main, padding=5)

        main.add(left, weight=4)
        main.add(right, weight=3)

        image_box = ttk.LabelFrame(
            left,
            text="۳) تصویر نمودار — با ماوس محدوده الگو را انتخاب کنید",
            padding=5,
        )
        image_box.pack(fill="both", expand=True)

        self.canvas = tk.Canvas(
            image_box,
            bg=palette["canvas_bg"],
            cursor="crosshair",
            highlightthickness=1,
            highlightbackground=palette["accent"],
            highlightcolor=palette["accent_bright"],
        )
        self.canvas.pack(fill="both", expand=True)

        self.canvas.bind(
            "<ButtonPress-1>",
            self.crop_begin,
        )
        self.canvas.bind(
            "<B1-Motion>",
            self.crop_move,
        )
        self.canvas.bind(
            "<ButtonRelease-1>",
            self.crop_finish,
        )

        crop_bar = ttk.Frame(left)
        crop_bar.pack(fill="x", pady=6)

        ttk.Label(
            crop_bar,
            text="فقط خود نمودار را Crop کنید؛ نوشته‌ها، دکمه‌ها و محور قیمت را وارد نکنید.",
            style="Muted.TLabel",
        ).pack(side="left", padx=4)

        result_box = ttk.LabelFrame(
            right,
            text="۴) نتایج مشابهت",
            padding=5,
        )
        result_box.pack(fill="both", expand=True)

        columns = (
            "rank",
            "similarity",
            "file",
            "time",
            "index",
        )

        self.results_table = ttk.Treeview(
            result_box,
            columns=columns,
            show="headings",
            height=13,
        )

        self.results_table.heading("rank", text="#")
        self.results_table.heading("similarity", text="شباهت")
        self.results_table.heading("file", text="فایل")
        self.results_table.heading("time", text="زمان شروع")
        self.results_table.heading("index", text="اندیس")

        self.results_table.column(
            "rank",
            width=40,
            anchor="center",
        )
        self.results_table.column(
            "similarity",
            width=75,
            anchor="center",
        )
        self.results_table.column(
            "file",
            width=150,
        )
        self.results_table.column(
            "time",
            width=125,
        )
        self.results_table.column(
            "index",
            width=70,
            anchor="center",
        )

        scrollbar = ttk.Scrollbar(
            result_box,
            orient="vertical",
            command=self.results_table.yview,
        )

        self.results_table.configure(
            yscrollcommand=scrollbar.set,
        )

        self.results_table.pack(
            side="left",
            fill="both",
            expand=True,
        )
        scrollbar.pack(
            side="right",
            fill="y",
        )

        self.results_table.bind(
            "<<TreeviewSelect>>",
            self.show_selected_result,
        )

        # نواربندی زبرا (زبرا-استرایپ) و برجسته کردن بهترین نتیجه،
        # هماهنگ با پالت تم فعال.
        self.results_table.tag_configure(
            "odd_row",
            background=palette["field_bg"],
        )
        self.results_table.tag_configure(
            "even_row",
            background=palette["panel_dark"],
        )
        self.results_table.tag_configure(
            "top_result",
            background=palette["selected"],
            foreground=palette["accent_bright"],
        )

        chart_box = ttk.LabelFrame(
            right,
            text="مسیر الگو و نتیجه انتخاب‌شده",
            padding=5,
        )
        chart_box.pack(
            fill="both",
            expand=True,
            pady=(8, 0),
        )

        self.figure = Figure(
            figsize=(6, 4),
            dpi=100,
            facecolor=palette["chart_bg"],
        )

        self.axis = self.figure.add_subplot(111)
        self.axis.set_facecolor(palette["chart_bg"])
        self.axis.set_title(
            "هنوز الگویی استخراج نشده است.",
            color=palette["accent_bright"],
        )
        self.axis.grid(
            True,
            alpha=0.25,
            color=palette["grid_color"],
        )

        self.axis.tick_params(
            colors=palette["tick_color"],
        )

        for spine in self.axis.spines.values():
            spine.set_color(palette["spine_color"])

        self.figure_canvas = FigureCanvasTkAgg(
            self.figure,
            master=chart_box,
        )
        self.figure_canvas.get_tk_widget().configure(
            background=palette["chart_bg"],
            highlightthickness=0,
        )
        self.figure_canvas.get_tk_widget().pack(
            fill="both",
            expand=True,
        )

        status_box = ttk.LabelFrame(
            self.root,
            text="وضعیت اجرا",
            padding=8,
        )
        status_box.pack(
            fill="x",
            padx=10,
            pady=(4, 10),
        )

        ttk.Label(
            status_box,
            textvariable=self.status_var,
            font=("Segoe UI", 10, "bold"),
        ).pack(anchor="w")

        ttk.Label(
            status_box,
            textvariable=self.detail_var,
            style="Muted.TLabel",
        ).pack(anchor="w", pady=(4, 0))

    # =========================================================
    # پنجره تنظیمات داده و تایم‌فریم
    # =========================================================