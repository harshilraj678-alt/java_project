# Java concepts in EarthquakeReady

| Concept | Where it is used | Practical purpose |
|---|---|---|
| Encapsulation and validation | `domain/Zone`, `ScenarioConfig`, `ResponseMission` | Keeps simulation values within meaningful bounds and controls state changes. |
| Inheritance and polymorphism | Abstract `SimulationEvent`, earthquake, aftershock, disruption, operator-action and recovery subclasses | Each event variant applies a meaningful domain effect through one shared contract. |
| Abstraction and interfaces | `SimulationEventHandler`, `Runnable`, `AutoCloseable` | Separates event handling, background tasks and lifecycle ownership from callers. |
| Composition | `CityModel`, `EarthquakeEngine`, `MainFrame` | Builds the city from zones/events/missions and wires engine, I/O, database, and view responsibilities. |
| Enums | `SeverityLevel`, `SimulationState`, `ResourceType`, `MissionStatus`, `EventType`, `ZoneStatus`, `PreparednessCategory` | Represents finite domain choices without magic strings. |
| Collections and generics | Zone/event/mission lists, enum maps, `EventBuffer<E>` | Stores model state and provides a type-safe reusable blocking queue. |
| Exceptions | Validation, I/O, SQL, interruption | Reports invalid requests or failed persistence instead of disguising errors. |
| Deterministic randomness | `ScenarioEventFeed`, `EarthquakeEngine.advanceOneStep` | Makes an exercise reproducible for comparisons and tests. |
| Threads and runnables | Feed, intake, simulation and response workers | Keeps event production, intake and response progress independent of Swing. |
| Monitors and synchronization | `EventBuffer`, `ResourceAllocator`, `CityModel` | Coordinates condition-based work and protects finite shared inventory. |
| Pipes | `PipedWriter` / `PipedReader` | Transports generated feed records to an independently running intake. |
| Concurrency utilities | Executor services, futures, latches, atomics | Provides bounded task execution, asynchronous persistence, counters and deterministic tests. |
| Swing EDT | `SwingUtilities.invokeLater`, Swing event listeners | Ensures UI state is only changed by the event-dispatch thread. |
| JDBC and transactions | `DatabaseManager`, `DatabaseService` | Stores local history and commits mission/resource changes atomically. |
| Character and binary I/O | `LogService`, `ReportExporter`, `ArchiveService` | Persists readable operational reports and typed event records. |
| Serialization and filtering | `SessionManager` | Saves a versioned model snapshot while excluding runtime-only resources. |
| Lambdas and streams | Snapshot mapping, callbacks, UI actions | Keeps transformations and event callbacks concise and localized. |
| Java2D | Map and risk chart inner components | Draws a fully offline schematic city and live score visualization. |
| Maven and JUnit | `pom.xml`, `src/test/java` | Reproducibly builds the executable and verifies core invariants. |

## Safe concurrency notes

The event buffer uses monitor `wait()`/`notifyAll()` in condition-checking loops. The resource allocator validates every requested resource before decrementing any of them. Lock order is simple: queue and allocator are not nested; code does not perform I/O or Swing work while holding either lock. An intentional deadlock is not part of the simulator.

Thread priority cannot guarantee scheduling order. Correct event ordering is provided by the queue and simulation tick, not by priority.

## Educational safety

Scenario scoring, city zones, vulnerability values, resource quantities, and mission progress are fictional. They are not forecasts, structural evaluations, evacuation routes, alerts, dispatch actions, or real-world emergency instructions. Follow local authorities during actual emergencies.
