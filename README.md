# Auto Slot OTP (Android)

Small Android app that reads the IVAC OTP SMS on your phone and sends the code to the **Auto Slot Pro Hack** extension.

## Build (Android Studio, once)
1. Install Android Studio. **File → Open** → select this `android-otp-app` folder. Let Gradle sync (needs internet the first time).
2. Connect your phone (USB debugging on) → **Run ▶**, or **Build → Build APK(s)** and copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and install it.
   (This project could not be compiled in the environment where it was written – if Android Studio shows a version warning, accept its suggested update.)

## Use
1. Extension → **OTP tab → Phone auto-OTP**: set the relay server address, copy the **pairing code**.
2. Phone app: enter the same server address and pairing code → **Save & allow SMS** (allow the SMS permission) → **Send test OTP**. The extension's status line should say it received the test code.
3. Normal use: open the IVAC login page, press the website's SEND OTP. When the SMS arrives the code appears in the website's OTP box; you press the website's verify button.

Notes: the phone and the extension must reach the same relay server (see `server-example`); the code is kept in the server's memory for 3 minutes and deleted when read. Keep the app unrestricted in battery settings so Android does not stop it. Only SMS containing the keyword (default `IVAC`) are forwarded.
