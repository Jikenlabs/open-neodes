package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;

import com.jikenlabs.openneodes.model.DsnEnvelope;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies US #13: Poly-Nature declarations and contextual validation.
 */
class DsnPolyNatureTest {

        private DsnHierarchicalParser parser;

        @BeforeEach
        void setUp() throws ExecutionException, InterruptedException {
                DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
                parser = new DsnHierarchicalParser(new DsnLineParser(), registry);
        }

        @Test
        void should_allow_remuneration_in_monthly_dsn_but_forbid_in_end_of_contract() {
                // Nature 01 (Mensuelle) allows S21.G00.51 (Remuneration)
                List<String> monthlyLines = List.of(
                                "S10.G00.00.001,'Pay'",
                                "S10.G00.00.002,'Jiken'",
                                "S10.G00.00.003,'1.0'",
                                "S10.G00.00.006,'P25V01'",
                                "S20.G00.05.001,'01'",
                                "S21.G00.06.001,'123'",
                                "S21.G00.11.001,'456'",
                                "S21.G00.30.001,'NIR1'",
                                "S21.G00.30.002,'DOE'",
                                "S21.G00.50.001,'01012025'",
                                "S21.G00.51.011,'001'",
                                "S21.G00.51.013,'1500.00'",
                                "S90.G00.90.001,'14'",
                                "S90.G00.90.002,'1'");
                assertDoesNotThrow(() -> parser.parseEnvelope(monthlyLines));

                List<String> endOfContractLines = List.of(
                                "S10.G00.00.001,'Pay'",
                                "S10.G00.00.002,'Jiken'",
                                "S10.G00.00.003,'1.0'",
                                "S10.G00.00.006,'P25V01'",
                                "S20.G00.05.001,'07'",
                                "S21.G00.06.001,'123'",
                                "S21.G00.11.001,'456'",
                                "S21.G00.30.001,'NIR1'",
                                "S21.G00.30.002,'DOE'",
                                "S21.G00.50.001,'01012025'",
                                "S90.G00.90.001,'12'",
                                "S90.G00.90.002,'1'");
                assertDoesNotThrow(() -> parser.parseEnvelope(endOfContractLines));
        }

        @Test
        void should_reset_hierarchy_for_each_s20() {
                List<String> mixedLines = List.of(
                                "S10.G00.00.001,'Pay'",
                                "S10.G00.00.002,'Jiken'",
                                "S10.G00.00.003,'1.0'",
                                "S10.G00.00.006,'P25V01'",
                                "S20.G00.05.001,'01'",
                                "S21.G00.06.001,'123'",
                                "S21.G00.11.001,'456'",
                                "S21.G00.30.001,'NIR1'",
                                "S21.G00.30.002,'DOE1'",
                                "S21.G00.40.001,'01012025'",
                                "S20.G00.05.001,'07'", // New Nature Switch
                                "S21.G00.06.001,'123'",
                                "S21.G00.11.001,'456'",
                                "S21.G00.30.001,'NIR1'",
                                "S21.G00.30.002,'DOE1'",
                                "S21.G00.40.001,'01012025'", // Required parent for S21.G00.62
                                "S21.G00.62.001,'01012025'", // S21.62 allowed in 07
                                "S90.G00.90.001,'19'",
                                "S90.G00.90.002,'2'");

                DsnEnvelope env = parser.parseEnvelope(mixedLines);
                assertEquals(2, env.declarations().size());

                var decl01 = env.declarations().get(0);
                var decl07 = env.declarations().get(1);

                // Use key-based lookup for verification
                assertTrue(decl01.getChildren("S21.G00.06").get(0).getChildren("S21.G00.11").get(0)
                                .getChildren("S21.G00.30")
                                .get(0).getChildren("S21.G00.40").size() > 0);
                assertTrue(decl07.getChildren("S21.G00.06").get(0).getChildren("S21.G00.11").get(0)
                                .getChildren("S21.G00.30")
                                .get(0).getChildren("S21.G00.40").get(0).getChildren("S21.G00.62").size() > 0);
        }
}
