# Architecture

## Overview

btest-android is a thin native Android wrapper around the [btest-rs](https://github.com/manawenuz/btest-rs) CLI binary. The app does **not** implement the MikroTik bandwidth test protocol — it bundles the pre-compiled Rust binary and executes it as a subprocess, parsing its stdout for real-time results.

```mermaid
graph TB
    subgraph Android App
        UI[Jetpack Compose UI]
        VM[TestViewModel]
        Runner[BtestRunner]
        CPU[CpuMonitor]
    end

    subgraph System
        PB[ProcessBuilder]
        Proc["/proc/stat"]
    end

    subgraph External
        Binary["libbtest.so<br/>(Rust binary)"]
        Server[MikroTik Server]
    end

    UI -->|user input| VM
    VM -->|config| Runner
    VM -->|start/stop| CPU
    CPU -->|reads| Proc
    Runner -->|spawns| PB
    PB -->|exec| Binary
    Binary <-->|btest protocol| Server
    Binary -->|stdout| Runner
    Runner -->|Flow&lt;BtestOutput&gt;| VM
    CPU -->|Flow&lt;Int&gt;| VM
    VM -->|StateFlow| UI
```

## Module Structure

```mermaid
graph LR
    subgraph Data Layer
        BC[BtestConfig.kt]
        BR[BtestResult.kt]
    end

    subgraph Process Layer
        BRun[BtestRunner.kt]
        CM[CpuMonitor.kt]
    end

    subgraph Presentation Layer
        TVM[TestViewModel.kt]
    end

    subgraph UI Layer
        MA[MainActivity.kt]
        TS[TestScreen.kt]
        SG[SpeedGraph.kt]
        TH[Theme.kt]
    end

    BC --> BRun
    BR --> BRun
    BRun --> TVM
    CM --> TVM
    TVM --> TS
    TS --> SG
    MA --> TS
    MA --> TH
```

| File | Responsibility |
|------|---------------|
| `BtestConfig.kt` | Data class for test parameters; builds CLI argument list |
| `BtestResult.kt` | Data classes for per-interval stats and test summary |
| `BtestRunner.kt` | Spawns binary via `ProcessBuilder`, parses stdout line-by-line, emits `Flow<BtestOutput>` |
| `CpuMonitor.kt` | Reads `/proc/stat` every 1s, computes system CPU %, emits `Flow<Int>` |
| `TestViewModel.kt` | Holds `StateFlow`s for UI state; coordinates runner and CPU monitor lifecycles |
| `MainActivity.kt` | Single-activity entry point, sets up Compose with Material3 theme |
| `TestScreen.kt` | Main UI: config form, start/stop button, live speed display, graph, statistics |
| `SpeedGraph.kt` | Canvas-based real-time line chart (TX blue, RX green) |
| `Theme.kt` | Material3 dark color scheme with dynamic colors on Android 12+ |

## Data Flow

```mermaid
sequenceDiagram
    participant User
    participant UI as TestScreen
    participant VM as TestViewModel
    participant Runner as BtestRunner
    participant Binary as libbtest.so
    participant Server as MikroTik Server
    participant CPU as CpuMonitor

    User->>UI: Tap "Start Test"
    UI->>VM: start(context)
    VM->>CPU: monitor()
    VM->>Runner: run(config)
    Runner->>Binary: ProcessBuilder.start()
    Binary->>Server: TCP connect + auth
    Server-->>Binary: HELLO + auth OK

    loop Every 1 second
        Binary->>Server: bandwidth data
        Server->>Binary: bandwidth data
        Binary-->>Runner: stdout: "[  5] TX 285.47 Mbps ..."
        Runner-->>VM: BtestOutput.Interval(result)
        VM-->>UI: intervals StateFlow update
        CPU-->>VM: localCpu StateFlow update
        UI->>UI: Recompose graph + stats
    end

    Binary-->>Runner: stdout: "TEST_END ..."
    Runner-->>VM: BtestOutput.Summary(summary)
    VM-->>UI: summary StateFlow update
    VM->>CPU: cancel

    User->>UI: Views final results
```

## Binary Integration

The `btest-rs` binary is bundled as a native library to leverage Android's built-in extraction:

```mermaid
graph TD
    subgraph APK
        JNI["jniLibs/arm64-v8a/libbtest.so<br/>jniLibs/armeabi-v7a/libbtest.so"]
    end

    subgraph "Install (Android OS)"
        Extract["Extracts to<br/>nativeLibraryDir"]
    end

    subgraph Runtime
        Path["context.applicationInfo<br/>.nativeLibraryDir<br/>+ '/libbtest.so'"]
        PB2[ProcessBuilder]
    end

    JNI -->|"APK install"| Extract
    Extract -->|"file path"| Path
    Path -->|"exec()"| PB2
```

Key details:

- Named `libbtest.so` (not `btest`) because Android only extracts `lib*.so` files from `jniLibs/`
- `extractNativeLibs="true"` and `useLegacyPackaging=true` ensure the binary is extracted to disk (required for `exec()`)
- The binary is a statically-linked PIE executable with Android's `/system/bin/linker64` as interpreter
- Dependencies: only `libc.so`, `libm.so`, `libdl.so` (all present on every Android device)

## Output Parsing

BtestRunner parses two output formats from stdout:

### Interval lines

```
[   5]  TX  285.47 Mbps (35684352 bytes)  cpu: 20%/62%
[   5]  RX  283.64 Mbps (35454988 bytes)  cpu: 20%/62%  lost: 12
```

Regex: `\[\s*(\d+)]\s+(TX|RX)\s+([\d.]+)\s+(Gbps|Mbps|Kbps|bps)\s+\((\d+) bytes\)(?:\s+cpu:\s+(\d+)%/(\d+)%)?(?:\s+lost:\s+(\d+))?`

### Summary line

```
TEST_END peer=... proto=TCP dir=both duration=60s tx_avg=284.94Mbps rx_avg=272.83Mbps tx_bytes=2137030656 rx_bytes=2046260728 lost=0
```

### CPU Monitoring

The binary's built-in CPU sampler does not work reliably on Android (always reports 0% for local CPU). The app implements its own `CpuMonitor` that reads `/proc/stat` directly:

```mermaid
graph LR
    PS["/proc/stat"] -->|read| CM[CpuMonitor]
    CM -->|"delta busy / delta total"| CPU["CPU %"]
    CPU -->|StateFlow| UI[TestScreen]
```

## Threading Model

```mermaid
graph TD
    subgraph "Main Thread"
        Compose[Compose UI]
    end

    subgraph "viewModelScope"
        Collect["Collect BtestOutput"]
        CpuCollect["Collect CPU %"]
    end

    subgraph "Dispatchers.IO"
        Read["Read stdout"]
        Parse["Parse lines"]
        ProcStat["Read /proc/stat"]
    end

    subgraph "OS Process"
        Btest["libbtest.so"]
    end

    Compose -->|"recompose on StateFlow"| Compose
    Collect -->|"flowOn(IO)"| Read
    Read --> Parse
    Parse -->|emit| Collect
    CpuCollect -->|"flowOn(IO)"| ProcStat
    ProcStat -->|emit| CpuCollect
    Read -->|"readLine()"| Btest
```

- **No JNI/NDK** — the binary runs as a separate OS process
- **No mutexes** — all state flows through Kotlin `StateFlow` (thread-safe)
- Process lifecycle tied to ViewModel: `stop()` calls `Process.destroyForcibly()`
- ViewModel's `onCleared()` ensures cleanup on activity destruction
