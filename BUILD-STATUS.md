# S Android rebuild — verified status

## Project identity preserved
- Repository: `chaouisakhr1-byte/S`
- Branch: `main`
- Application ID: `com.sstar.video`
- Backend: existing Supabase project configured in source.
- No new repository, package identity, or replacement app was created.

## Implemented in source
- Supabase email/password sign-up and sign-in, including the email-confirmation case.
- Persisted access and refresh tokens; refresh token renewal after HTTP 401.
- Public video feed and local video picker.
- Upload to public `s-media` Storage bucket (50 MiB configured limit), with client-side size and MIME checks.
- Public video playback.
- Load and post comments.
- Video likes and creator follows, with existing state restored in the feed.
- Creator names and a personal uploads list in the profile.
- Load and save profile data.
- Live Hub discovery for live sessions and host-owned scheduled sessions, including resume-studio action.
- Resume a host's scheduled live studio.
- RTMP/RTMPS publishing using RootEncoder 2.8.1.
- Runtime camera/microphone permission requests.
- Live chat send/read with periodic REST polling.
- Optional externally supplied HTTP(S) playback URL for a live stream.

## Backend checks
- Public tables inspected; RLS is enabled on profiles, videos, follows, likes, comments, live_streams and live_chat_messages.
- Security advisor returned no lint findings at the last check.
- Storage bucket `s-media` is public, configured for selected video MIME types, with a 50 MiB size limit.
- Storage INSERT and DELETE policies scope objects to the authenticated user's UID folder.
- The app uses a publishable key only. No service-role key is shipped in the Android client.

## Current app version
- Version name: `0.3.0`
- Version code: `3`

## Latest successful CI build before the latest code updates
- Workflow run: `42315862778` (run #36).
- Commit: `59ec98b93f1fa4a3b5e2a26cb6b1b48417d23433`.
- APK build and apksigner verification: passed.
- Debug AAB build and jarsigner verification: passed.
- Complete source ZIP packaging and artifact upload: passed.

Subsequent source changes update Live Hub scheduled-session handling and documentation. A fresh CI build is required to verify the latest HEAD.

## Not complete / not verified
- Physical-device runtime tests.
- Real Supabase account end-to-end tests.
- RTMP ingest and viewer playback against a real media server.
- Multi-guest WebRTC rooms.
- Managed streaming, transcoding, recording/replay and adaptive playback.
- Native Realtime chat (current chat polls REST).
- Push notifications and moderation/reporting.
- YouTube/Facebook/X account linking and platform publishing.
- Production signing keystore and release AAB.

Debug-signed APK/AAB artifacts are for testing only and are not Play Store release artifacts.
