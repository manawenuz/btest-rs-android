# Architecture

## Overview

btest-android is a native Android wrapper around the [btest-rs](https://github.com/manawenuz/btest-rs) CLI binary. The app does **not** implement the MikroTik bandwidth test protocol — it bundles the pre-compiled Rust binary and executes it as a subprocess, parsing its stdout for real-time results. Test results are persisted to a local Room database and optionally synced to a [btest-rs-web](https://btest-rs-web.vercel.app) dashboard.

```mermaid
graph TB
    subgraph Android App
        UI[Jetpack Compose UI]
        TVM[TestViewModel]
        HVM[HistoryViewModel]
        Runner[BtestRunner]
        CPU[CpuMonitor]
        DB[(Room DB)]
        Sync[ResultPoster]
    end

    subgraph System
        PB[ProcessBuilder]
        Proc["/proc/stat"]
    end

    subgraph External
        Binary["libbtest.so<br/>(Rust binary)"]
        Server[MikroTik Server]
        Web["btest-rs-web<br/>Dashboard"]
    end

    UI -->|user input| TVM
    UI -->|history actions| HVM
    TVM -->|config| Runner
    TVM -->|start/stop| CPU
    TVM -->|save run| DB
    HVM -->|query/delete| DB
    HVM -->|sync| Sync
    Sync -->|POST /api/results| Web
    CPU -->|reads| Proc
    Runner -->|spawns| PB
    PB -->|exec| Binary
    Binary <-->|btest protocol| Server
    Binary -->|stdout| Runner
    Runner -->|Flow&lt;BtestOutput&gt;| TVM
    CPU -->|Flow&lt;Int&gt;| TVM
    TVM -->|StateFlow| UI
    HVM -->|StateFlow| UI
```

## Module Structure

```mermaid
graph LR
    subgraph Data Layer
        BC[BtestConfig.kt]
        BR[BtestResult.kt]
        Entity[TestRunEntity.kt<br/>TestIntervalEntity.kt]
        DAO[TestRunDao.kt]
        AppDB[AppDatabase.kt]
    end

    subgraph Process Layer
        BRun[BtestRunner.kt]
        CM[CpuMonitor.kt]
    end

    subgraph Network Layer
        JP[JsonExporter.kt]
        RP[ResultPoster.kt]
        RS[RendererSettings.kt]
    end

    subgraph Presentation Layer
        TVM[TestViewModel.kt]
        HVM[HistoryViewModel.kt]
        CS[CredentialStore.kt]
        CSV[CsvExporter.kt]
    end

    subgraph UI Layer
        MA[MainActivity.kt]
        TS[TestScreen.kt]
        HS[HistoryScreen.kt]
        RD[ResultDetailScreen.kt]
        SG[SpeedGraph.kt]
        TH[Theme.kt]
    end

    BC --> BRun
    BR --> BRun
    BRun --> TVM
    CM --> TVM
    Entity --> DAO
    DAO --> AppDB
    AppDB --> TVM
    AppDB --> HVM
    TVM --> TS
    HVM --> HS
    HVM --> RD
    TS --> SG
    MA --> TS
    MA --> HS
    MA --> TH
    JP --> RP
    RS --> HVM
    RP --> HVM
    CS --> TVM
    CSV --> HVM
```

| File | Responsibility |
|------|---------------|
| `BtestConfig.kt` | Data class for test parameters; builds CLI argument list |
| `BtestResult.kt` | Data classes for per-interval stats and test summary |
| `BtestRunner.kt` | Spawns binary via `ProcessBuilder`, parses stdout line-by-line, emits `Flow<BtestOutput>` |
| `CpuMonitor.kt` | Reads `/proc/<pid>/stat` every 1s, computes process CPU %, emits `Flow<Int>` |
| `TestViewModel.kt` | Holds `StateFlow`s for test UI state; coordinates runner, CPU monitor, and auto-sync |
| `HistoryViewModel.kt` | Manages history list, selection, detail view, CSV export, and web dashboard sync |
| `CredentialStore.kt` | Persists server credentials to SharedPreferences |
| `RendererSettings.kt` | Persists web dashboard URL, API key, and sync toggle |
| `JsonExporter.kt` | Converts runs/intervals to JSON for API submission (single and batch) |
| `ResultPoster.kt` | HTTP POST to btest-rs-web API (`/api/results` and `/api/results/batch`) |
| `CsvExporter.kt` | Exports selected runs as CSV for sharing |
| `TestRunEntity.kt` | Room entity for test runs (includes `synced` flag) |
| `TestIntervalEntity.kt` | Room entity for per-second interval data |
| `TestRunDao.kt` | Room DAO with queries for CRUD, sync status, and batch operations |
| `AppDatabase.kt` | Room database singleton with migrations |
| `MainActivity.kt` | Single-activity entry point with navigation drawer (Test / History) |
| `TestScreen.kt` | Config form, start/stop, live speed display, graph, statistics |
| `HistoryScreen.kt` | Run list with selection, action bar, web dashboard settings card |
| `ResultDetailScreen.kt` | Detailed view of a saved run with graph and full statistics |
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
    participant DB as Room DB
    participant Web as btest-rs-web

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
    VM->>DB: insertRunWithIntervals()
    VM->>Web: POST /api/results (if sync enabled)
    Web-->>VM: 201 Created
    VM->>DB: markSynced()
    VM-->>UI: summary StateFlow update
    VM->>CPU: cancel

    User->>UI: Views final results
```

## Persistence Layer

Test results are stored in a Room database (`btest_history.db`) with two tables:

```mermaid
erDiagram
    test_runs {
        long id PK
        long timestamp
        string server
        string protocol
        string direction
        int durationSec
        double txAvgMbps
        double rxAvgMbps
        long txBytes
        long rxBytes
        long lost
        boolean synced
    }

    test_intervals {
        long id PK
        long runId FK
        int intervalSec
        string direction
        double speedMbps
        long bytes
        int localCpu
        int remoteCpu
        long lost
    }

    test_runs ||--o{ test_intervals : "has"
```

The `synced` column tracks whether each run has been uploaded to the web dashboard, ensuring each run is only uploaded once.

## Web Dashboard Sync

```mermaid
sequenceDiagram
    participant App as Android App
    participant DB as Room DB
    participant API as btest-rs-web API

    Note over App: Test completes
    App->>DB: Insert run (synced=false)

    alt Auto-sync enabled
        App->>DB: Query unsynced runs
        DB-->>App: List of unsynced runs
        loop For each unsynced run
            App->>API: POST /api/results (JSON + device_id)
            API-->>App: 201 Created
            App->>DB: UPDATE synced=true
        end
    end

    Note over App: User enables sync toggle
    App->>DB: Query unsynced runs
    DB-->>App: All unsynced runs
    App->>API: POST each to /api/results
    App->>DB: Mark synced
```

- **Single runs**: `POST /api/results` with JSON body
- **Batch sync**: Each run POSTed individually to avoid partial failures
- **Deduplication**: The `synced` flag prevents re-uploading
- **Device ID**: `ANDROID_ID` is included in every payload for device-level grouping on the dashboard
- **Verify**: `GET /api/auth/me` validates the API key before enabling sync

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

The binary's built-in CPU sampler does not work reliably on Android (always reports 0% for local CPU). The app implements its own `CpuMonitor` that reads `/proc/<pid>/stat` for the btest process:

```mermaid
graph LR
    PS["/proc/pid/stat"] -->|read| CM[CpuMonitor]
    CM -->|"delta utime+stime"| CPU["CPU %"]
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
        SyncJob["Sync to dashboard"]
    end

    subgraph "Dispatchers.IO"
        Read["Read stdout"]
        Parse["Parse lines"]
        ProcStat["Read /proc/pid/stat"]
        HTTP["HTTP POST"]
        DBWrite["Room DB writes"]
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
    SyncJob -->|"withContext(IO)"| HTTP
    SyncJob -->|"suspend"| DBWrite
```

- **No JNI/NDK** — the binary runs as a separate OS process
- **No mutexes** — all state flows through Kotlin `StateFlow` (thread-safe)
- Process lifecycle tied to ViewModel: `stop()` calls `Process.destroyForcibly()`
- ViewModel's `onCleared()` ensures cleanup on activity destruction
- Network I/O (sync) runs on `Dispatchers.IO`, never blocks the main thread
