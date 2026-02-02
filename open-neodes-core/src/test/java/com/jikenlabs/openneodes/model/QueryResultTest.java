package com.jikenlabs.openneodes.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QueryResultTest {

    @Test
    void should_handle_single_value() {
        QueryResult qr = QueryResult.of("value");
        assertTrue(qr.isPresent());
        assertEquals(1, qr.size());
        assertEquals("value", qr.asString().orElse(null));
        assertTrue(qr.asBlock().isEmpty());
    }

    @Test
    void should_handle_block() {
        DsnBlockInstance block = new DsnBlockInstance("S21.G00.06");
        QueryResult qr = QueryResult.of(block);
        assertTrue(qr.isPresent());
        assertEquals(block, qr.asBlock().orElse(null));
        assertTrue(qr.asValue().isEmpty());
    }

    @Test
    void should_handle_list() {
        QueryResult qr = QueryResult.of(List.of("v1", "v2"));
        assertEquals(2, qr.size());
        assertEquals(List.of("v1", "v2"), qr.asValues());
    }

    @Test
    void should_chain_queries() {
        DsnBlockInstance root = new DsnBlockInstance("ROOT");
        DsnBlockInstance child = new DsnBlockInstance("CHILD");
        child.addValue("Value", "secret");
        root.addChild(child);

        QueryResult qr = QueryResult.of(root).query("CHILD").query("Value");
        assertEquals("secret", qr.asString().orElse(null));
    }
}
