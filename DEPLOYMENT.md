# Windows deployment

## Build and test

Install JDK 17+ and Maven 3.8+, then run from the project directory:

```powershell
mvn clean test
mvn package
java -jar target\earthquakeready-1.0.0.jar
```

The Maven Shade Plugin creates the executable JAR with SQLite JDBC included. A separate SQLite installation or server is not required.

## Portable application image

The `scripts\package-windows.ps1` helper packages the application with `jpackage`. Run it from a Windows machine with a supported JDK that includes `jpackage`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

The script builds/tests before creating `dist\EarthquakeReady` as a Windows app image. `jpackage` uses the JDK's `jlink` support to include a runtime; the receiving machine does not need Java or Maven. App images are platform-specific, so create the Windows image on Windows. The local user data is created under `%APPDATA%\EarthquakeReady` at first launch and is not embedded in the image.

For code signing, MSI installers, or organization-managed distribution, apply the institution's signing and endpoint deployment policies after generating the image. No network service is involved at runtime.
