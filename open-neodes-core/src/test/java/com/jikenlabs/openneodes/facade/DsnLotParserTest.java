package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnGroup;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DsnLotParserTest {

    @Test
    void should_parse_lot_with_multiple_groups_and_dsns() throws Exception {
        String path = "sample_dsn_P25V01.dsn";
        DsnLotParser parser = new DsnLotParser();

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        List<DsnGroup> groups = new ArrayList<>();
        List<DsnBlockInstance> lotHeaders = new ArrayList<>();
        List<DsnBlockInstance> lotFooters = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                throw new java.io.FileNotFoundException("Resource not found: " + path);
            parser.parseLotStreaming(is, new DsnLotListener() {
                DsnGroup currentGroup;

                @Override
                public void onLotHeader(DsnBlockInstance header) {
                    lotHeaders.add(header);
                }

                @Override
                public void onGroupHeader(DsnBlockInstance groupHeader) {
                    currentGroup = new DsnGroup(groupHeader);
                    groups.add(currentGroup);
                }

                @Override
                public void onDeclaration(Stream<String> lines) {
                    if (currentGroup != null)
                        currentGroup.addDeclaration(lines.toList());
                }

                @Override
                public void onGroupFooter(DsnBlockInstance groupFooter) {
                    if (currentGroup != null)
                        currentGroup.setFooter(groupFooter);
                }

                @Override
                public void onLotFooter(DsnBlockInstance lotFooter) {
                    lotFooters.add(lotFooter);
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

            assertFalse(lotHeaders.isEmpty());
            DsnBlockInstance header = lotHeaders.get(0);
            assertEquals("V01R02", header.getValue("VersionNorme"));
            assertEquals("769608", header.getValue("NumeroLot"));

            assertFalse(groups.isEmpty());

            // Check first group (DSNOK)
            DsnGroup firstGroup = groups.get(0);
            assertEquals("DSNOK", firstGroup.getHeader().getValue("CodeGroupe"));
            assertEquals(3, firstGroup.getDeclarations().size());

            // Check if S96 is populated
            assertNotNull(firstGroup.getFooter());
            assertEquals("FINSL", firstGroup.getFooter().getValue("FinLigne"));

            // Check overall footer S95
            assertFalse(lotFooters.isEmpty());
            assertEquals("769608", lotFooters.get(0).getValue("NumeroLot"));
        }
    }
}
