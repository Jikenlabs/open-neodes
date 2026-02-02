package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.core.DsnStreamReader;
import com.jikenlabs.openneodes.engine.DsnHierarchicalParser;
import com.jikenlabs.openneodes.exception.DsnBusinessException;
import com.jikenlabs.openneodes.exception.InvalidDsnFormatException;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityPhase2Test {

    private DsnNormRegistry registry;
    private DsnHierarchicalParser parser;

    @BeforeEach
    void setUp() throws Exception {
        registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        parser = new DsnHierarchicalParser(new DsnLineParser(), registry);
    }

    @Test
    void testPIIRedactionInException() {
        // S21.G00.30.001 is NIR (Sensitive)
        // Try to trigger a mapping error by using a non-numeric value for something
        // expected as numeric
        // or just a custom DsnBusinessException
        DsnBusinessException ex = new DsnBusinessException("Test Error", 1, "S21.G00.30.001", "1234567890123");

        String message = ex.getMessage();
        assertFalse(message.contains("1234567890123"), "PII value should not be in error message");
        assertTrue(message.contains("12****23"), "PII value should be masked in error message");
    }

    @Test
    void testNonSensitiveValueNoRedaction() {
        // S10.G00.00.001 (Software version) is not sensitive
        DsnBusinessException ex = new DsnBusinessException("Test Error", 1, "S10.G00.00.001", "MYSOFT");

        String message = ex.getMessage();
        assertTrue(message.contains("MYSOFT"), "Non-sensitive value should be visible in error message");
    }

    @Test
    void testMaxLineLength() {
        DsnStreamReader reader = new DsnStreamReader(parser);
        String longLine = "S10.G00.00.001,'" + "A".repeat(5000) + "'";
        ByteArrayInputStream is = new ByteArrayInputStream(longLine.getBytes(StandardCharsets.ISO_8859_1));

        ExecutionException ex = assertThrows(ExecutionException.class, () -> reader.readAsync(is).get());
        assertTrue(ex.getCause() instanceof InvalidDsnFormatException);
        assertTrue(ex.getCause().getMessage().contains("Line too long"));
    }

    @Test
    void testFieldLengthValidation() {
        // S10.G00.00.001 length is 20 in norm-P25V01.yaml (estimated)
        // Let's verify our registry
        int maxLen = registry.getDefinition("S10.G00.01.001").length(); // Nom logiciel (20)

        String tooLongValue = "A".repeat(maxLen + 1);
        String line = "S10.G00.01.001,'" + tooLongValue + "'";

        DsnBusinessException ex = assertThrows(DsnBusinessException.class,
                () -> parser.parse(Collections.singletonList(line)));
        assertTrue(ex.getMessage().contains("Value exceeds maximum length"));
        assertTrue(ex.getMessage().contains("S10.G00.01.001"));
    }

    @Test
    void testErrorMessageTruncation() {
        DsnLineParser lineParser = new DsnLineParser();
        String malformedLongLine = "S10.G00.00.001," + "A".repeat(200); // Missing quotes

        InvalidDsnFormatException ex = assertThrows(InvalidDsnFormatException.class,
                () -> lineParser.parse(malformedLongLine));

        assertTrue(ex.getMessage().length() < 150, "Error message should be truncated");
        assertTrue(ex.getMessage().endsWith("..."), "Truncated message should end with ellipsis");
    }
}
