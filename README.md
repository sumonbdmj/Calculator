# Simple Calculator Deign

A premium Android calculator built with **Kotlin, Jetpack Compose, Material 3, MVVM, Room, CameraX and Google ML Kit OCR**.

- **Unlimited precision** — BigInteger/BigDecimal engine. Exact results up to 100,000 digits (e.g. `25000!`, `2^300000`), never silently truncated. Only non‑terminating values (1 ÷ 3, √2, π, sin) are rounded, to a precision you choose (20–200 digits), and they are always marked **≈**.
- **Calculator** — + − × ÷, %, brackets, √, x², xʸ, 1/x, ±, MC/MR/M+/M−, backspace (hold = clear), live preview, copy, horizontal scrolling for long numbers, full-result viewer.
- **Scientific mode** — sin/cos/tan (exact at special angles), log, ln, π, e, n!, DEG/RAD.
- **Smart Scanner** — CameraX capture or gallery → crop / rotate / brightness / contrast / B&W → on-device ML Kit OCR → reviewable, editable expressions → results with step-by-step explanations, written-answer checking, optional totals, save to history, share as text or PDF, save PDF via the Storage Access Framework.
  OCR never assumes every number is a calculation: dates/times are ignored, lone numbers are listed separately for you to select, and corrected characters or low-confidence (often handwritten) lines are flagged **Please verify**.
- **Step-by-step screen** for any calculation (from calculator, history or scanner).
- **History** — Room database, search, grouped by day, tap to reuse, copy, show steps, delete, clear all.
- **Tools** — Percentage (5 modes), Discount (stacked discounts + tax), GST/VAT (add/remove), Loan/EMI (chart + full schedule), Unit Converter (10 categories incl. Bangladeshi land & weight units).
- **AI Math Assistant** — offline tutor solves percentages, discounts, interest, EMI, linear & quadratic equations and word expressions exactly, on-device. Optionally connects to **your own secure backend** (`/backend`) for open-ended questions — no API key is ever stored in the APK.
- **Settings** — Light/Dark/System theme, sound, vibration, precision, decimal places, number format (1,234,567 / 12,34,567 / none), angle unit, clear history, privacy policy, about, version.

## Build & install

### Android Studio (recommended)
1. Install **Android Studio** (Ladybug 2024.2 or newer) with the Android SDK 35.
2. **File → Open…** and select the `SimpleCalculatorDeign` folder.
3. Wait for **Gradle Sync** to finish (first sync downloads dependencies — needs internet).
4. Connect a phone with **USB debugging** on (or start an emulator from Device Manager).
5. Press **Run ▶**.

### Command line
```bash
cd SimpleCalculatorDeign
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew test                   # runs the math-engine / OCR-parser / finance unit tests
```

### Release APK / Play Store bundle
**Build → Generate Signed App Bundle / APK…**, create a keystore, choose *release*. Release builds are minified and resource-shrunk.

Requirements: JDK 17 (bundled with Android Studio), min Android 8.0 (API 26), target API 35.

## Optional: AI backend
See [`backend/README.md`](backend/README.md). In short: run `node backend/server.js` with your provider key in an environment variable and add `ai.backend.url=...` to `local.properties`. Without it, the assistant still works offline.

## Project structure
```
app/src/main/java/com/simplecalculatordeign/
├── CalculatorApp.kt            Application + manual DI container
├── MainActivity.kt             Splash API, edge-to-edge, theme from settings
├── core/math/                  Pure Kotlin (unit-tested, no Android deps)
│   ├── Tokenizer.kt, Parser.kt, Evaluator.kt, MathEngine.kt   expression engine + steps
│   ├── BigMath.kt              arbitrary-precision √, exp, ln, log, sin, cos, tan, π
│   ├── NumberFormatter.kt      grouping, fixed decimals, full-precision plain output
│   ├── InputEditor.kt          keypad editing rules (±, 1/x, smart operators…)
│   ├── ExpressionExtractor.kt  OCR text → reviewable expressions / loose numbers
│   ├── FinanceCalculators.kt   percentage, discount, VAT, EMI, interest
│   ├── UnitConverter.kt        BigDecimal unit conversion
│   └── LocalMathTutor.kt       offline step-by-step tutor
├── data/
│   ├── db/HistoryDatabase.kt   Room entity, DAO, database
│   └── repository/             HistoryRepository, SettingsRepository (DataStore), AiRepository
└── ui/
    ├── theme/                  colours, gradients, typography, shapes (light + dark)
    ├── components/             premium cards, animated keys, buttons, fields…
    ├── navigation/             routes, NavHost, bottom navigation
    ├── onboarding/             Splash + Onboarding
    ├── calculator/             ViewModel, display, basic + scientific screens, mode menu
    ├── scanner/                ViewModel, camera, crop, OCR, results, steps, PDF export
    ├── history/                History screen + ViewModel
    ├── tools/                  Percentage, Discount, GST/VAT, Loan/EMI, Unit converter
    ├── assistant/              AI Math Assistant
    └── settings/               Settings, About, Privacy policy
backend/                        Optional Node.js AI proxy (keeps secret keys off the device)
```

## Screens (18)
Splash · Onboarding · Calculator · Scientific · Scanner home · Camera · Crop/adjust · OCR processing · Scan results · Step-by-step · History · Percentage · Discount · GST/VAT · Loan/EMI · Unit converter · AI Assistant · Settings · About (+ Privacy policy)

## Privacy & security
No ads, analytics or accounts. Images are processed on-device and never uploaded. History stays in a private on-device database (backups disabled). Network access is HTTPS-only (clear-text only for a local dev backend). Camera permission is requested at runtime with a settings fallback if denied.
