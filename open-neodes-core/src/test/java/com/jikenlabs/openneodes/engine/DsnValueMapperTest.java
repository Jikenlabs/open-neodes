package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.exception.DsnBusinessException;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DsnValueMapperTest {

    private final DsnValueMapper mapper = new DsnValueMapper();

    @Test
    void should_map_string_type_x() {
        RubriqueDefinition def = new RubriqueDefinition("Nom", "X", 10, null);
        Object result = mapper.map("DOE", def, 1, "S21.G00.30.002");
        assertEquals("DOE", result);
    }

    @Test
    void should_map_numeric_type_n() {
        RubriqueDefinition def = new RubriqueDefinition("Quotité", "N", 5, null);
        Object result = mapper.map("151.67", def, 1, "S21.G00.40.013");
        assertEquals(new BigDecimal("151.67"), result);
    }

    @Test
    void should_map_date_type_d() {
        RubriqueDefinition def = new RubriqueDefinition("Date début", "D", 8, null);
        Object result = mapper.map("01012025", def, 1, "S21.G00.40.001");
        assertEquals(LocalDate.of(2025, 1, 1), result);
    }

    @Test
    void should_map_enum_to_dsnenumoption() {
        Map<String, String> options = Map.of("01", "Masculin", "02", "Féminin");
        RubriqueDefinition def = new RubriqueDefinition("Sexe", "X", 2, options);

        Object result = mapper.map("01", def, 1, "S21.G00.30.003");

        assertTrue(result instanceof DsnEnumOption);
        DsnEnumOption enumOption = (DsnEnumOption) result;
        assertEquals("01", enumOption.code());
        assertEquals("Masculin", enumOption.label());
    }

    @Test
    void should_throw_exception_for_invalid_enum() {
        Map<String, String> options = Map.of("01", "Masculin");
        RubriqueDefinition def = new RubriqueDefinition("Sexe", "X", 2, options);

        DsnBusinessException ex = assertThrows(DsnBusinessException.class,
                () -> mapper.map("99", def, 10, "S21.G00.30.003"));

        assertTrue(ex.getMessage().contains("Invalid enum code"));
        assertEquals(10, ex.getLineNumber());
        assertEquals("S21.G00.30.003", ex.getRubriqueId());
        assertEquals("99", ex.getFaultyValue());
    }

    @Test
    void should_throw_exception_for_invalid_date() {
        RubriqueDefinition def = new RubriqueDefinition("Date", "D", 8, null);
        assertThrows(DsnBusinessException.class,
                () -> mapper.map("20250101", def, 1, "ID")); // Wrong format YYYYMMDD instead of DDMMYYYY
    }

    @Test
    void should_handle_null_or_empty_values() {
        RubriqueDefinition def = new RubriqueDefinition("Nom", "X", 10, null);
        assertNull(mapper.map(null, def, 1, "ID"));
        assertNull(mapper.map("", def, 1, "ID"));
    }
}
