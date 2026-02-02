package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.core.DsnToken;
import com.jikenlabs.openneodes.engine.DsnValueMapper;
import com.jikenlabs.openneodes.exception.DefinitionNotFoundException;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnLotEvent;
import com.jikenlabs.openneodes.model.DsnLotEvent.*;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Parseur pour les lots de déclarations DSN (S05...S95).
 * Supporte le mode streaming pour une efficacité mémoire optimale.
 */
public class DsnLotParser {

    /**
     * Constructeur par défaut.
     */
    public DsnLotParser() {
    }

    private final DsnLineParser lineParser = new DsnLineParser();
    private final DsnValueMapper mapper = new DsnValueMapper();
    private DsnNormRegistry lotRegistry;

    /**
     * Parse un flux de lot DSN en streaming, émettant des événements au listener.
     * 
     * @param is       Le flux d'entrée.
     * @param listener Le listener pour les événements.
     */
    public void parseLotStreaming(InputStream is, DsnLotListener listener) {
        streamLot(is).forEach(event -> {
            if (event instanceof LotHeaderEvent e)
                listener.onLotHeader(e.header());
            else if (event instanceof GroupHeaderEvent e)
                listener.onGroupHeader(e.header());
            else if (event instanceof DeclarationEvent e) {
                listener.onDeclaration(e.lines());
            } else if (event instanceof GroupFooterEvent e)
                listener.onGroupFooter(e.footer());
            else if (event instanceof LotFooterEvent e)
                listener.onLotFooter(e.footer());
            else if (event instanceof ErrorEvent e)
                listener.onError(e.error());
        });
        listener.onComplete();
    }

    /**
     * Retourne un Stream d'événements pour le lot DSN.
     * Chaque déclaration est représentée par un sous-stream de lignes.
     * Cette méthode est hautement optimisée pour la mémoire (O(1) par ligne).
     *
     * @param is Le flux d'entrée.
     * @return Un Stream de DsnLotEvent.
     */
    public Stream<DsnLotEvent> streamLot(InputStream is) {
        BlockingQueue<DsnLotEvent> eventQueue = new LinkedBlockingQueue<>(50);

        Thread.ofVirtual().start(() -> {
            try {
                if (lotRegistry == null) {
                    lotRegistry = DsnNormRegistry.loadFromYamlAsync("/norm-V01R02.yaml").join();
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.ISO_8859_1))) {
                    DsnBlockInstance currentLotHeader = null;
                    DsnBlockInstance currentGroupHeader = null;
                    DsnBlockInstance currentGroupFooter = null;
                    DsnBlockInstance currentLotFooter = null;

                    String line = null;
                    int lineNumber = 0;
                    while (true) {
                        if (line == null) {
                            line = reader.readLine();
                        }
                        if (line == null)
                            break;

                        lineNumber++;
                        if (line.isBlank()) {
                            line = null;
                            continue;
                        }

                        DsnToken token = lineParser.parse(line);
                        String key = token.key();

                        if (key.startsWith("S05")) {
                            if (currentLotHeader == null) {
                                currentLotHeader = parseBlock(token, lotRegistry, lineNumber);
                            } else {
                                updateBlock(currentLotHeader, token, lotRegistry, lineNumber);
                            }
                            line = null; // Consume
                        } else if (key.startsWith("S06")) {
                            if (token.key().equals("S06.G51.00.001")) {
                                // Transition : on émet ce qu'on peut
                                if (currentLotHeader != null) {
                                    eventQueue.put(new LotHeaderEvent(currentLotHeader));
                                    currentLotHeader = null;
                                }
                                if (currentGroupHeader != null) {
                                    eventQueue.put(new GroupHeaderEvent(currentGroupHeader));
                                    currentGroupHeader = null;
                                }
                                if (currentGroupFooter != null) {
                                    eventQueue.put(new GroupFooterEvent(currentGroupFooter));
                                    currentGroupFooter = null;
                                }
                                currentGroupHeader = parseBlock(token, lotRegistry, lineNumber);
                            } else {
                                updateBlock(currentGroupHeader, token, lotRegistry, lineNumber);
                            }
                            line = null; // Consume
                        } else if (key.startsWith("S10")) {
                            if (currentGroupHeader != null) {
                                eventQueue.put(new GroupHeaderEvent(currentGroupHeader));
                                currentGroupHeader = null;
                            }

                            BlockingQueue<String> lineQueue = new SynchronousQueue<>();
                            eventQueue.put(new DeclarationEvent(createLineStream(lineQueue)));

                            lineQueue.put(line); // On met le S10
                            boolean s90Seen = false;
                            while ((line = reader.readLine()) != null) {
                                lineNumber++;
                                if (line.isBlank())
                                    continue;

                                DsnToken nextToken = lineParser.parse(line);
                                String nextKey = nextToken.key();

                                if (nextKey.startsWith("S90")) {
                                    s90Seen = true;
                                }

                                // Si on tombe sur un nouveau bloc majeur, on arrête la déclaration
                                // On ne coupe que si on a déjà vu le pied (S90) ou si c'est un bloc
                                // hors-décloration
                                if (nextKey.startsWith("S05") || nextKey.startsWith("S06") ||
                                        nextKey.startsWith("S95") || nextKey.startsWith("S96") ||
                                        (s90Seen && nextKey.startsWith("S10"))) {
                                    break;
                                }

                                lineQueue.put(line);
                            }
                            lineQueue.put("__END_OF_DECLARATION__");
                            // Ici 'line' peut être soit null (EOF) soit le début du bloc suivant
                        } else if (key.startsWith("S96")) {
                            if (currentGroupHeader != null) {
                                eventQueue.put(new GroupHeaderEvent(currentGroupHeader));
                                currentGroupHeader = null;
                            }
                            if (currentGroupFooter == null) {
                                currentGroupFooter = parseBlock(token, lotRegistry, lineNumber);
                            } else {
                                updateBlock(currentGroupFooter, token, lotRegistry, lineNumber);
                            }
                            line = null; // Consume
                        } else if (key.startsWith("S95")) {
                            if (currentGroupFooter != null) {
                                eventQueue.put(new GroupFooterEvent(currentGroupFooter));
                                currentGroupFooter = null;
                            }
                            if (currentLotFooter == null) {
                                currentLotFooter = parseBlock(token, lotRegistry, lineNumber);
                            } else {
                                updateBlock(currentLotFooter, token, lotRegistry, lineNumber);
                            }
                            line = null; // Consume
                        } else {
                            // Rubrique inconnue ou ignorée au niveau lot
                            line = null; // Consume pour éviter boucle infinie
                        }
                    }
                    // Emission des restes
                    if (currentLotHeader != null)
                        eventQueue.put(new LotHeaderEvent(currentLotHeader));
                    if (currentGroupHeader != null)
                        eventQueue.put(new GroupHeaderEvent(currentGroupHeader));
                    if (currentGroupFooter != null)
                        eventQueue.put(new GroupFooterEvent(currentGroupFooter));
                    if (currentLotFooter != null)
                        eventQueue.put(new LotFooterEvent(currentLotFooter));

                }
            } catch (Throwable e) {
                try {
                    eventQueue.put(new ErrorEvent(e));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                try {
                    eventQueue.put(new ErrorEvent(null));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<DsnLotEvent>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(java.util.function.Consumer<? super DsnLotEvent> action) {
                try {
                    DsnLotEvent event = eventQueue.take();
                    if (event instanceof ErrorEvent err && err.error() == null)
                        return false;
                    action.accept(event);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }, false);
    }

    private Stream<String> createLineStream(BlockingQueue<String> lineQueue) {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<String>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(java.util.function.Consumer<? super String> action) {
                try {
                    String line = lineQueue.take();
                    if ("__END_OF_DECLARATION__".equals(line))
                        return false;
                    action.accept(line);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }, false);
    }

    private DsnBlockInstance parseBlock(DsnToken token, DsnNormRegistry registry, int lineNumber) {
        String blockKey = getBlockKey(token.key());
        DsnBlockInstance instance = new DsnBlockInstance(blockKey, registry.getBlock(blockKey));
        updateBlock(instance, token, registry, lineNumber);
        return instance;
    }

    private void updateBlock(DsnBlockInstance instance, DsnToken token, DsnNormRegistry registry, int lineNumber) {
        if (instance == null)
            return;
        try {
            RubriqueDefinition def = registry.getDefinition(token.key());
            Object value = mapper.map(token.value(), def, lineNumber, token.key());
            instance.addValue(token.key(), value);
        } catch (DefinitionNotFoundException e) {
            // Pour le niveau lot, on tolère des rubriques inconnues (on les stocke brut)
            instance.addValue(token.key(), token.value());
        }
    }

    private String getBlockKey(String rubriqueKey) {
        int lastDot = rubriqueKey.lastIndexOf('.');
        return lastDot == -1 ? rubriqueKey : rubriqueKey.substring(0, lastDot);
    }
}
