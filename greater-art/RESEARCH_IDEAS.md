# Greater Art — Offline Player Research Notes

Research date: 2026-09-04. These are ideas, not silently enabled features. Greater
Art's rules remain: local-first, no ads, no analytics, no accounts and no Internet
permission.

## Best Next Improvements

1. **ReplayGain support** — read gain tags and normalize perceived loudness without
   modifying the original file. This is the strongest quality-of-life upgrade for a
   mixed local library. It must respect peak tags to avoid clipping.
2. **Folder include/exclude controls** — keep recursive `Download` discovery as the
   default, but let users hide ringtone/export/cache folders without deleting files.
3. **Duplicate finder** — report likely duplicates by size, duration and metadata.
   Privacy-safe and useful, but deletion must remain an Android-confirmed action.
4. **System equalizer handoff** — open Android's installed equalizer for the current
   audio session. This avoids bundling a risky DSP engine or changing the source audio.
5. **Settings/playlist backup and restore** — local JSON plus M3U8, explicitly chosen
   by the user. No cloud and no playback-history export.
6. **Home-screen widget** — play/pause/next and current artwork. Useful, but it adds a
   separate lifecycle surface and therefore needs device testing before release.
7. **Android Auto** — safe queue and voice browsing in the car. High value, but larger
   scope and stricter testing than a patch release.
8. **A–B repeat** — repeat a selected section for music practice or language learning.
   Small, offline and compatible with the existing repeat cycle if presented separately.

## Deliberately Rejected for Greater Art

- Listening history, play-count analytics and “most played” screens: local or not,
  they conflict with the user's privacy-first direction.
- Online lyrics, artwork lookup, streaming, accounts and scrobbling: they would require
  network access and weaken the manifest-level offline guarantee.
- Crossfade by default: it changes the original music and conflicts with the preference
  for gapless, unmodified playback. If ever added, it should be opt-in and off by default.
- Huge built-in DSP/effect racks: impressive on paper, but they increase decoder risk,
  app size and opportunities to damage clean playback.

## Sources Reviewed

- [Fossify Music Player](https://github.com/FossifyOrg/Music-Player) — offline playback,
  customizable widgets, notification/headset controls and metadata editing.
- [PixelPlayerOSS](https://github.com/PixelPlayerHQ/PixelPlayerOSS) — Media3/Compose,
  FFmpeg support, local lyrics, backup/restore and album/artist organization.
- [Tune](https://github.com/v8065791/Tune) — ReplayGain, duplicate detection, folder
  filters, safe deletion and portable local data.
- [Retro Music Player](https://github.com/RetroMusicPlayer/RetroMusicPlayer) — Android
  Auto, widgets, queue editing, gapless playback and folder browsing.
- [Aetherfin](https://github.com/Aetherfin/mobile-app) — A–B repeat, gapless queues,
  Bluetooth disconnect handling and output-driven visualization.

Only behavior and product ideas were compared. No third-party GPL source code was
copied into Greater Art.
