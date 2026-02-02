package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnLotEvent;
import com.jikenlabs.openneodes.model.DsnLotEvent.DeclarationEvent;
import com.jikenlabs.openneodes.model.DsnLotEvent.LotHeaderEvent;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DsnLotParserNestedStreamTest {

    @Test
    void should_process_lot_using_nested_streams() throws Exception {
        String path = "sample_dsn_P25V01.dsn";
        DsnLotParser parser = new DsnLotParser();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                throw new java.io.FileNotFoundException("Resource not found: " + path);
            Stream<DsnLotEvent> lotStream = parser.streamLot(is);

            List<String> allLines = new ArrayList<>();
            int[] declarationCount = { 0 };

            lotStream.forEach(event -> {
                if (event instanceof LotHeaderEvent lotHeader) {
                    assertEquals("V01R02", lotHeader.header().getValue("VersionNorme"));
                } else if (event instanceof DeclarationEvent decl) {
                    declarationCount[0]++;
                    // On consomme le sous-stream
                    decl.lines().forEach(line -> {
                        allLines.add(line);
                        boolean matches = line.matches("^S[1-9][0-9].*");
                        assertTrue(matches, "Line should be a DSN segment (S10-S90): " + line);
                    });
                }
            });

            assertEquals(3, declarationCount[0]);
            assertFalse(allLines.isEmpty());
            // Vérifie que les lignes commencent bien par S10 et finissent par S90 (environ)
            assertTrue(allLines.get(0).startsWith("S10"));
            assertTrue(allLines.stream().anyMatch(l -> l.startsWith("S90")));
        }
    }
}
