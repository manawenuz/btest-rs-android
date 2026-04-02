# btest-android

MikroTik Bandwidth Test (btest) client for Android — native Material3 UI wrapping the [btest-rs](https://github.com/manawenuz/btest-rs) CLI binary, with cloud sync to the [btest-rs-web](https://github.com/manawenuz/btest-rs-web) dashboard.

## Screenshots

| Config | Running | Results |
|--------|---------|---------|
| ![config](docs/screenshots/config.png) | ![running](docs/screenshots/running.png) | ![results](docs/screenshots/results.png) |

## Features

- TCP and UDP bandwidth testing against MikroTik-compatible servers
- Bidirectional testing (send, receive, or both simultaneously)
- Real-time speed graph with TX/RX overlay
- Live CPU monitoring (local via `/proc/stat`, remote from server)
- Configurable duration (10s to 1 hour, non-linear slider)
- Test history with Room database persistence
- Cloud sync to [btest-rs-web](https://btest-rs-web.vercel.app) dashboard (auto-sync with dedup)
- CSV export and share for selected test runs
- Saved server credentials with quick-select dropdown
- Material3 dark theme with dynamic colors (Android 12+)
- Navigation drawer with Test and History screens

## Ecosystem

btest-android is part of the **btest-rs** ecosystem:

| Project | Description | Link |
|---------|-------------|------|
| [btest-rs](https://github.com/manawenuz/btest-rs) | Rust CLI client for MikroTik bandwidth test | GitHub |
| **btest-rs-android** | Android client (this repo) | [GitHub](https://github.com/manawenuz/btest-rs-android) |
| [btest-rs-web](https://github.com/manawenuz/btest-rs-web) | Web dashboard for viewing and comparing results | [Live](https://btest-rs-web.vercel.app) |

## Public Test Servers

| Location | Address | Dashboard |
|----------|---------|-----------|
| US | `104.225.217.60` | [btest.home.kg](https://btest.home.kg) |
| EU | `188.245.59.196` | [btest.mikata.ru](https://btest.mikata.ru) |

Default credentials: `btest` / `btest`

Limits: 2 GB daily, 120s max duration. Results viewable at `https://btest.home.kg/dashboard/YOUR_IP`.

## Web Dashboard

Test results can be synced to a [btest-rs-web](https://btest-rs-web.vercel.app) instance for viewing, comparing, and exporting from any browser.

**Quick setup:**

1. Register at [btest-rs-web.vercel.app](https://btest-rs-web.vercel.app)
2. Copy your API key (starts with `btk_`)
3. In the app, go to **History > Web Dashboard**, paste your API key
4. Tap **Verify** to confirm connectivity
5. Enable **Auto-sync** — all future test results are uploaded automatically

Each run is synced exactly once (tracked in local DB). A cloud icon appears on synced runs in the history list.

To deploy your own instance, see [btest-rs-web deployment](#deploying-btest-rs-web).

## Requirements

- Android 7.0+ (API 24)
- `INTERNET` permission (no runtime permission needed)

## Install

Download the latest APK from [Releases](../../releases) and install on your device.

Or build from source:

```bash
# Clone
git clone https://github.com/manawenuz/btest-rs-android.git
cd btest-rs-android

# Download pre-built btest binaries
curl -L https://github.com/manawenuz/btest-rs/releases/latest/download/btest-android-aarch64.tar.gz | tar xz
mv btest app/src/main/jniLibs/arm64-v8a/libbtest.so

curl -L https://github.com/manawenuz/btest-rs/releases/latest/download/btest-android-armv7.tar.gz | tar xz
mv btest app/src/main/jniLibs/armeabi-v7a/libbtest.so

# Build
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

See [User Guide](docs/user-guide.md) for detailed instructions.

**Quick start:** Launch the app, leave defaults, tap **Start Test**. You'll see real-time TX/RX speeds, a live graph, and statistics. Results are saved to history automatically.

## Architecture

See [docs/architecture.md](docs/architecture.md) for the full architecture overview.

The app wraps the `btest-rs` binary (pre-compiled for ARM64 and ARMv7) and runs it as a subprocess via `ProcessBuilder`. The binary handles all protocol logic — the app provides the UI, persists results to Room DB, and syncs to the web dashboard.

## Deploying btest-rs-web

You can deploy your own dashboard instance to Vercel in 5 steps:

1. **Fork** the [btest-rs-web](https://github.com/manawenuz/btest-rs-web) repository
2. **Create a Neon Postgres database** at [neon.tech](https://neon.tech) and copy the connection string
3. **Deploy to Vercel** — import the fork and set two environment variables:
   - `DATABASE_URL` = your Neon connection string
   - `JWT_SECRET` = a random secret (`openssl rand -hex 32`)
4. **Run the migration** by visiting `https://your-app.vercel.app/api/migrate`
5. **Register** on your instance, copy your API key (`btk_...`), and configure it in the Android app

Optional environment variables:

| Variable | Description |
|----------|-------------|
| `MIGRATE_SECRET` | Protects the `/api/migrate` endpoint |
| `NEXT_PUBLIC_APP_URL` | Your app's public URL |

The public instance is available at [btest-rs-web.vercel.app](https://btest-rs-web.vercel.app).

## License

MIT
