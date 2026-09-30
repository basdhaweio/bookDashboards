# Library — Android app

A native shell around the published dashboard (https://basdhaweio.github.io/bookDashboards/)
plus a **home-screen widget**. Same pattern as weatherTerminal: the app loads the
live site (pushes reach it without an APK update), CI builds the APK.

## Install / update

Newest build, always at the same URL (open it on the phone, allow installs from
that source once):

https://github.com/basdhaweio/bookDashboards/releases/download/android-latest/library.apk

Debug builds are signed with the committed `app/debug.keystore` (the standard
well-known debug key, same file as weatherTerminal), so a newer APK installs over
an older one. Native changes (widget layout, permissions) need a new APK; dashboard
changes do not.

## What the shell does

- WebView on the live dashboard; external links (Tombolo, Goodreads…) open in the
  system browser; back navigates the page's history.
- Camera: the page's ISBN scanner asks for the camera through the WebView, which
  becomes Android's camera permission dialog (once).
- The inbox token you paste into Metadata › Set up lives in the app's own web
  storage — `allowBackup=false` keeps it out of cloud backups.
- Opens on a page URL when the widget sends one (`…/#scan`, `…/#add`, `…/#order`,
  `…/#order/<id>`, `…/#tbr`); if the app is already open, only the hash moves.

## The widget (`LibraryWidget` + `WidgetWorker`)

Fetches `data/widget.json` and one chart PNG (light or dark to match the system
theme) every 30 minutes or on demand, and draws: four buttons (Scan · Add · Order ·
Read), the book being read, the year line (read · pace · this time last year),
up to three open order lines (tap → that order's card), the chart (tap → next chart:
progress, per year, by month, bought/arriving) and a status line (tap → refresh).
Offline it keeps the last good data. Nothing is written from the widget — every
tap opens the app on the right page. See `../docs/WIDGET.md` for what jerry publishes.

## Building locally

```sh
cd android
./gradlew assembleDebug     # needs ANDROID_HOME or android/local.properties
# → app/build/outputs/apk/debug/app-debug.apk
```
