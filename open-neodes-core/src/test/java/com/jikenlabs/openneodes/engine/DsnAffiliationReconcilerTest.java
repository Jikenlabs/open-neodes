package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.domain.Affiliation;
import com.jikenlabs.openneodes.domain.AyantDroit;
import com.jikenlabs.openneodes.domain.SalarieAffilie;
import com.jikenlabs.openneodes.exception.DsnBusinessException;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DsnAffiliationReconcilerTest {

    private DsnHierarchicalParser parser;
    private DsnAffiliationReconciler reconciler;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync("/norm-P25V01.yaml").join();
        this.parser = new DsnHierarchicalParser(new DsnLineParser(), registry);
        this.reconciler = new DsnAffiliationReconciler();
    }

    @Test
    void should_reconcile_salaries_and_ayants_droit() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Test'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.15.001,'IP123'",
                "S21.G00.15.005,'ADH'",
                "S21.G00.30.001,'NIR_SALARIE'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.40.001,'01012025'", // Need Contrat before Affiliation
                "S21.G00.70.004,'OPTION1'",
                "S21.G00.70.005,'POP1'",
                "S21.G00.70.013,'ADH'",
                "S21.G00.73.007,'NIR_KID'",
                "S21.G00.73.006,'JUNIOR'",
                "S90.G00.90.001,'19'", // 19 rubrics
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);
        List<Affiliation> affiliations = reconciler.reconcile(doc);

        assertEquals(1, affiliations.size());
        Affiliation aff = affiliations.get(0);

        assertTrue(aff.principal() instanceof SalarieAffilie);
        assertEquals("DOE", aff.principal().getNom());
        assertEquals("OPTION1", aff.option());
        assertEquals("ADH", aff.idAdhesionCible());

        assertEquals(1, aff.ayantsDroit().size());
        AyantDroit kid = aff.ayantsDroit().get(0);
        assertEquals("JUNIOR", kid.getNom());
    }

    @Test
    void should_throw_exception_on_broken_link() {
        List<String> lines = List.of(
                "S10.G00.00.001,'Test'",
                "S10.G00.00.002,'Jiken'",
                "S10.G00.00.003,'1.0'",
                "S10.G00.00.006,'P25V01'",
                "S20.G00.05.001,'01'",
                "S21.G00.06.001,'123456789'",
                "S21.G00.11.001,'12345'",
                "S21.G00.15.001,'CONTRAT1'",
                "S21.G00.15.005,'ADH'",
                "S21.G00.30.001,'NIR1'",
                "S21.G00.30.002,'DOE'",
                "S21.G00.40.001,'01012024'",
                "S21.G00.70.013,'UNK'",
                "S90.G00.90.001,'15'",
                "S90.G00.90.002,'1'");

        DsnDocument doc = parser.parse(lines);
        assertThrows(DsnBusinessException.class, () -> reconciler.reconcile(doc));
    }
}
