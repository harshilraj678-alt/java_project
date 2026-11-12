package com.earthquakeready.io;

import com.earthquakeready.engine.EarthquakeEngine;
import com.earthquakeready.engine.SimulationSnapshot;
import com.earthquakeready.domain.MissionStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ReportExporter {
    private ReportExporter() {}
    public static Path exportTxt(Path folder, EarthquakeEngine engine) throws IOException {
        Path file = folder.resolve("earthquakeready-report.txt");
        SimulationSnapshot snapshot = engine.snapshot();
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("EARTHQUAKEREADY — EDUCATIONAL SIMULATION REPORT\n");
            out.write("Fictional data only; not a prediction or engineering assessment.\n\n");
            out.write("Scenario: " + engine.config().name() + "\nIntensity: " + engine.config().intensity()
                    + "\nSeed: " + engine.config().seed() + "\nConfigured duration: " + engine.config().durationTicks()
                    + " ticks\nCompleted duration: " + snapshot.tick() + " ticks\nAftershocks configured: "
                    + engine.config().aftershocks() + "\nPreparedness before exercise: "
                    + engine.config().preparedness() + "%\nPreparedness after activities: "
                    + snapshot.preparedness() + "%\n\n");
            out.write("ZONE RESULTS\n");
            for (var zone : snapshot.zones()) out.write(zone.name() + " | simulated risk " + zone.hazard()
                    + " | readiness before " + engine.config().preparedness() + "% / after "
                    + zone.preparedness() + "% | infrastructure " + zone.infrastructure() + "\n");
            out.write("\nEVENT HISTORY\n");
            for (var event : engine.city().events()) out.write("T+" + event.tick() + " | " + event.type() + " | "
                    + event.zoneId() + " | severity " + event.severity() + " | " + event.description() + "\n");
            out.write("\nMISSIONS\n");
            for (var mission : snapshot.missions()) out.write(mission.type() + " | " + mission.zone()
                    + " | " + mission.status() + " | " + mission.progress() + "% | resources "
                    + mission.allocation() + "\n");
            out.write("\nPREPAREDNESS ACTIVITIES\n");
            for (String task : engine.city().completedTasks()) out.write(task + "\n");
            out.write("\nRESOURCE UTILIZATION\n");
            var totals = engine.resources().totalInventory();
            for (var entry : snapshot.resources().entrySet()) out.write(entry.getKey().label() + " | used "
                    + (totals.get(entry.getKey()) - entry.getValue()) + " | available " + entry.getValue()
                    + " | total " + totals.get(entry.getKey()) + "\n");
            out.write("\nFINAL EVALUATION\n");
            out.write("Average zone readiness: " + snapshot.preparedness() + "%; "
                    + snapshot.missions().stream().filter(m -> m.status() == MissionStatus.COMPLETED).count()
                    + " missions completed. This is a fictional simulation summary only.\n");
        }
        return file;
    }
    public static Path exportCsv(Path folder, EarthquakeEngine engine) throws IOException {
        Path file = folder.resolve("earthquakeready-zones.csv");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("zone_id,zone_name,hazard_score,preparedness_score,infrastructure_status\n");
            for (var zone : engine.snapshot().zones()) out.write(csv(zone.id()) + "," + csv(zone.name()) + ","
                    + zone.hazard() + "," + zone.preparedness() + "," + csv(zone.infrastructure()) + "\n");
        }
        return file;
    }
    private static String csv(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }
}
