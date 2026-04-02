# User Guide

## Getting Started

Install the APK on your Android device (Android 7.0 or newer). The app appears as **btest** in your app drawer.

Launch the app — you'll see a single screen with a configuration form and a Start Test button.

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

### Protocol

- **TCP** — Reliable, ordered delivery. Best for measuring actual throughput. Recommended for most tests.
- **UDP** — Connectionless. Can achieve higher raw speeds but may show packet loss. Useful for stress-testing or measuring network capacity beyond TCP's overhead.

### Direction

- **Send** — Measures upload speed (device to server)
- **Receive** — Measures download speed (server to device)
- **Both** — Simultaneous upload and download (bidirectional). This is the default and the most common test mode.

### Duration

How long the test runs, from 10 to 120 seconds. Default is 30 seconds. Longer tests give more stable averages. Public servers limit tests to 120 seconds.

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

## Interpreting Results

### Speed readings

Speeds are shown in Mbps (megabits per second). Typical results depend on your connection:

- **Wi-Fi 5 (802.11ac)**: 200–400 Mbps
- **Wi-Fi 6 (802.11ax)**: 400–900 Mbps
- **LTE**: 30–100 Mbps
- **5G**: 100–1000+ Mbps

### CPU usage

- **Local CPU**: Your device's overall CPU usage during the test. High CPU (>70%) can bottleneck speeds.
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

## Public Server Limits

The public test servers enforce per-IP quotas:

- **Daily**: 2 GB
- **Weekly**: 8 GB
- **Monthly**: 24 GB
- **Max duration**: 120 seconds per test

View your usage and test history at [btest.home.kg/dashboard](https://btest.home.kg/dashboard).
