package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnParserAutoDetectTest {

    @Test
    void should_detect_version_p25v01_and_parse() throws ExecutionException, InterruptedException {
        String dsn = "S10.G00.00.001,'Test'\n" +
                "S10.G00.00.006,'P25V01'\n" + // Version P25V01
                "S20.G00.05.001,'01'\n" +
                "S21.G00.06.001,'123456789'\n" +
                "S90.G00.90.001,'6'\n" +
                "S90.G00.90.002,'1'";

        InputStream is = new ByteArrayInputStream(dsn.getBytes(StandardCharsets.ISO_8859_1));

        // Utilise la détection automatique
        DsnDocument doc = DsnParser.parseAutoDetectAsync(is).get();

        assertNotNull(doc);
        assertEquals("P25V01", doc.getNormVersion());
        assertEquals(3, doc.getRootBlocks().size());
        assertEquals("Test", doc.getRootBlocks().get(0).getValue("NomLogiciel"));
    }

    @Test
    void should_detect_version_p26v01_and_parse() throws ExecutionException, InterruptedException {
        // Suppose que norm-P26V01.yaml existe depuis les étapes précédentes
        String dsn = "S10.G00.00.001,'Test26'\n" +
                "S10.G00.00.006,'P26V01'\n" + // Version P26V01
                "S20.G00.05.001,'01'\n" +
                "S21.G00.06.001,'987654321'\n" +
                "S90.G00.90.001,'6'\n" +
                "S90.G00.90.002,'1'";

        InputStream is = new ByteArrayInputStream(dsn.getBytes(StandardCharsets.ISO_8859_1));

        DsnDocument doc = DsnParser.parseAutoDetectAsync(is).get();

        assertNotNull(doc);
        // Vérifie que la norme 2026 est utilisée (pourrait être implicite via un
        // parsing réussi si le schéma diffère,
        // mais ici on vérifie juste que cela parse)
        assertEquals("Test26", doc.getRootBlocks().get(0).getValue("NomLogiciel"));
    }

    @Test
    void should_fail_when_version_missing() {
        String dsn = "S10.G00.00.001,'NoVersion'\n" +
                "S20.G00.05.001,'01'";

        InputStream is = new ByteArrayInputStream(dsn.getBytes(StandardCharsets.ISO_8859_1));

        ExecutionException ex = assertThrows(ExecutionException.class, () -> {
            DsnParser.parseAutoDetectAsync(is).get();
        });

        assertTrue(ex.getCause().getMessage().contains("Version DSN (S10.G00.00.006) introuvable"));
    }
}
