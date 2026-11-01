# SiLu Browser

A Chromium-based Android browser built on Android System WebView, written entirely in Kotlin.

## Features

- Native home screen with clock, shortcuts, wallpapers and a secret mode variant
- Native error and offline screens with automatic reload when the connection returns
- Secret tabs backed by an isolated WebView profile
- Ad and tracker blocking, per-site allow list, HTTPS-only mode
- Userscripts, developer tools, find in page, desktop site, per-site text zoom
- Bookmarks, history, downloads, search widget

## Build

```
gradle assembleRelease
```

Requires JDK 17 and the Android SDK. Set `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` to sign the release build.

## Source layout

| File | Responsibility |
| --- | --- |
| `MainActivity.kt` | Tabs, navigation, address bar, menus, downloads |
| `HomeScreen.kt` | Native home page |
| `ErrorScreen.kt` | Native error and offline pages |
| `Wallpapers.kt` | Gradient and photo wallpaper rendering |
| `Favicons.kt` | Cached favicon loading for shortcuts |
| `UserScripts.kt` | Userscript storage, metadata parsing and matching |
| `PageScripts.kt` | JavaScript injected into pages |
| `DevTools.kt` | In-app developer tools |
| `SettingsActivity.kt` | Settings screens |
| `AppSettings.kt`, `Store.kt`, `Blocker.kt` | Preferences, persisted data, blocklists |
