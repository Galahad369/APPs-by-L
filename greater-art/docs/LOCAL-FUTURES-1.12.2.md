# Local-first feature architecture after 1.12.2

This is a design boundary, not a promise that every item is implemented.

## Related files from Nodes

Use the existing cached filename graph. For the current media ID, fetch its
strongest edges, exclude itself and exact duplicates, then diversify near-tied
results so filename variants do not fill the whole list. Initial score is the
existing edge weight. Future local-only boosts may use artist, album, folder,
playlist co-membership and Favourite status. Optional history may weakly re-rank
only when the user has explicitly enabled it. The UI name should be **Related**,
not an AI recommendation claim.

Recommended repository API:

```text
related(mediaId, limit, optionalSignals) -> List<RelatedCandidate>
```

Computation belongs on `Dispatchers.Default`; graph/cache invalidation follows
the existing media fingerprint. No new permission, network request or telemetry.

## Optional history portability

The local history toggle already defaults off. A future explicit export/import
should use a versioned JSON schema and Android Sharesheet. Cross-device matching
should prefer size + duration + normalized title/metadata, with a background
partial/content hash only when ambiguity remains. Never hash multi-gigabyte video
on the main thread or include raw paths by default.

## Voice search

Only on explicit microphone tap. Request microphone permission at that moment,
use Android on-device `SpeechRecognizer` when available, show a visible listening
state, destroy the recognizer after use, and never silently fall back to a cloud
recognizer. The recognized text enters the normal local search pipeline.

## Captions

Unify Media3 embedded text tracks with sidecar SRT/TTML while keeping LRC lyrics
as a separate concept. Provide Off / track-language selection and accessible
appearance controls. Local caption generation and translation are separate,
download-heavy optional modules; neither belongs in the base APK without a clear
size, device capability and privacy decision.

## Desktop and device handoff

A future desktop player should share portable playlist/history schemas, not the
Android UI layer. Cross-device handoff should be an explicit local-network or
file-transfer action with authenticated pairing. It must not turn ordinary local
playback into an account or cloud dependency.
