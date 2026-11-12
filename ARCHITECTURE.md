# Architecture

## Overview

EarthquakeReady is a layered local desktop application. Domain objects and simulation workers do not depend on Swing. `MainFrame` renders immutable snapshots and forwards user commands to the engine. SQLite is an optional additional persistence channel; local sessions, logs, archives, and reports are file-based.

## Package responsibilities

- `domain`: validated `ScenarioConfig`, encapsulated `Zone`, polymorphic serializable `SimulationEvent` subtypes, mission and resource enums.
- `engine`: `CityModel`, deterministic `ScenarioEventFeed`, `EventIntake`, generic `EventBuffer<E>`, `SimulationWorker`, `ResponseWorker`, `ResourceAllocator`, and the engine lifecycle.
- `db`: `DatabaseManager` owns JDBC connections and parameterized SQL, `DatabaseInitializer` creates schema, and `DatabaseService` serializes operations on a single background executor.
- `io`: platform-specific data paths, versioned and filtered session files, asynchronous operational logs and typed binary archive, and TXT/CSV exporters.
- `app`: Swing navigation, dashboard, interactive map, controls, reporting and inspector. All model-to-UI callbacks use `SwingUtilities.invokeLater`.

## Event pipeline and lifecycle

```text
ScenarioEventFeed (Thread)
  -> PipedWriter / PipedReader
  -> EventIntake (Runnable)
  -> bounded EventBuffer<SimulationEvent>
  -> SimulationWorker (Runnable)
  -> CityModel
  -> immutable SimulationSnapshot
  -> Swing EDT

User mission -> atomic ResourceAllocator reservation
  -> response work buffer -> ResponseWorker -> progress/release -> snapshot
```

`put` and `take` wait in `while` loops and signal peers with `notifyAll`. Closing a buffer wakes waiters; already-accepted simulation events drain before the simulation worker exits. The feed and each worker are real `Thread` instances launched with `start()`. Pause and shutdown use an explicit monitor/interrupt lifecycle. `stop()` closes both buffers and uses bounded joins outside the engine monitor. No thread is forcibly stopped. Feed speed affects pacing only; Java thread priority remains an optional scheduling hint and never establishes event order. The SQLite service has one background worker and a bounded task queue; saturation is reported rather than blocking the EDT.

`advanceOneStep` provides a deterministic, pre-feed educational step. With the same seed, scenario, and tick, it chooses the same zone and severity. The event feed uses the same deterministic random sequence and transports observations through the pipe. Event subclasses implement `applyTo` for earthquake shaking, aftershocks, infrastructure/utility/access disruptions, operator preparedness actions, and recovery. Completing a response mission emits an illustrative recovery event.

## Scoring and safety

The illustrative zone hazard score is clamped to 0–100 from scenario intensity, fictional vulnerability input, exposure index, and a preparedness reduction:

`round(intensity × 0.62 + vulnerability × 0.28 + exposure × 0.10 − preparedness × 0.22)`.

Feed observations add a bounded seed-controlled variation to intensity. Risk buckets use fixed score bands: below 25 low, below 50 moderate, below 75 high, otherwise critical. These are teaching heuristics, not prediction or engineering calculations. The schematic map has no geographic coordinates.

## Synchronization and thread safety

`ResourceAllocator` owns one monitor and uses a validate-then-decrement critical section over all requested resource types. Inventory conservation is checked on release. No file/database calls, sleeps, or Swing updates occur while its monitor is held. The event queue has its own monitor; no nested allocator/queue locking policy is required.

The engine creates immutable `SimulationSnapshot` values. Swing listeners schedule updates on the EDT. JDBC, session, report, stop/join, archive, and log work is performed outside interactive callbacks where it could block; database/archive/log operations have dedicated background execution.

## Database flow

`DatabaseService` is a single-thread asynchronous boundary. It invokes `DatabaseManager`, which opens short-lived SQLite JDBC connections, enables foreign keys per connection, uses `PreparedStatement` for data values, and wraps multi-row mission creation/resource changes in transactions with rollback on failure. The schema contains scenarios, observations, zone history, preparedness tasks, response missions, resource inventory, mission resource allocations, and operational logs. If SQLite is unavailable, an error is exposed in the Engineering Inspector; core simulation and local files continue.

## Persistence and I/O

Sessions contain a versioned domain snapshot, not Swing components, threads, locks, open streams, or database connections. Saves are written to a temporary file then moved into place atomically where supported. Loading uses an `ObjectInputFilter` with size/depth limits and an application/JDK class allowlist; corrupt or unsupported saves produce explicit errors. User-authored custom scenarios use validated UTF-8 Java properties loaded through `FileReader` and exported with `FileWriter`. Observations are also appended as typed `DataOutputStream` records and can be scanned with buffered data input. Text logs and reports use UTF-8 buffered writers.

## Error handling and limitations

Invalid domain values fail at construction. Feed/intake, database, archive, session, and report failures are surfaced to the UI or caller; optional SQLite failure does not silently replace core behavior with false data. Database errors are available in the inspector. This remains a single-user local educational application, not an operational emergency-management tool.
