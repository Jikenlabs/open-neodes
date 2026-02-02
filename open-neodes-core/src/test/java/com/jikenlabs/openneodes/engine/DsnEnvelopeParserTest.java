package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.exception.DsnHierarchyException;
import com.jikenlabs.openneodes.exception.DsnSequenceException;
import com.jikenlabs.openneodes.model.DsnEnvelope;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnEnvelopeParserTest {

    private DsnHierarchicalParser parser;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        parser = new DsnHierarchicalParser(new DsnLineParser(), registry);
    }

    @Test
    void should_parse_valid_envelope() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Logiciel'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'987654321'",
                "S90.G00.90.001,'10'",
                "S90.G00.90.002,'2'" // Total DSN = 2
        );

        DsnEnvelope envelope = parser.parseEnvelope(lines);

        assertNotNull(envelope.header());
        assertEquals(2, envelope.declarations().size());
        assertNotNull(envelope.footer());
        assertEquals(2, ((Number) envelope.footer().getValue("nbDSN")).intValue());
    }

    @Test
    void should_throw_if_s21_before_s10() {
        List<String> lines = List.of(
                "S21.G00.06.001,'123456789'");
        assertThrows(DsnHierarchyException.class, () -> parser.parseEnvelope(lines));
    }

    @Test
    void should_throw_if_s20_before_s10() {
        List<String> lines = List.of(
                "S20.G00.05.001,'01'");
        assertThrows(DsnSequenceException.class, () -> parser.parseEnvelope(lines));
    }

    @Test
    void should_throw_on_footer_coherence_error() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Logiciel'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S90.G00.90.001,'10'",
                "S90.G00.90.002,'5'" // Expected 5, but only 1 found
        );
        DsnSequenceException ex = assertThrows(DsnSequenceException.class, () -> parser.parseEnvelope(lines));
        assertTrue(ex.getMessage().contains("Coherence error"));
    }
}
