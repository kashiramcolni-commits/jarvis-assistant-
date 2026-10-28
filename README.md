# Jarvis (Android, Kotlin + Compose)

## Setup
1. Get a free AccessKey at console.picovoice.ai. Copy `local.properties.example` to `local.properties` and paste it in.
2. Open the folder in Android Studio (Ladybug or newer). It generates the Gradle wrapper. Sync and run on a real phone (Android 10+).
3. Open Jarvis and tap Grant on each Setup row, top to bottom.
4. Flip "Listen for Jarvis". Say "Jarvis", wait for the beep, then speak.

"Hey Jarvis" exactly: Porcupine ships a built-in "Jarvis" keyword. For the full phrase, train it in the Picovoice Console, drop the .ppn in `app/src/main/assets/`, and swap `.setKeyword(...)` for `.setKeywordPath("hey_jarvis.ppn")` in `WakeWordService`.

## Voice commands
- "turn on/off wifi", "turn on/off bluetooth"
- "volume up/down", "set volume to 40"
- "set brightness to 60"
- "open YouTube"
- "scroll down", "tap Settings", "go back", "go home"
- "send WhatsApp message to Mom saying I'll be late"
- "call Dad"

## Permission guide
| Permission | Why | How it is granted | Risk if abused |
|---|---|---|---|
| Microphone | Wake word, commands, call agent | Runtime prompt | Constant listening; keep audio on-device |
| Contacts | Name to number, known-caller check | Runtime prompt | Contact leak |
| Phone / Answer calls / Call log | Place and answer calls | Runtime prompt | Premium-rate calls |
| Accessibility | Read screen, tap, scroll | Settings > Accessibility > Jarvis | Can read everything on screen, including passwords and OTPs |
| Display over other apps | Start activities from background | Special access screen | Overlay phishing |
| Modify system settings | Brightness | Special access screen | Low |
| Call screening + Default phone app | Silence and answer calls | System role dialog | You own the entire call UI |

Security practices:
- Never put API keys in the APK. Anything LLM-based should go through your own backend.
- Do not log screen content from the accessibility service. This code reads nodes only to click them.
- Call transcripts live in app-private storage (`filesDir/calls.json`). Add encryption (Jetpack Security / Keystore) before storing anything sensitive.
- Sideload only. Google Play restricts accessibility apps that automate other apps and restricts call-audio access.

## Known platform limits (read before relying on it)
- **Call audio:** Android does not let third-party apps capture or inject the call audio stream. The call agent works by putting the call on speakerphone: TTS plays aloud and the mic hears the caller. Quality depends on the phone, the room and the OEM. Some phones mute the mic for non-system apps during calls.
- **Transcript, not a recording:** The summary comes from the speech-recognition text of what the mic heard (mostly the caller). There is no audio file.
- **Recording consent:** Many places legally require the caller's consent to record or transcribe. The greeting says so; check your local law.
- **Default dialer:** Becoming the default phone app makes Jarvis responsible for all call UI. This project only has answer/decline notifications. A real dialer needs an in-call screen (hang up, mute, keypad). Keep your normal dialer unless you build one.
- **Wi-Fi / Bluetooth:** Android 10+ blocks apps from toggling these. Jarvis taps the Quick Settings tile through accessibility, which can break on some OEM skins. It checks the current state first, so "turn on" never turns it off.
- **Wake word and battery:** Porcupine is light, but a microphone foreground service shows a persistent notification and some OEMs kill it. Exempt Jarvis from battery optimisation.
- **Sensitive screens:** Banking apps and password fields block accessibility actions on purpose.
