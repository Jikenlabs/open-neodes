package com.jikenlabs.openneodes.core;

import com.jikenlabs.openneodes.exception.InvalidDsnFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class DsnLineParserTest {

    private DsnLineParser parser;

    @BeforeEach
    void setUp() {
        parser = new DsnLineParser();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "S21.G00.30.001,'1234567890123' | S21.G00.30.001 | 1234567890123",
            " S21.G00.30.001 , '1234567890123' | S21.G00.30.001 | 1234567890123",
            "S21.G00.30.001,'' | S21.G00.30.001 | ",
            "S90.G00.00.001,'Fin'|S90.G00.00.001|Fin",
            "  S21.G00.30.001  ,  'Value With Space'  | S21.G00.30.001 | Value With Space",
            "S21.G00.06.004,'10 RUE D'IENA' | S21.G00.06.004 | 10 RUE D'IENA"
    })
    void should_parse_valid_lines(String line, String expectedKey, String expectedValue) {
        DsnToken token = parser.parse(line);
        assertEquals(expectedKey, token.key());
        assertEquals(expectedValue == null ? "" : expectedValue, token.value());
    }

    @Test
    void should_throw_exception_for_malformed_line() {
        assertThrows(InvalidDsnFormatException.class, () -> parser.parse("INVALID_LINE"));
        assertThrows(InvalidDsnFormatException.class, () -> parser.parse("KEY,VALUE_WITHOUT_QUOTES"));
        assertThrows(InvalidDsnFormatException.class, () -> parser.parse("S21.G00.30.001,123")); // Manque les quotes
    }

    @Test
    void should_handle_empty_values() {
        DsnToken token = parser.parse("S21.G00.30.001,''");
        assertEquals("", token.value());
    }

    @Test
    void should_throw_exception_for_null_line() {
        assertThrows(InvalidDsnFormatException.class, () -> parser.parse(null));
    }
}
