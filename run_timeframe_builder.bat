@echo off
chcp 65001 > nul

cd /d E:\chart_dna\scripts

echo ==========================================
echo Chart DNA - Timeframe Builder
echo ==========================================
echo.

python timeframe_builder.py

echo.
echo ==========================================
echo پردازش تمام شد.
echo برای خروج Enter را بزنید.
echo ==========================================
pause