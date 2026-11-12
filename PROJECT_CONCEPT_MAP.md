# Java concept map

| Java concept | Actual class/package | Application feature | Why it is needed |
|---|---|---|---|
| Encapsulation | `domain/Zone`, `ScenarioConfig` | Readiness and hazard state | Validates and protects domain invariants. |
| Inheritance | `SimulationEvent` and event subclasses | Earthquake, aftershock, disruption and recovery | Shares event metadata while representing event-specific behavior. |
| Polymorphism | `SimulationEvent.applyTo` | Zone hazard, infrastructure, preparedness and recovery updates | Applies event effects without GUI-specific branching. |
| Abstraction | `Runnable`, `AutoCloseable` | Worker and service lifecycle | Separates execution roles from orchestration. |
| Functional interface | `SimulationEventHandler` | Simulation-worker event callbacks | Gives event processing a domain-specific callback contract. |
| Composition | `CityModel`, `EarthquakeEngine` | City simulation and operations | Combines zones, events, missions, inventory and workers. |
| Enums | Domain enums | Risk, state, resource and mission statuses | Constrains finite choices. |
| Generics | `EventBuffer<E>` | Bounded producer-consumer buffer | Reusable, type-safe blocking communication. |
| Collections | `ArrayList`, `HashSet`, `EnumMap` | Zones, event history, tasks and inventory | Efficiently represents changing domain collections. |
| Exception handling | Validation, JDBC and I/O boundaries | User feedback and persistence errors | Prevents invalid data and reports failures clearly. |
| Threads | `ScenarioEventFeed`, engine-managed workers | Live simulation | Runs independent work outside the EDT. |
| Monitors | `EventBuffer`, `ResourceAllocator` | Queue coordination and atomic reservations | Avoids polling and resource over-allocation. |
| Pipes | `ScenarioEventFeed`, `EventIntake` | Observation transport | Demonstrates producer/consumer stream communication. |
| Atomic/immutable data | `AtomicLong`, `SimulationSnapshot` | Event counts and view updates | Provides safe counters and consistent EDT handoff. |
| JDBC | `DatabaseManager` | Local SQLite history | Persists events, tasks, missions and inventory. |
| Transactions | `createMissionTransactional` | Mission dispatch | Keeps reservations and mission-resource rows consistent. |
| Serialization | `SessionManager` | Session restore | Reconstructs domain objects without restoring runtime threads. |
| Binary I/O | `ArchiveService` | Typed event archive | Stores compact event records for local review. |
| Buffered text I/O | `LogService`, `ReportExporter` | Logs and exports | Writes readable reports efficiently. |
| Swing/EDT | `MainFrame` | Desktop console | Keeps component updates thread-safe. |
| Java2D | `RiskMap`, `RiskChart` | Offline map and chart | Draws live visualizations without external map/chart services. |
| Maven/JUnit | `pom.xml`, unit tests | Build and verification | Standardizes dependency management and automated checks. |
