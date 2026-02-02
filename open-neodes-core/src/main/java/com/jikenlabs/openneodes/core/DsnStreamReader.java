package com.jikenlabs.openneodes.core;

import com.jikenlabs.openneodes.engine.DsnHierarchicalParser;
import com.jikenlabs.openneodes.model.DsnDocument;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Lecteur de fichiers DSN basé sur des flux mettant en valeur les Virtual
 * Threads Java 25.
 */
public class DsnStreamReader {

    private final DsnHierarchicalParser parser;
    private final Charset charset;

    /**
     * Constructeur avec encodage par défaut.
     *
     * @param parser Le parseur hiérarchique.
     */
    public DsnStreamReader(DsnHierarchicalParser parser) {
        this(parser, StandardCharsets.ISO_8859_1);
    }

    /**
     * Constructeur complet.
     *
     * @param parser  Le parseur hiérarchique.
     * @param charset Le jeu de caractères (par défaut ISO-8859-1).
     */
    public DsnStreamReader(DsnHierarchicalParser parser, Charset charset) {
        this.parser = parser;
        this.charset = charset;
    }

    private static final int MAX_LINE_LENGTH = 4096;

    /**
     * Lit et parse un flux DSN de manière asynchrone en utilisant des Virtual
     * Threads.
     *
     * @param inputStream Le flux DSN.
     * @return Un future pour le DsnDocument parsé.
     */
    public CompletableFuture<DsnDocument> readAsync(InputStream inputStream) {
        CompletableFuture<DsnDocument> future = new CompletableFuture<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                    Stream<String> lines = reader.lines().peek(line -> {
                        if (line != null && line.length() > MAX_LINE_LENGTH) {
                            throw new com.jikenlabs.openneodes.exception.InvalidDsnFormatException(
                                    "Line too long (max " + MAX_LINE_LENGTH + ")");
                        }
                    });
                    DsnDocument doc = parser.parse(lines::iterator);
                    future.complete(doc);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }

        return future;
    }
}
