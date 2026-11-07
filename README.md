# S ☆ — Native Android application

## Project identity
- Repository: `chaouisakhr1-byte/S`
- Android package / application ID: `com.sstar.video`
- Supabase project: existing project configured in `src/com/sstar/video/Supabase.java`
- Development continues in this repository and on `main`; no replacement project is used.

## Implemented in the current source
- Email/password Supabase sign-up and sign-in, including the email-confirmation response.
- Persisted access/refresh tokens and refresh-token renewal after an expired access token.
- Public video feed and local video selection.
- Upload to the existing public `s-media` bucket (currently limited to 50 MiB and its configured video MIME types).
- Public video playback.
- Read and post comments on public videos.
- Video likes and creator follows.
- Profile loading and saving.
- Live Hub listing of public live sessions and the signed-in host's scheduled sessions.
- Resume a host's scheduled studio session.
- RTMP/RTMPS camera publishing through RootEncoder 2.8.1, with camera/microphone runtime permission handling.
- Live chat with periodic REST polling and message posting.
- Optional HTTP(S) HLS playback URL for viewers when the broadcaster has a compatible playback endpoint.

## Backend and access controls checked
- Public schema tables have Row Level Security enabled.
- Supabase security advisor returned no security lint findings during the latest check.
- Storage bucket `s-media` is public, accepts configured video MIME types, and has a 50 MiB upload limit.
- Storage upload/delete policies restrict each authenticated user to their own UID folder.
- This Android client uses the publishable key only; never add a service-role key to the app.

## Build and artifacts
GitHub Actions runs:
- `:app:assembleDebug` and APK signature verification.
- `:app:bundleDebug` and AAB signature verification.
- A complete source ZIP containing the project files, APK, debug AAB, Git bundle and SHA-256 manifest.

The APK/AAB from this workflow are **debug-signed test artifacts**, not Play Store release artifacts.

## Remaining work / not yet verified
- Real installation and interaction tests on physical Android devices.
- Real backend end-to-end tests with a test account (email confirmation, profile, upload, likes, follows, comments).
- RTMP ingest and viewer playback against an actual media server; the app currently accepts an externally supplied RTMP publish URL and optional HLS playback URL.
- Multi-guest WebRTC rooms.
- Managed media server, adaptive transcoding, recording and replay.
- Native Supabase Realtime subscriptions (chat currently polls REST).
- Push notifications and moderation/reporting tools.
- YouTube/Facebook/X OAuth account linking and compliant platform publishing integrations.
- Production release keystore and Play Store AAB.

A successful GitHub Actions run confirms compilation, packaging, signature checks and artifact creation. It does not prove the runtime features above.
