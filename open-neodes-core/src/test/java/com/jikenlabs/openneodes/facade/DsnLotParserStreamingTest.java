package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnBlockInstance;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DsnLotParserStreamingTest {

    @Test
    void should_stream_lot_events() throws Exception {
        String path = "sample_dsn_P25V01.dsn";
        DsnLotParser parser = new DsnLotParser();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger headers = new AtomicInteger();
        AtomicInteger groups = new AtomicInteger();
        AtomicInteger declarations = new AtomicInteger();
        AtomicInteger footers = new AtomicInteger();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                throw new java.io.FileNotFoundException("Resource not found: " + path);
            parser.parseLotStreaming(is, new DsnLotListener() {
                @Override
                public void onLotHeader(DsnBlockInstance header) {
                    headers.incrementAndGet();
                    assertEquals("S05.G51.00", header.getKey());
                }

                @Override
                public void onGroupHeader(DsnBlockInstance groupHeader) {
                    groups.incrementAndGet();
                    assertEquals("S06.G51.00", groupHeader.getKey());
                }

                @Override
                public void onDeclaration(Stream<String> lines) {
                    declarations.incrementAndGet();
                    // On peut choisir d'ignorer ou de compter les lignes
                    long count = lines.count();
                    assertTrue(count > 0);
                }

                @Override
                public void onGroupFooter(DsnBlockInstance groupFooter) {
                    assertEquals("S96.G51.00", groupFooter.getKey());
                }

                @Override
                public void onLotFooter(DsnBlockInstance lotFooter) {
                    footers.incrementAndGet();
                    assertEquals("S95.G51.00", lotFooter.getKey());
                }

                @Override
                public void onError(Throwable e) {
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(1, headers.get());
            assertEquals(4, groups.get());
            assertEquals(3, declarations.get()); // Le lot contient 3 DSN dans le premier groupe
            assertEquals(1, footers.get());
        }
    }
}
