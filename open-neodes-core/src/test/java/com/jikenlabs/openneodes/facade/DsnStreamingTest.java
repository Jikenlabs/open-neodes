package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DsnStreamingTest {

    @Test
    void should_parse_dsn_stream_with_iso_8859_1_and_virtual_threads() throws ExecutionException, InterruptedException {
        String dsnContent = "S10.G00.00.001,'Societe Elevée'\n" +
                "S10.G00.00.002,'Jiken'\n" +
                "S10.G00.00.003,'1.0'\n" +
                "S10.G00.00.006,'P25V01'\n" +
                "S20.G00.05.001,'01'\n" +
                "S21.G00.06.001,'123456789'\n" +
                "S21.G00.11.001,'12345'\n" +
                "S21.G00.30.001,'1234567890123'\n" +
                "S21.G00.30.002,'PRÉNOM Accentué'\n" +
                "S90.G00.90.001,'11'\n" +
                "S90.G00.90.002,'1'";

        InputStream is = new ByteArrayInputStream(dsnContent.getBytes(StandardCharsets.ISO_8859_1));
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();

        CompletableFuture<DsnDocument> future = DsnParser.parseAsync(is, registry);
        DsnDocument doc = future.get();

        assertNotNull(doc);
        DsnBlockInstance s10 = doc.getRootBlocks().get(0);
        assertEquals("Societe Elevée", s10.getValue("NomLogiciel"));

        DsnBlockInstance s21 = doc.getRootBlocks().get(1).getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30").get(0);
        assertEquals("PRÉNOM Accentué", s21.getValue("NomFamille"));
    }

    @Test
    void should_emit_events_with_typed_data() throws ExecutionException, InterruptedException {
        String dsnContent = "S10.G00.00.001,'Emetteur'\n" +
                "S10.G00.00.002,'Jiken'\n" +
                "S10.G00.00.003,'1.0'\n" +
                "S10.G00.00.006,'P25V01'\n" +
                "S20.G00.05.001,'01'\n" +
                "S21.G00.06.001,'123456789'\n" +
                "S21.G00.11.001,'12345'\n" +
                "S21.G00.30.001,'INDIV1'\n" +
                "S21.G00.30.002,'NOM1'\n" +
                "S21.G00.30.005,'01'\n" +
                "S90.G00.90.001,'12'\n" +
                "S90.G00.90.002,'1'"; // Masculin

        InputStream is = new ByteArrayInputStream(dsnContent.getBytes(StandardCharsets.ISO_8859_1));
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();

        AtomicInteger eventCount = new AtomicInteger(0);
        CompletableFuture<DsnDocument> future = DsnParser.parseAsync(is, registry, List.of(block -> {
            if ("S21.G00.30".equals(block.getKey())) {
                Object sexe = block.getValue("Sexe");
                assertTrue(sexe instanceof DsnEnumOption);
                assertEquals("masculin", ((DsnEnumOption) sexe).label());
                eventCount.incrementAndGet();
            }
        }), false);

        future.get();
        assertEquals(1, eventCount.get());
    }
}
