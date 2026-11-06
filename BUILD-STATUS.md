# S Android rebuild status

## Identity and architecture
- Continued in the same repository and app identity: `com.sstar.video`.
- Java Android sources remain in `src/com/sstar/video`.
- Migrated the build to Android Gradle Plugin in the existing repository; no second app/repository was created.
- Added RootEncoder 2.8.1 dependency for native RTMP camera publishing.

## Implemented in source
- Supabase auth/session, public video feed, upload/playback and comments.
- Video likes and creator follow actions.
- Basic profile editor.
- Live Studio screen with camera/microphone permission handling and RTMP/RTMPS publisher controls.

## Not yet complete / not yet verified
- Multi-guest video rooms / WebRTC.
- Managed RTMP ingest and playback infrastructure.
- Fully real-time chat and notifications.
- External platform account integrations (YouTube/Facebook/X).
- Device-level tests of camera, microphone, network streaming and backend RLS/storage policies.
- Production signing and release AAB.

## Build
GitHub Actions uses `gradle :app:assembleDebug`. A green workflow is a compile/package check, not proof of all runtime features.
