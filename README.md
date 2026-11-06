# S ☆ — Native Android application

Existing application identity is preserved:
- Package / application ID: `com.sstar.video`
- Repository: `chaouisakhr1-byte/S`
- Backend: the existing Supabase project configured in `Supabase.java`

## Android build

The primary build is now the Android Gradle Plugin build in `app/`. Java sources remain in the existing `src/` tree, and the existing root `AndroidManifest.xml` and `res/` are used by the app module.

GitHub Actions builds `:app:assembleDebug` and uploads `app-debug.apk`.

## Current application areas

- Supabase email/password authentication and persisted session.
- Public video feed, local video picker, Supabase Storage upload and playback.
- Video comments.
- Creator follow and video likes using existing Supabase tables.
- Profile editor.
- Live Studio entry in the home screen.
- RTMP camera publisher screen implemented with RootEncoder 2.8.1. It requests camera/microphone permission and publishes to a user-provided RTMP/RTMPS URL.

## Important verification status

A successful CI build proves compilation and APK packaging only. It does not prove camera compatibility on every phone, a successful connection to a real RTMP server, database policy compatibility, or production readiness. These require installation and live device/backend tests.

The current RTMP screen is a direct publisher to an RTMP/RTMPS ingest URL. It does not yet provide a managed media server, automatic YouTube/Facebook account integration, multi-guest WebRTC rooms, or a full broadcast compositor.
