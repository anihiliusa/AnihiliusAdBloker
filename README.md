# AnihiliusAdBlocker

🛡️ Android Ad Blocker with VPN-based filtering and YouTube background playback.

## Features

- **VPN-based Ad Blocking** — Intercepts DNS queries and blocks ad/tracking domains
- **YouTube Background Play** — Keep YouTube audio playing when screen is off
- **Filter Management** — 100+ built-in filter rules, custom filters support
- **Auto-start on Boot** — Optional auto-start after device restart
- **Dark Theme** — Modern Material Design dark UI
- **Whitelist Support** — Exclude specific apps from VPN filtering
- **Filter Updates** — Online filter list updates with 100+ rules

## How It Works

1. **AdBlock VPN** creates a local VPN on your device
2. All DNS queries pass through the filter engine
3. Ad/tracking domains are blocked (NXDOMAIN response)
4. Legitimate traffic passes through normally

## APK Build Status

Automatic Android APK build trigger: 2026-05-29 09:00 UTC.

## Build

### Automatic (GitHub Actions)
Just push to `main` branch. APK will be built automatically.

### Manual
```bash
chmod +x gradlew
./gradlew assembleDebug
```

## Install

1. Download the APK from [Releases](../../releases)
2. Enable "Install from unknown sources" on your device
3. Install and grant VPN permission

## Permissions

- **VPN Service** — Required for ad blocking
- **Foreground Service** — Required for background operation
- **Boot Complete** — Optional auto-start on boot
- **Internet** — For filter updates

## Made with ❤️ by AnihiliusA
