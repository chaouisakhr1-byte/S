# S ☆ — Native Android Java build

This is a native Android rewrite scaffold for package `com.sstar.video`. It does not use Expo, React Native, Kotlin, Node.js, npm, or Gradle. Android UI and app logic are Java. Supabase is accessed through Android's built-in `HttpURLConnection` and REST endpoints.

## Implemented in this Java revision
- Email/password sign-in and sign-up against Supabase Auth REST API.
- Persistent access token and user ID in Android SharedPreferences.
- Public video feed retrieval from `public.videos`.
- Video playback using Android `VideoView`.
- Device video selection using Android document picker.
- Upload to the `s-media` Supabase Storage bucket and insert video metadata.
- Comment creation against `public.comments`.
- Sign-out and basic profile/session screen.

## Build without Gradle
Install Android SDK Platform 35 and Build Tools 36.0.0, plus JDK 17 or 21. Then set `ANDROID_HOME` and run:

```sh
./build-apk.sh
```

The script uses `aapt2`, `javac`, `d8`, `zipalign`, `keytool`, and `apksigner` directly. It writes a debug-signed APK to `build/out/S-debug.apk`.

## Important limits
- This is the first Java-native conversion pass, not feature parity with the prior React Native project.
- Live streaming transport (RTMP/RTMPS/WebRTC), guest co-hosting, real-time chat, creator follow/like UI, and profile editing are not implemented in this Java pass.
- Sign-up may require email confirmation depending on Supabase Auth settings.
- Supabase RLS and Storage policies must permit the signed-in user operations. Never embed a service-role key in the app; only the publishable client key is used.
- No APK is included until the build script has been run successfully with Android SDK installed.
