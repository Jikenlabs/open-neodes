package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.engine.DsnHierarchicalParser;
import com.jikenlabs.openneodes.exception.DsnHierarchyException;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAuditTest {

    @Test
    void shouldFailOnMaliciousVersion() {
        String dsnContent = "S10.G00.00.006,'../../../etc/passwd'\nS20.G00.05.001,'01'";
        InputStream is = new ByteArrayInputStream(dsnContent.getBytes(StandardCharsets.ISO_8859_1));

        CompletionException exception = assertThrows(CompletionException.class, () -> {
            DsnParser.parseAutoDetectAsync(is).join();
        });

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Format de version DSN invalide"));
    }

    @Test
    void shouldFailOnTooManyBlocks() {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();

        // Limit to 3 blocks
        DsnHierarchicalParser limitParser = new DsnHierarchicalParser(new com.jikenlabs.openneodes.core.DsnLineParser(),
                registry, List.of(), false, 32, 3);

        List<String> manyBlocks = new ArrayList<>();
        manyBlocks.add("S10.G00.00.006,'P25V01'"); // Block 1
        manyBlocks.add("S20.G00.05.001,'01'"); // Block 2
        manyBlocks.add("S20.G00.05.001,'01'"); // Block 3
        manyBlocks.add("S20.G00.05.001,'01'"); // Block 4 -> Should Fail

        assertThrows(DsnHierarchyException.class, () -> {
            limitParser.parse(manyBlocks);
        });
    }

    @Test
    void shouldFailOnTooDeepNesting() {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();

        // Limit depth to 3
        DsnHierarchicalParser limitParser = new DsnHierarchicalParser(new com.jikenlabs.openneodes.core.DsnLineParser(),
                registry, List.of(), false, 3, 100);

        List<String> nestedBlocks = new ArrayList<>();
        nestedBlocks.add("S10.G00.00.006,'P25V01'"); // S10 (Depth 1)
        nestedBlocks.add("S20.G00.05.001,'01'"); // S20 (Depth 1)
        nestedBlocks.add("S21.G00.06.001,'123456789'"); // Entreprise (Depth 2)
        nestedBlocks.add("S21.G00.11.001,'00012'"); // Etablissement (Depth 3)
        nestedBlocks.add("S21.G00.30.001,'1234567890123'"); // Individu (Depth 4) -> Should Fail

        assertThrows(DsnHierarchyException.class, () -> {
            limitParser.parse(nestedBlocks);
        });
    }
}
