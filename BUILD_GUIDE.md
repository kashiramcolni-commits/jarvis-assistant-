# Building the Jarvis debug APK

You need ONE of the three routes below. Route A needs no computer setup.

## Route A: GitHub Actions (no SDK install, works from a phone)
1. Create a private GitHub repo (branch `main`) and upload every file from this project, including the `.github` folder.
2. Settings > Secrets and variables > Actions > New repository secret:
   name `PICOVOICE_ACCESS_KEY`, value = your key from https://console.picovoice.ai
3. Actions tab > "Build Jarvis APK" > Run workflow.
4. When it finishes, open the run and download the artifact `Jarvis-debug-apk`. It contains `app-debug.apk`.
5. Copy the APK to your phone and install it (allow "Install unknown apps" for your file manager or browser).

## Route B: Android Studio
1. Install Android Studio (latest stable). It installs the SDK, JDK 17 and Gradle for you.
2. Copy `local.properties.example` to `local.properties` and set `PICOVOICE_ACCESS_KEY`.
   Android Studio adds the `sdk.dir` line itself.
3. File > Open > select this folder. Wait for Gradle sync (first sync downloads dependencies).
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Route C: Command line only (Linux/macOS, needs internet)
```bash
# 1. JDK 17
sudo apt install openjdk-17-jdk            # macOS: brew install openjdk@17

# 2. Android command-line tools (get the current zip name from developer.android.com/studio#command-line-tools-only)
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools
unzip ~/Downloads/commandlinetools-*_latest.zip && mv cmdline-tools latest

# 3. Environment
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 4. SDK packages
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# 5. Gradle 8.9 (or run `gradle wrapper --gradle-version 8.9` once, then use ./gradlew)
#    https://gradle.org/releases  -> unzip and add bin/ to PATH

# 6. Build
cd /path/to/Jarvis
cp local.properties.example local.properties   # edit: PICOVOICE_ACCESS_KEY=...
echo "sdk.dir=$ANDROID_HOME" >> local.properties
gradle :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# 7. Install on a phone with USB debugging on
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting
- "SDK location not found": add `sdk.dir=/path/to/android-sdk` to `local.properties`.
- "License not accepted": run `yes | sdkmanager --licenses`.
- Porcupine "invalid AccessKey" at runtime: the key was empty at build time; rebuild after setting it.
- Compile error: send the first error line and file name and it can be fixed quickly.
