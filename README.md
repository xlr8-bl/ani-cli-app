<div align="center">

# ⚡ XLR8

**An open-source Android anime streaming + downloading app.**
Polished native UI over the same scraping approach as [ani-cli](https://github.com/pystardust/ani-cli) and [Aniyomi](https://github.com/aniyomiorg/aniyomi).

No accounts · No telemetry · No app store — just sideload the APK.

</div>

---

> ⚠️ **Disclaimer.** XLR8 is a personal / educational project built for a small
> friend community. It does **not** host any content; it streams from third-party
> sources (the same public endpoints ani-cli uses). You are responsible for how you
> use it and for complying with the laws in your jurisdiction. No warranty of any kind.

## What is XLR8?

XLR8 gives you a premium, content-first anime experience on Android:

- **Rich discovery** — Trending, Popular This Season, Upcoming, Top 100, and a
  "Just Aired" feed with **NEW** badges.
- **Detailed anime pages** — synopsis, genres, score, studio, season selector,
  episode grid with real thumbnails, voice actors, and related shows.
- **Great player** — Media3/ExoPlayer with HLS + mp4, quality switching, gesture
  controls, PiP, resume, and auto-next.
- **Offline downloads** — pick a quality and watch without a connection.
- **Themes** — System / Light / Dark, Material You dynamic color, and an AMOLED black variant.
- **The device is the account** — history, watchlist, downloads and settings all live
  on-device. No sign-up, no cloud, no trackers.

## Data sources

| Source | Used for |
| ------ | -------- |
| [**AniList GraphQL**](https://graphql.anilist.co) | Metadata & discovery: titles, synopses, scores, genres, airing schedule, episode thumbnails, voice actors, relations |
| **AllAnime** | The actual video streams, resolved with logic ported from ani-cli |

XLR8 searches AniList for metadata, then resolves the same title on AllAnime to get
playable sources, caching the `AniList ID ↔ AllAnime ID` mapping locally.

## Project status

This repo is being built in the order below. **Steps 1–2 are in place**: project
scaffold, Material 3 theming + XLR8 branding, navigation, the AniList client, and a
Home screen wired to live discovery feeds. Remaining steps are stubbed and clearly marked.

1. ✅ Scaffold, theming, navigation, Material 3 + branding
2. ✅ AniList client + Home discovery feeds
3. ✅ Detail page (metadata, VAs, relations, episode grid with thumbnails, season selector)
4. ✅ AllAnime scraper port + title matching + source resolution (Room-cached mapping)
5. ✅ Media3 player + resume + quality switch + auto-next + gestures/PiP + Continue Watching
6. ⬜ Downloads + offline playback
7. ⬜ Search, watchlist, settings, privacy
8. ⬜ Easter eggs
9. ⬜ GitHub Actions release pipeline + README
10. ⬜ Polish: transitions, empty/error states, skeletons

## Tech stack

Kotlin · Jetpack Compose + Material 3 · Media3/ExoPlayer · Ktor + OkHttp ·
kotlinx.serialization · Coroutines/Flow · Room · Coil · WorkManager · DataStore.
Min SDK 24, target SDK 35. MVVM + repository layer, single-module to start.

## Install (from a Release)

1. Go to the [**Releases**](../../releases) page and download the latest `XLR8-*.apk`
   (use the `arm64-v8a` build for most modern phones, or the `universal` build if unsure).
2. On your phone, enable **Install unknown apps** for your browser/file manager
   (Settings → Apps → Special access → Install unknown apps).
3. Open the downloaded APK and tap **Install**.

## Build from source

Requires JDK 17+ and the Android SDK (API 35).

```bash
git clone https://github.com/xlr8-bl/ani-cli-app.git XLR8
cd XLR8
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/
# or a release build (see signing below)
./gradlew assembleRelease
```

Open the project in Android Studio (Ladybug or newer) and hit **Run** to deploy to a
device/emulator.

### Release signing

Release builds are signed from a keystore that is **never** committed. Provide it
either via a gitignored `keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=********
keyAlias=xlr8
keyPassword=********
```

…or via the `XLR8_KEYSTORE_FILE`, `XLR8_KEYSTORE_PASSWORD`, `XLR8_KEY_ALIAS`,
`XLR8_KEY_PASSWORD` environment variables (used by CI — see below). If no keystore is
present, release builds fall back to the debug signing key so the project still builds.

## Releasing (GitHub Actions)

Pushing a tag matching `v*` triggers `.github/workflows/release.yml`, which builds a
signed APK (per-ABI splits + a universal APK), generates a changelog, and attaches the
APKs to a GitHub Release. The keystore is decoded from repo secrets at build time.

```bash
git tag v0.1.0
git push origin v0.1.0
```

Required repository secrets: `XLR8_KEYSTORE_BASE64`, `XLR8_KEYSTORE_PASSWORD`,
`XLR8_KEY_ALIAS`, `XLR8_KEY_PASSWORD`.

## Privacy

XLR8 has **no accounts, no analytics, no crash-reporting, no ad SDKs, and no trackers**
of any kind. The only network calls are to AniList, AllAnime, and the image/stream CDNs
they reference. Everything else — history, watchlist, downloads, settings — stays on your
device. Uninstalling wipes it all, because the device was only ever the account. An
optional local JSON export/import is the only "sync".

## License

GPL-3.0 — see [LICENSE](LICENSE). XLR8 reuses scraping logic from ani-cli, which is
GPL-3.0, so XLR8 is GPL-3.0 too.
