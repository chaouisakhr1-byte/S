# Java conversion status

- Created a separate native Java Android project to preserve the previous advanced project untouched.
- Removed Expo/React Native/Kotlin/Gradle from this new project.
- Implemented Java screens and Supabase REST integration for auth, feed, video upload/playback, and comments.
- Added a direct Android SDK build script that does not invoke Gradle.
- APK build is currently blocked in this execution container because Android SDK tools (`aapt2`, `d8`, `zipalign`, `apksigner`) and `android.jar` are not installed. JDK 21 is available.
- This is not yet a verified APK and is not yet feature-complete versus the previous project.
