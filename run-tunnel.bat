@echo off
title Cinemax - Cloudflare Quick Tunnel (Port 8081)
echo ========================================================
echo   MENJALANKAN CLOUDFLARE QUICK TUNNEL UNTUK CINEMAX
echo ========================================================
echo.
echo Target Lokal : http://localhost:8081
echo.
echo PERHATIAN:
echo 1. Tunggu beberapa detik sampai Cloudflare menampilkan URL tunnel.
echo 2. Cari baris yang memuat: https://[nama-random].trycloudflare.com
echo 3. Buka Midtrans Merchant Administration Portal (MAP) Sandbox / Production.
echo 4. Masuk ke Settings -^> Configuration.
echo 5. Masukkan Payment Notification URL:
echo    https://[nama-random].trycloudflare.com/api/payment/notification
echo.
echo ========================================================
echo.
cloudflared tunnel --url http://localhost:8081
pause
