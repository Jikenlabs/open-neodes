package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.engine.DsnBlockListener;
import com.jikenlabs.openneodes.engine.DsnJsonExporter;
import com.jikenlabs.openneodes.model.DsnEnvelope;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full integration test for Monthly Norm structure and Technical Envelope (US
 * #12).
 */
class Sprint2IntegrationTest {

    @Test
    void should_process_full_dsn_cycle() throws ExecutionException, InterruptedException {
        String dsnContent = "S10.G00.00.001,'OpenDSN Parser'\n" +
                "S10.G00.00.002,'Jiken'\n" +
                "S10.G00.00.003,'1.0'\n" +
                "S10.G00.00.006,'P25V01'\n" +
                "S20.G00.05.001,'01'\n" + // Declaration 1
                "S21.G00.06.001,'123456789'\n" +
                "S21.G00.11.001,'12345'\n" +
                "S21.G00.15.001,'IP123'\n" +
                "S21.G00.15.005,'ADH'\n" +
                "S21.G00.30.001,'NIR1'\n" +
                "S21.G00.30.002,'DOE'\n" +
                "S21.G00.30.005,'01'\n" +
                "S21.G00.40.001,'01012025'\n" +
                "S21.G00.40.013,'151.67'\n" +
                "S21.G00.30.001,'NIR2'\n" +
                "S21.G00.30.002,'SMITH'\n" +
                "S21.G00.30.005,'02'\n" +
                "S21.G00.40.001,'15012025'\n" +
                "S21.G00.40.013,'35.00'\n" +
                "S90.G00.90.001,'21'\n" +
                "S90.G00.90.002,'1'"; // 1 DSN (S20 block)

        InputStream is = new ByteArrayInputStream(dsnContent.getBytes(StandardCharsets.ISO_8859_1));
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        DsnJsonExporter exporter = new DsnJsonExporter(registry);

        List<String> exportedJsons = new ArrayList<>();

        DsnBlockListener individuListener = block -> {
            if ("S21.G00.30".equals(block.getKey())) {
                try {
                    exportedJsons.add(exporter.toJson(block));
                } catch (Exception e) {
                    fail("JSON export failed for Individu: " + e.getMessage());
                }
            }
        };

        // Use new parseEnvelopeAsync
        CompletableFuture<DsnEnvelope> future = DsnParser.parseEnvelopeAsync(is, registry, List.of(individuListener),
                false);
        DsnEnvelope envelope = future.get();

        // 1. Verify Envelope
        assertNotNull(envelope.header());
        assertEquals(1, envelope.declarations().size());
        assertNotNull(envelope.footer());
        assertEquals(1, ((Number) envelope.footer().getValue("nbDSN")).intValue());

        // 2. Verify Events (2 Individus)
        assertEquals(2, exportedJsons.size());

        // 3. Verify types on the first Individu (DOE)
        var indiv1 = envelope.declarations().get(0)
                .getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30").get(0);

        assertEquals("DOE", indiv1.getValue("NomFamille"));

        Object sexe = indiv1.getValue("Sexe");
        assertTrue(sexe instanceof DsnEnumOption);
        assertEquals("masculin", ((DsnEnumOption) sexe).label());

        var contrats = indiv1.getChildren("S21.G00.40").get(0);
        assertEquals(LocalDate.of(2025, 1, 1), contrats.getValue("DateDebut"));
        assertEquals(new BigDecimal("151.67"), contrats.getValue("S21.G00.40.013"));
    }
}
