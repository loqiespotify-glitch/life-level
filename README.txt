LIFE LEVEL Android 0.5.0 — native Android project

WHAT IS INCLUDED
- Current LIFE LEVEL web UI bundled locally in the Android app (WebView).
- Health Connect read permission for daily step totals.
- Native Android notification channel and background reminders:
  * water reminder interval, default 2 hours
  * fasting window start and end times
- Alarm rescheduling after device reboot.
- App-local progress remains in WebView localStorage.

BUILD
1. Install Android Studio (current stable) and Android SDK Platform 36.
2. Open this folder in Android Studio.
3. Allow Gradle sync and install any requested SDK components.
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. The debug APK is usually generated at app/build/outputs/apk/debug/app-debug.apk.

DEVICE SETUP
- Android 13 and lower: install/update Health Connect from Google Play if the device does not have it.
- Android 14+: Health Connect is part of system settings on supported devices.
- Open LIFE LEVEL, tap “Синхронизировать шаги”, and grant READ_STEPS.
- Grant notification permission when Android asks.
- Save water and fasting settings to schedule native reminders.
- If the phone aggressively restricts background activity, allow LIFE LEVEL notifications/background activity in battery settings.

IMPORTANT LIMITATIONS
- This is an Android Studio source project, not a prebuilt APK.
- It has not been compiled or tested on a physical Android device in this environment because Android SDK/Gradle are not installed here.
- AlarmManager.setAndAllowWhileIdle is intentionally used without requesting the special exact-alarm permission. Android may defer these notifications during Doze/battery saving; do not treat them as guaranteed to-the-minute alarms.
- Health Connect returns steps from sources connected to Health Connect. Availability depends on the phone, Android version, provider apps, and granted permissions.
- The app currently stores its own progress in WebView localStorage. Clearing app data or uninstalling the app can remove that local progress. Export your data from the app before reinstalling.
- The water target is a user-configured habit goal, not medical advice.

TECH
- Kotlin, AndroidX Activity, Health Connect Client 1.1.0
- minSdk 26, targetSdk 35, compileSdk 36

Health Connect permission rationale screen is included in the manifest and app source.

BUILD WITHOUT INSTALLING ANDROID STUDIO LOCALLY
A GitHub Actions workflow is included at .github/workflows/build-apk.yml.
Upload this project to a GitHub repository, push it to the main branch, then open Actions > Build LIFE LEVEL APK. Download the LIFE-LEVEL-debug-APK artifact from the completed run.

When the app opens or returns to the foreground, it refreshes today's aggregated step count automatically if Health Connect permission has already been granted. Full periodic Health Connect reads while the app is closed are not implemented in this build.
