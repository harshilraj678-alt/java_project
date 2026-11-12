# EarthquakeReady — Disaster Preparedness Simulator

EarthquakeReady is an offline Java Swing learning application for exploring a fictional earthquake exercise, community preparedness activities, and limited-resource response missions. All zones, exposure values, hazard scores, and outcomes are invented for education; they are not earthquake predictions, engineering assessments, emergency dispatches, alerts, or real evacuation guidance. Follow local emergency-management authority guidance in real situations.

## Features

- Dark desktop console with Dashboard, Scenario Setup, interactive Java2D City Map, Preparedness, Simulation Control, Response Center, Analytics, History & Reports, Engineering Inspector, and Settings.
- Mild, moderate, severe, and custom exercises with bounded inputs and a reproducible random seed.
- Import/export of validated custom scenario configuration using UTF-8 `.properties` files.
- Six schematic city zones with transparent illustrative hazard scoring and readiness levels.
- Threaded producer/consumer pipeline using `ScenarioEventFeed`, piped character streams, `EventIntake`, a bounded wait/notify buffer, and simulation/response workers.
- Atomic resource reservations, concurrent mission progress, pause/resume/stop/single-step controls, feed-speed selection, and an optional priority hint experiment.
- Local SQLite history through JDBC and a dedicated asynchronous database executor. Database failure does not disable the core simulator or local session files.
- Versioned session save/restore, typed binary observation archive, operational text log, TXT and CSV exports.
- Live Engineering Inspector, conceptual architecture diagrams, and Java concept explanations.

## Technology stack

Java 17+, Swing/Java2D, Maven, SQLite JDBC (Xerial), JUnit 5, Java I/O, JDBC, Java serialization, and Java concurrency primitives. The project does not use Spring, JavaFX, a web server, or an external service.

## Prerequisites and run

Install a JDK 17+ and Maven 3.8+.

```powershell
mvn clean test
mvn package
java -jar target\earthquakeready-1.0.0.jar
```

The shaded executable JAR includes SQLite JDBC. The application is fully offline and requires no separate database service.

## Usage

1. Open **Scenario Setup**, choose a preset or adjust the educational scenario values and seed, then apply.
2. Visit **Preparedness** and select checklist activities to raise the chosen zone's readiness score.
3. Use **Simulation Control** to start the threaded event feed or advance deterministic individual steps. The live status card shows state, processed events, and tick progress; pause/resume, stop, and adjust feed speed there.
4. Inspect the schematic **City Map** and select a zone. Use **Response Center** to reserve resources for concurrent missions.
5. View **Analytics**, **History & Reports**, and **Engineering Inspector**.
6. Save a named session, load a previous save, or export TXT/CSV reports.

## Local data and database

By default, the application creates `%APPDATA%\EarthquakeReady\data\` on Windows (or `~/.earthquakeready/data/` elsewhere), containing `logs`, `archive`, `saves`, `reports`, and `database` subfolders. SQLite history resides in `database\earthquakeready.db`; saved sessions use `.ers` files. SQLite operations run on a single controlled background executor. The database is an additional history store, not a prerequisite for running or saving the core model.

## Testing

```powershell
mvn clean test
powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1
```

Tests cover scenario validation and risk boundaries, fixed-seed single-step determinism, producer/consumer blocking and closure, concurrent resource conservation, graceful engine shutdown, save/restore and corrupt saves, parameterized JDBC operations and transaction rollback, and report exports.

## Screenshots

No screenshots are committed yet. The app is designed for a live desktop demonstration.

## Limitations

This is a single-user teaching simulator. The city map is schematic and fictional. The risk formula and resource/mission outcomes are deliberately illustrative and are not calibrated for real hazards, buildings, or populations. It does not ingest sensors, contact emergency services, send warnings, calculate safe routes, or provide operational advice. Serialization is versioned and filtered for application-created local saves only.

## Project layout

```text
src/main/java/com/earthquakeready/
  app/       Swing desktop UI and Java2D views
  db/        SQLite manager, schema initialization, asynchronous service
  domain/    Scenario, zones, events, resources, missions and enums
  engine/    Feed, pipe intake, bounded queue, workers and city model
  io/        Local app paths, session persistence, logs, archives and reports
src/test/java/  JUnit 5 domain, engine, database and I/O tests
scripts/        Windows test and jpackage helpers
```
