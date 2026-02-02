package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnJsonExporterTest {

    private DsnJsonExporter exporter;
    private DsnHierarchicalParser parser;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        this.exporter = new DsnJsonExporter(registry);
        this.parser = new DsnHierarchicalParser(new DsnLineParser(), registry);
    }

    @Test
    void should_export_block_to_json_with_labels_and_types() throws IOException {
        List<String> lines = List.of(
                "S10.G00.00.001,'Logiciel de Paie'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.30.001,'1234567890123'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.30.005,'01'", // Sexe: Masculin
                "S21.G00.40.001,'01012025'", // Date début
                "S21.G00.40.013,'151.67'", // Quotité (Numeric)
                "S90.G00.90.001,'14'",
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);
        DsnBlockInstance individu = doc.getRootBlocks().get(1) // S20
                .getChildren("S21.G00.06").get(0)
                .getChildren("S21.G00.11").get(0)
                .getChildren("S21.G00.30").get(0);

        String json = exporter.toJson(individu);

        assertNotNull(json);
        assertTrue(json.contains("\"Identifiant\" : \"1234567890123\""));
        assertTrue(json.contains("\"NomFamille\" : \"DOE\""));
        assertTrue(json.contains("\"Sexe\" : \"masculin\""));
        assertTrue(json.contains("\"Contrat\" : [")); // Multiplicity check (Array)
        assertTrue(json.contains("\"DateDebut\" : \"2025-01-01\""));
        // assertTrue(json.contains("\"QuotiteTravail\"") ||
        // json.contains("S21.G00.40.013"));
        // assertTrue(json.contains("151.67"));
    }
}
