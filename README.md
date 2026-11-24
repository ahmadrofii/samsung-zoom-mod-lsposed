# Samsung Cam Zoom Mod

Modul LSPosed untuk kamera bawaan Samsung (`com.sec.android.app.camera`, One UI 8, v16.0.00.66).
Target: Galaxy S20+ (root + LSPosed).

## Fitur
- Smooth zoom: perubahan zoom dihaluskan (easing logaritmik), durasi bisa diatur.
- Batas zoom maksimum dinaikkan (sampai 100x, sesuai data kurva zoom di APK kamera).

## Cara build
**Android Studio:** buka folder ini, Sync, Build > Build APK.
**GitHub Actions (tanpa Android Studio):** upload folder ini ke repo GitHub baru, buka tab Actions,
tunggu "Build APK" selesai, unduh artifact `SamsungCamZoomMod`.

## Pasang
1. Install APK, buka LSPosed > Modul > aktifkan "Samsung Cam Zoom Mod".
2. Scope: pilih **Kamera** (com.sec.android.app.camera).
3. Force Stop aplikasi Kamera, buka lagi.
4. Atur opsi di aplikasi modul. Perubahan terbaca live oleh kamera.

## Cek log kalau tidak jalan
LSPosed > Log, cari `[SamsungCamZoom]`. Harus muncul "hook max zoom terpasang" dan
"hook smooth zoom terpasang". Kalau muncul "gagal", kirim barisnya.

## Titik hook (dari bongkar APK kamera)
- `engine.core.CapabilityImpl.getScalerAvailableMaxDigitalZoom(ZoomType, Resolution)` -> max zoom
- `engine.ZoomController.setZoomLevel(int)` -> nilai zoom = rasio x 1000 (500 = 0.5x)
