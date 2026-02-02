package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnTreeBuilderTest {

    private DsnHierarchicalParser parser;
    private DsnNormRegistry registry;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        DsnLineParser lineParser = new DsnLineParser();
        registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        parser = new DsnHierarchicalParser(lineParser, registry);
    }

    @Test
    void should_preserve_rubrique_order_using_sequenced_map() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Emetteur'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.30.001,'INDIV1'",
                "S21.G00.30.004,'John'",
                "S21.G00.30.002,'DOE'",
                "S90.G00.90.001,'12'",
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);
        DsnBlockInstance s20 = doc.getRootBlocks().get(1);
        DsnBlockInstance individu = s20
                .getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30").get(0);

        SequencedMap<String, Object> values = individu.getValues();
        List<String> keys = new ArrayList<>(values.keySet());

        assertEquals("S21.G00.30.001", keys.get(0));
        assertEquals("S21.G00.30.004", keys.get(1));
        assertEquals("S21.G00.30.002", keys.get(2));
    }

    @Test
    void should_auto_close_blocks_on_new_sibling() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Emetteur'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123'",
                "S21.G00.11.001,'456'",
                "S21.G00.30.001,'NIR1'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.40.001,'01012025'", // Contrat
                "S21.G00.11.001,'789'", // New Etablissement (sibling of 456)
                "S21.G00.30.001,'NIR2'",
                "S21.G00.30.002,'SMITH'",
                "S90.G00.90.001,'15'",
                "S90.G00.90.002,'1'");

        List<String> closedBlocks = new ArrayList<>();
        DsnBlockListener listener = block -> closedBlocks.add(block.getKey());

        parser = new DsnHierarchicalParser(new DsnLineParser(), registry, List.of(listener));
        parser.parse(lines);

        // New Etablissement (789) starts: Closes CONTRAT, INDIV1, ETABLISSMENT(456)
        assertTrue(closedBlocks.contains("S21.G00.40"));
        assertTrue(closedBlocks.contains("S21.G00.30"));
        assertTrue(closedBlocks.contains("S21.G00.11"));
    }

    @Test
    void should_detach_children_for_ram_optimization() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Emetteur'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.30.001,'INDIV1'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.40.001,'01012025'",
                "S20.G00.05.001,'01'", // New root block S20 closes S10, and children
                "S90.G00.90.001,'13'",
                "S90.G00.90.002,'2'");

        parser = new DsnHierarchicalParser(new DsnLineParser(), registry, List.of(), true);
        DsnDocument doc = parser.parse(lines);

        DsnBlockInstance s10 = doc.getRootBlocks().get(0);
        assertTrue(s10.getChildren().isEmpty()); // Detached
    }
}
