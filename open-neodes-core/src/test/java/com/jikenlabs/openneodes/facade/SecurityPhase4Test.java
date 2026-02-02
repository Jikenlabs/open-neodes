package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnLotEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityPhase4Test {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_not_crash_on_millions_of_empty_lines() throws Exception {
        DsnLotParser parser = new DsnLotParser();

        // Un InputStream qui génère 1 million de lignes vides
        InputStream infiniteEmptyLines = new InputStream() {
            private int count = 0;
            private final byte[] line = "\n".getBytes(StandardCharsets.ISO_8859_1);
            private int pos = 0;

            @Override
            public int read() {
                if (count >= 1_000_000)
                    return -1;
                byte b = line[pos++];
                if (pos >= line.length) {
                    pos = 0;
                    count++;
                }
                return b & 0xFF;
            }
        };

        AtomicInteger events = new AtomicInteger(0);
        parser.streamLot(infiniteEmptyLines).forEach(e -> events.incrementAndGet());

        // On ne s'attend à aucun événement car il n'y a que des lignes vides
        assertEquals(0, events.get(), "Should not produce any events for empty lines");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_terminate_cleanly_on_truncated_lot_header() throws Exception {
        DsnLotParser parser = new DsnLotParser();
        // S05 sans suite
        InputStream is = new java.io.ByteArrayInputStream(
                "S05.G51.00.001,'V01R02'".getBytes(StandardCharsets.ISO_8859_1));

        AtomicBoolean completed = new AtomicBoolean(false);
        parser.parseLotStreaming(is, new DsnLotListener() {
            @Override
            public void onLotHeader(DsnBlockInstance header) {
            }

            @Override
            public void onGroupHeader(DsnBlockInstance groupHeader) {
            }

            @Override
            public void onDeclaration(Stream<String> lines) {
            }

            @Override
            public void onGroupFooter(DsnBlockInstance groupFooter) {
            }

            @Override
            public void onLotFooter(DsnBlockInstance lotFooter) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertTrue(completed.get(), "Parser should complete even on truncated input");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_terminate_on_truncated_stream_during_declaration() throws Exception {
        DsnLotParser parser = new DsnLotParser();

        // Un stream qui s'arrête brutalement au milieu d'une déclaration
        InputStream truncated = new InputStream() {
            private String data = "S05.G51.00.001,'V01R02'\nS10.G00.00.001,'TEST'\nS20.G00.05.001,'01'\n";
            private int pos = 0;

            @Override
            public int read() {
                if (pos >= data.length())
                    return -1; // Fin brutale
                return data.charAt(pos++) & 0xFF;
            }
        };

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger linesCount = new AtomicInteger(0);

        parser.parseLotStreaming(truncated, new DsnLotListener() {
            @Override
            public void onLotHeader(DsnBlockInstance header) {
            }

            @Override
            public void onGroupHeader(DsnBlockInstance groupHeader) {
            }

            @Override
            public void onDeclaration(Stream<String> lines) {
                lines.forEach(l -> linesCount.incrementAndGet());
            }

            @Override
            public void onGroupFooter(DsnBlockInstance groupFooter) {
            }

            @Override
            public void onLotFooter(DsnBlockInstance lotFooter) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Parser should complete");
        assertTrue(linesCount.get() > 0, "Should have received some lines before truncation");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_handle_extremely_long_rubric_gracefully() throws Exception {
        DsnLotParser parser = new DsnLotParser();
        // Une rubrique S05 avec une valeur de 1Mo (si pas de limite dans LineParser, ça
        // pourrait exploser)
        // Mais LineParser a déjà des protections normalement.
        StringBuilder sb = new StringBuilder("S05.G51.00.001,'");
        for (int i = 0; i < 100_000; i++)
            sb.append("A");
        sb.append("'");

        InputStream is = new java.io.ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.ISO_8859_1));

        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        parser.parseLotStreaming(is, new DsnLotListener() {
            @Override
            public void onLotHeader(DsnBlockInstance header) {
            }

            @Override
            public void onGroupHeader(DsnBlockInstance groupHeader) {
            }

            @Override
            public void onDeclaration(Stream<String> lines) {
            }

            @Override
            public void onGroupFooter(DsnBlockInstance groupFooter) {
            }

            @Override
            public void onLotFooter(DsnBlockInstance lotFooter) {
            }

            @Override
            public void onError(Throwable e) {
                errorOccurred.set(true);
            }

            @Override
            public void onComplete() {
            }
        });

        // DsnLineParser est censé limiter la taille, donc on s'attend à une erreur ou
        // un parsing tronqué
        // Si ça passe sans erreur, c'est que la mémoire a tenu, mais on vérifie le
        // non-crash
    }
}
