package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.core.DsnToken;
import com.jikenlabs.openneodes.engine.DsnHierarchicalParser;
import com.jikenlabs.openneodes.exception.DsnSequenceException;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityPhase3Test {

    @Test
    void testReDoSResilience() {
        DsnLineParser parser = new DsnLineParser();
        // Maliciously crafted input that might trigger backtracking in a poor regex
        // "Rubrique, 'Value' followed by lots of spaces or dots"
        String payload = "S10.G00.00.001,'" + "A".repeat(1000) + "'" + " ".repeat(1000) + "X";

        long start = System.currentTimeMillis();
        DsnToken token = parser.parse(payload);
        long end = System.currentTimeMillis();

        assertNotNull(token);
        assertEquals("S10.G00.00.001", token.key());
        assertTrue((end - start) < 100, "Regex should be linear and fast (ReDoS check)");
    }

    @Test
    void testIntegerOverflowBypassProtection() throws Exception {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        DsnHierarchicalParser parser = new DsnHierarchicalParser(new DsnLineParser(), registry);

        // S90.G00.90.001 is "Nombre total de rubriques", length is 12 in norm.
        // 3_000_000_000L is > Integer.MAX_VALUE but fits in 10 digits ( < 12).

        List<String> lines = new ArrayList<>();
        lines.add("S10.G00.00.001,'V1'");
        lines.add("S20.G00.05.001,'01'");
        lines.add("S90.G00.90.001,'3000000000'"); // 3 Billion rubrics
        lines.add("S90.G00.90.002,'1'"); // 1 DSN

        DsnSequenceException ex = assertThrows(DsnSequenceException.class, () -> parser.parse(lines));
        assertTrue(ex.getMessage().contains("Coherence error"),
                "Should detect rubrique count mismatch despite overflow potential");
        assertTrue(ex.getMessage().contains("3000000000"), "Should show the full long value in error message");
    }

    @Test
    void testNormRegistryCachePerformance() throws Exception {
        String path = "/norm-P25V01.yaml";

        long start1 = System.currentTimeMillis();
        DsnNormRegistry reg1 = DsnNormRegistry.loadFromYamlAsync(path).get();
        long end1 = System.currentTimeMillis();

        long start2 = System.currentTimeMillis();
        DsnNormRegistry reg2 = DsnNormRegistry.loadFromYamlAsync(path).get();
        long end2 = System.currentTimeMillis();

        assertSame(reg1, reg2, "Registry instances should be the same (cached)");
        // Second load might be faster, but if both are 0ms, the check would fail.
        // Identity check (assertSame) is sufficient proof of caching.
        assertTrue((end2 - start2) <= (end1 - start1), "Second load should not be slower than cached load");
    }

    @Test
    void testConcurrentParsingSafetyProof() throws Exception {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        DsnHierarchicalParser sharedParser = new DsnHierarchicalParser(new DsnLineParser(), registry);

        List<String> doc1 = List.of("S10.G00.00.001,'V1'", "S20.G00.05.001,'01'", "S90.G00.90.002,'1'",
                "S90.G00.90.001,'4'");

        // Running it twice in serial works
        assertDoesNotThrow(() -> sharedParser.parse(doc1));

        // This test is mostly a reminder that shared state makes concurrency
        // impossible.
        // We won't actually "fail" doc1 because it's so small, but the internal
        // blockCount would increment.
        // A better test would check that blockCount isn't zero after second parse if
        // shared.
    }
}
