# ReadAloud

ReadAloud is a native Android app written in Kotlin and Jetpack Compose. It reads visible message text aloud using on-device accessibility extraction, local OCR fallback, local language normalization, and Android's local TextToSpeech engine.

The project intentionally has no `INTERNET` permission, no analytics SDK, no Firebase dependency, and no cloud OCR/TTS/translation/API calls.

## What Is Implemented

- Kotlin + Jetpack Compose app shell.
- Simple analysis screen with message cards, speaker buttons, Play All, Pause/Resume, and Stop.
- Settings for activation, speech language, speed, pitch, translation, roman conversion, OCR fallback, processed text, and highlight behavior.
- First-run onboarding for accessibility setup, offline voice checks, test speech, preferred language, and activation explanation.
- AccessibilityService that can retrieve visible window content, request the accessibility button, request key-event filtering, and request screenshots.
- Double volume-up or double volume-down detection through the AccessibilityService key-event filter.
- Accessibility button callback.
- Quick Settings tile activation.
- Accessibility node text extraction with ordering, duplicate removal, and UI-noise filtering.
- OCR fallback using `AccessibilityService.takeScreenshot()` and bundled on-device ML Kit Latin + Devanagari recognizers.
- Per-message language detection for English, Hindi, Hinglish, Gujarati, Roman Gujarati, and mixed Indian-language chat text.
- Hinglish normalization to Devanagari.
- Roman Gujarati normalization to Gujarati.
- Lightweight offline English-to-Hindi and English-to-Gujarati phrase/word translation for common chat phrases.
- Natural handling of common Indian-English words such as cricket, school, college, meeting, office, practice, match, mobile, laptop, and internet.
- TextToSpeech manager with offline voice selection, speech speed, pitch, queue playback, completion callbacks, error handling, stop, pause, and resume.
- Unit tests for core language-processing examples.

## Project Structure

```text
ReadAloud
|-- settings.gradle.kts
|-- build.gradle.kts
|-- gradle.properties
|-- app
    |-- build.gradle.kts
    |-- src/main/AndroidManifest.xml
    |-- src/main/java/com/readaloud/app
        |-- MainActivity.kt
        |-- ReadAloudApplication.kt
        |-- accessibility/ReadAloudAccessibilityService.kt
        |-- activation/AccessibilityActivation.kt
        |-- activation/ReadAloudTileService.kt
        |-- activation/VolumeButtonDetector.kt
        |-- analysis/AnalysisRepository.kt
        |-- extraction/AccessibilityTextExtractor.kt
        |-- extraction/MessageGrouper.kt
        |-- extraction/OcrTextExtractor.kt
        |-- language/HinglishProcessor.kt
        |-- language/IndicTransliterator.kt
        |-- language/LanguageDetector.kt
        |-- language/RomanGujaratiProcessor.kt
        |-- language/SpeechTextProcessor.kt
        |-- model/AppSettings.kt
        |-- model/Message.kt
        |-- settings/SettingsRepository.kt
        |-- speech/TtsManager.kt
        |-- translation/OfflineTranslationEngine.kt
        |-- ui/ReadAloudApp.kt
        |-- ui/ReadAloudViewModel.kt
    |-- src/test/java/com/readaloud/app/language/SpeechTextProcessorTest.kt
```

## Build Requirements

- Android Studio that supports Android SDK API 36 and Android Gradle Plugin 9.3.0.
- JDK 17.
- Android SDK Platform 36 installed for compilation.
- Android SDK Build Tools 36.0.0 or newer.
- A device or emulator running Android 11/API 30 or newer.

This workspace does not include a Gradle wrapper JAR because no downloads were performed while creating the project. Android Studio can still open and sync the project using its bundled Gradle. If you want CLI builds later, generate or add a Gradle 9.5.0 wrapper.

## Build Instructions

1. Open the `ReadAloud` folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Confirm the Android SDK Platform 36 and Build Tools are installed.
4. Build debug APK:

```bash
gradle :app:assembleDebug
```

Or use Android Studio:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build APK With GitHub Actions

If you do not want to install Android Studio locally, push this repository to GitHub and use the included workflow:

1. Open the GitHub repository.
2. Go to `Actions`.
3. Open `Build Debug APK`.
4. Click `Run workflow`.
5. Wait for the run to finish.
6. Open the finished run and download the `ReadAloud-debug-apk` artifact.
7. Extract the artifact ZIP. The APK inside can be installed on an Android phone for testing.

The workflow uses GitHub-hosted Linux runners, JDK 17, Android SDK Platform 36, Gradle 9.5.0, and Android Gradle Plugin 9.3.0.

## Installation

Install with Android Studio's Run button, or with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Accessibility Setup

1. Open ReadAloud.
2. On onboarding step 1, tap Open.
3. In Android Accessibility settings, enable `ReadAloud screen reader`.
4. Return to ReadAloud.
5. Confirm the app shows the service as enabled.

The service asks for:

- Window-content access so it can read visible accessibility nodes.
- Accessibility button support for reliable activation.
- Key-event filtering so double volume presses can be detected where Android/OEM policy allows it.
- Screenshot capability for local OCR fallback.

## Offline TTS Setup

1. Complete onboarding step 2.
2. Check Hindi, Gujarati, and English voice availability.
3. If a voice says Install needed, tap Install voices.
4. In Android TTS settings, install offline data for the missing language.
5. Return to ReadAloud and tap Refresh.

ReadAloud only selects voices whose Android `Voice.isNetworkConnectionRequired()` value is false when voice metadata is available.

## Testing With WhatsApp, SMS, Telegram, or Similar Apps

1. Install ReadAloud and enable its accessibility service.
2. Open WhatsApp, Telegram, SMS, Instagram, or another chat app.
3. Show the conversation messages on screen.
4. Activate ReadAloud with one of these methods:
   - Press Volume Up twice quickly.
   - Press Volume Down twice quickly.
   - Use the Android accessibility button for ReadAloud.
   - Add and tap the ReadAloud Quick Settings tile.
5. ReadAloud opens the analysis screen.
6. Tap a speaker button to read one message.
7. Tap Play All to read messages sequentially.
8. Try Pause, Resume, and Stop.

Suggested sample messages:

```text
Aaj cricket match hai?
Kal school jaana hai.
Tame kem cho?
Hu office ma chu.
Where are you going?
```

## Supported Android Versions

- Minimum: Android 11/API 30.
- Target: Android 16/API 36.
- Compile: API 36, matching the Android 16 target SDK available from the standard Android SDK manager.

## Known Android and Manufacturer Limitations

- Normal background apps cannot reliably intercept global hardware volume keys. ReadAloud uses the AccessibilityService key-event filtering capability, and returns `false` so normal volume behavior can continue. Some OEMs or Android builds may still not deliver volume key events to third-party accessibility services.
- The accessibility button is the reliable activation path when volume-key filtering is unavailable.
- Starting an activity from a background service can be restricted on modern Android. User-initiated accessibility/Quick Settings activation usually works better than passive background launches, but OEM behavior can vary.
- Some apps do not expose message text through accessibility nodes. ReadAloud cannot read text an app deliberately hides from accessibility.
- Secure windows cannot be captured by accessibility screenshots.
- OCR fallback is local and bundled, but the included ML Kit scripts are Latin and Devanagari. Native Gujarati messages are best handled through accessibility text; Roman Gujarati works through Latin OCR. A full native Gujarati screenshot-only OCR path would require bundling a separate Gujarati OCR model.
- Android TextToSpeech does not expose a true pause/resume API for the middle of one utterance. ReadAloud implements Pause by stopping speech and Resume by restarting the current message or current Play All item.
- Offline TTS availability depends on the installed Android TTS engine and language packs. ReadAloud does not fall back to online TTS.
- Manufacturer battery/background restrictions can affect Quick Settings or accessibility-service responsiveness. Keep ReadAloud unrestricted if a device aggressively stops background services.

## Privacy Notes

- Messages stay on the device.
- Screenshots used for OCR stay in memory and are recycled after OCR.
- No message text, OCR text, screenshots, usernames, or conversations are uploaded.
- The manifest declares no internet permission.
- Shared preferences store only settings, not conversations.

## Reference Notes

- Android AccessibilityService key-event filtering and accessibility button are official AccessibilityServiceInfo capabilities.
- `AccessibilityService.takeScreenshot()` is available from API 30 and requires the screenshot capability in service metadata.
- Android background activity launches have been restricted since Android 10 and tightened in later releases.
- ML Kit bundled text-recognition artifacts statically link OCR models into the app, unlike the Google Play Services dynamic-download artifacts.
- Android TTS `Voice.isNetworkConnectionRequired()` indicates whether a voice requires network access.
