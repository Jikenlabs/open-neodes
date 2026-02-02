package com.jikenlabs.openneodes.norm;

import com.jikenlabs.openneodes.exception.DefinitionNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;

class DsnNormRegistryTest {

    @Test
    void should_load_norm_from_yaml() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        assertNotNull(registry);
        assertNotNull(registry.getNorm());
        assertEquals("P25V01", registry.getNorm().version());
    }

    @Test
    void should_retrieve_rubrique_definition() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        RubriqueDefinition def = registry.getDefinition("S21.G00.30.001");
        assertNotNull(def);
        assertEquals("Identifiant", def.name());
        assertEquals("X", def.type());
    }

    @Test
    void should_throw_exception_when_rubrique_not_found() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        assertThrows(DefinitionNotFoundException.class, () -> registry.getDefinition("UNKNOWN.KEY"));
    }

    @Test
    void should_retrieve_block_definition() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        DsnBlockDefinition block = registry.getBlock("S21.G00.30");
        assertNotNull(block);
        assertEquals("Individu", block.name());
    }

    @Test
    void should_list_segments_with_rubrics() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        Map<String, List<String>> segments = registry.getNorm().getSegmentsWithRubrics();
        assertNotNull(segments);
        assertTrue(segments.containsKey("S21.G00.30"));
        assertTrue(segments.get("S21.G00.30").contains("S21.G00.30.001"));
    }

    @Test
    void should_load_natures_correctly() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").get();
        assertNotNull(registry.getNorm().natures());
        assertTrue(registry.getNorm().natures().containsKey("01"));

        DsnNatureConfiguration nature01 = registry.getNorm().natures().get("01");
        assertNotNull(nature01.map());
        assertTrue(nature01.map().containsKey("S21.G00.06"), "Nature 01 should contain S21.G00.06");

        DsnBlockMapping s21_06 = nature01.map().get("S21.G00.06");
        assertNotNull(s21_06.children());
        assertTrue(s21_06.children().containsKey("S21.G00.11"));
    }
}
