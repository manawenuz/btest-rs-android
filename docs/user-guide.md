# User Guide

## Getting Started

Install the APK on your Android device (Android 7.0 or newer). The app appears as **btest** in your app drawer.

Launch the app — you'll see the **Test** screen with a configuration form and a Start Test button. Use the hamburger menu to switch between **Test** and **History** screens.

## Configuration

### Server

The address of a MikroTik-compatible bandwidth test server. Pre-filled with `104.225.217.60` (US public server).

Public servers available for testing:

| Server | Location | Dashboard |
|--------|----------|-----------|
| `104.225.217.60` | US | [btest.home.kg](https://btest.home.kg) |
| `188.245.59.196` | EU | [btest.mikata.ru](https://btest.mikata.ru) |

You can also use your own MikroTik router's IP address if it has the bandwidth-test server enabled.

### Credentials

- **User**: Authentication username (default: `btest`)
- **Password**: Authentication password (default: `btest`)

For public servers, use `btest` / `btest`. For your own router, use the credentials configured in MikroTik's bandwidth-test server settings.

### Saved Credentials

When **Save credentials** is checked (default), the server/user/password are remembered. Previously saved servers appear in a dropdown at the top of the form — tap to quick-select, or tap the X to delete a saved entry.

### Protocol

- **TCP** — Reliable, ordered delivery. Best for measuring actual throughput. Recommended for most tests.
- **UDP** — Connectionless. Can achieve higher raw speeds but may show packet loss. Useful for stress-testing or measuring network capacity beyond TCP's overhead.

### Direction

- **Send** — Measures upload speed (device to server)
- **Receive** — Measures download speed (server to device)
- **Both** — Simultaneous upload and download (bidirectional). This is the default and the most common test mode.

### Duration

How long the test runs. The slider uses non-linear steps for practical ranges:

**10s, 15s, 20s, 30s, 45s, 1m, 1m30s, 2m, 3m, 5m, 10m, 15m, 30m, 1h**

Default is 30 seconds. Longer tests give more stable averages. Public servers limit tests to 120 seconds.

## Running a Test

1. Configure the settings (or leave defaults for a quick test)
2. Tap **Start Test**
3. The button turns red and changes to **Stop** — tap it anytime to end early

During the test you'll see:

### Live Speed Display

Large TX (blue) and RX (green) numbers showing current speed in Mbps, updated every second.

### Speed Graph

A real-time line chart plotting TX and RX speeds over time. The Y-axis auto-scales to fit the maximum observed speed. Each data point has a dot marker.

- **Blue line** — TX (upload)
- **Green line** — RX (download)

### Statistics

| Stat | Description |
|------|-------------|
| Avg TX / Avg RX | Running average speed since test start |
| TX bytes / RX bytes | Total data transferred |
| CPU (local/remote) | Device CPU usage / server CPU usage |
| Lost packets | Packets lost during the test (UDP only, always 0 for TCP) |
| Elapsed | Seconds since test start |

### Final Results

After the test completes, a **Final Results** section appears with the server-calculated averages:

| Stat | Description |
|------|-------------|
| TX avg / RX avg | Server-reported average speeds |
| Total TX / Total RX | Total bytes transferred |
| Lost | Total lost packets |

Results are automatically saved to the local database. If auto-sync is enabled, they are also uploaded to your web dashboard.

## History

Open the navigation drawer (hamburger menu) and tap **History** to see all saved test runs.

### Viewing Results

Tap any run to see its full detail view, including the speed graph, all statistics, and final results. Press the back arrow to return to the list.

### Selecting and Exporting

- **Long-press** a run to enter selection mode
- Tap additional runs to add/remove from selection
- Use **All** to select everything

Action bar options:
- **Share** — Export selected runs as a CSV file
- **Delete** — Remove selected runs from local database

### Sync Status

Runs that have been uploaded to the web dashboard show a cloud icon (☁) next to the protocol/direction label.

## Web Dashboard

The [btest-rs-web](https://btest-rs-web.vercel.app) dashboard lets you view, compare, and export test results from any browser.

### Setup

1. Open a [btest-rs-web](https://btest-rs-web.vercel.app) instance in your browser
2. **Register** an account (email + password)
3. Copy your **API key** (starts with `btk_`)
4. In the Android app, go to **History** and expand the **Web Dashboard** card
5. The URL is pre-filled with `https://btest-rs-web.vercel.app` — change it if using your own instance
6. Paste your API key
7. Tap **Verify** to confirm the connection works
8. Enable **Auto-sync**

### How Sync Works

- When auto-sync is enabled, new test results are uploaded immediately after each test completes
- Each run is uploaded exactly once — the app tracks sync status in the local database
- Toggling auto-sync on will also upload any previously unsynced runs
- The sync happens in the background and doesn't block the UI
- A `device_id` is included in each upload so the dashboard can group results by device

### Self-Hosting

You can deploy your own btest-rs-web instance to Vercel:

1. **Fork** the [btest-rs-web](https://github.com/manawenuz/btest-rs-web) repository
2. **Create a database** at [neon.tech](https://neon.tech) (free tier is fine) and copy the connection string
3. **Import to Vercel** — set environment variables:
   - `DATABASE_URL` = your Neon connection string
   - `JWT_SECRET` = random secret (`openssl rand -hex 32`)
4. **Run migration**: visit `https://your-app.vercel.app/api/migrate`
5. **Register** and copy your API key

Then configure the Android app with your custom URL instead of the default.

## Interpreting Results

### Speed readings

Speeds are shown in Mbps (megabits per second). Typical results depend on your connection:

- **Wi-Fi 5 (802.11ac)**: 200–400 Mbps
- **Wi-Fi 6 (802.11ax)**: 400–900 Mbps
- **LTE**: 30–100 Mbps
- **5G**: 100–1000+ Mbps

### CPU usage

- **Local CPU**: Your device's CPU usage during the test. High CPU (>70%) can bottleneck speeds.
- **Remote CPU**: The server's CPU usage. If >80%, the server may be the bottleneck.

### Packet loss

- **TCP**: Always 0 (TCP retransmits lost packets)
- **UDP**: Some loss is normal, especially at high speeds. Loss >1% may indicate network congestion.

## Tips

- **Use TCP for reliable measurements.** UDP tests can show inflated speeds due to packet loss not being counted.
- **Run multiple tests** and compare averages for more reliable results.
- **Test at different times** to identify peak-hour congestion.
- **Check both directions separately** if you suspect asymmetric bandwidth (common on residential connections).
- **Close other apps** that use bandwidth during testing for accurate results.
- **Wi-Fi vs cellular**: Switch between Wi-Fi and mobile data to compare connection speeds.
- **Use the web dashboard** to compare results across devices and over time.

## Troubleshooting

### "Cannot run program... No such file or directory"

The app failed to find the btest binary. Try reinstalling the APK — the binary must be extracted to disk on install.

### Test starts but shows 0 Mbps

The connection to the server timed out. Check that:
- You have internet connectivity
- The server address is correct
- Your network allows outbound TCP connections on port 2000

### Connection refused

The bandwidth test server is not running or is blocking your IP. Try a different public server.

### Low speeds compared to other speed tests

btest measures raw TCP/UDP throughput to a specific server. Results depend on the route between your device and the test server. For local network testing, use your own MikroTik router as the server.

### Sync errors

- **"Enter API key first"** — paste your `btk_...` key in the Web Dashboard settings
- **"Failed: HTTP 401"** — your API key is invalid or expired; re-register or regenerate it on the dashboard
- **"Failed: ..."** — check the dashboard URL is correct and the instance is online

## Public Server Limits

The public test servers enforce per-IP quotas:

- **Daily**: 2 GB
- **Weekly**: 8 GB
- **Monthly**: 24 GB
- **Max duration**: 120 seconds per test

View your usage and test history at [btest.home.kg/dashboard](https://btest.home.kg/dashboard).
