"""
نقطه شروع اجرای برنامه Chart DNA.
اجرای این فایل، پنجره اصلی برنامه را باز می‌کند.
"""
import tkinter as tk

from ui.main_window import ChartDNAApp


def main():
    root = tk.Tk()
    app = ChartDNAApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
