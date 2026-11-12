package com.earthquakeready.io;

import com.earthquakeready.domain.EarthquakeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveServiceTest {
    @TempDir Path temp;

    @Test void typedEventArchiveCanBeReadBack() throws Exception {
        ArchiveService archive = new ArchiveService(temp.resolve("events.bin"));
        try {
            archive.append(new EarthquakeEvent(1, "RES", 44, "sample")).get();
            archive.append(new EarthquakeEvent(2, "SHE", 31, "sample two")).get();
            assertEquals(2, archive.recordCount());
        } finally { archive.close(); }
    }
}
