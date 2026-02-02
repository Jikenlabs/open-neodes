package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import com.jikenlabs.openneodes.exception.InvalidDsnFormatException;
import com.jikenlabs.openneodes.exception.DsnSequenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnHierarchicalParserTest {

    private DsnHierarchicalParser parser;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        DsnLineParser lineParser = new DsnLineParser();
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        parser = new DsnHierarchicalParser(lineParser, registry);
    }

    @Test
    void should_parse_flat_lines_into_hierarchy_with_types() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Logiciel de Paie'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S20.G00.07.001,'VAL'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.30.001,'1234567890123'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.30.005,'01'",
                "S21.G00.40.001,'01012025'",
                "S21.G00.30.001,'9876543210987'",
                "S21.G00.30.002,'SMITH'",
                "S90.G00.90.001,'16'", // 4 (S10) + 1 (S20) + 1 (S20.07) + 1 (S21.06) + 1 (S21.11) + 3 (Indiv1) + 1
                                       // (Contrat) + 2 (Indiv2) + 2 (S90) = 16
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);

        assertNotNull(doc);
        assertEquals(3, doc.getRootBlocks().size()); // S10, S20, S90

        DsnBlockInstance s20 = doc.getRootBlocks().get(1);
        List<DsnBlockInstance> individus = s20.getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30");
        assertEquals(2, individus.size());

        DsnBlockInstance doe = individus.get(0);
        assertEquals("1234567890123", doe.getValue("Identifiant"));

        // Check Enum
        Object sexe = doe.getValue("Sexe");
        assertTrue(sexe instanceof DsnEnumOption);
        assertEquals("masculin", ((DsnEnumOption) sexe).label());

        // Check Date
        List<DsnBlockInstance> contrats = doe.getChildren("S21.G00.40");
        assertEquals(1, contrats.size());
        assertEquals(LocalDate.of(2025, 1, 1), contrats.get(0).getValue("DateDebut"));
    }

    @Test
    void should_throw_exception_if_no_header() {
        List<String> lines = List.of("S20.G00.05.001,'01'");
        assertThrows(DsnSequenceException.class, () -> parser.parseEnvelope(lines));
    }

    @Test
    void should_handle_complex_nesting_and_multiplicity() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Payer'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S20.G00.07.001,'VAL'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'456'",
                "S21.G00.30.001,'NIR1'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.40.001,'01012025'",
                "S21.G00.40.001,'02012025'",
                "S21.G00.30.001,'NIR2'",
                "S21.G00.30.002,'SMITH'",
                "S21.G00.40.001,'03012025'",
                "S90.G00.90.001,'17'",
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);
        DsnBlockInstance s20 = doc.getRootBlocks().get(1);
        List<DsnBlockInstance> individus = s20.getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30");

        assertEquals(2, individus.size());

        DsnBlockInstance indiv1 = individus.get(0);
        assertEquals(2, indiv1.getChildren("S21.G00.40").size());
    }

    @Test
    void should_throw_exception_if_s90_counts_mismatch() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Test'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S90.G00.90.001,'10'", // Expecting 10 rubrics, but only 7 provided
                "S90.G00.90.002,'1'");

        assertThrows(DsnSequenceException.class, () -> parser.parseEnvelope(lines));
    }

    @Test
    void should_throw_exception_for_unknown_block() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Test'",
                "UNKNOWN.BLOCK.001,'Value'");
        assertThrows(InvalidDsnFormatException.class, () -> parser.parse(lines));
    }
}
