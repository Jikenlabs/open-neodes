package com.jikenlabs.openneodes.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnLotEvent;
import com.jikenlabs.openneodes.model.DsnLotEvent.*;
import com.jikenlabs.openneodes.facade.DsnLotParser;
import com.jikenlabs.openneodes.facade.DsnParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * Démo pour parser un lot DSN et afficher chaque déclaration au format JSON
 * avec des noms lisibles (Humains) pour les blocs et les rubriques.
 */
public class DsnLotJsonDemo {

    public static void main(String[] args) throws Exception {
        String defaultPath = "normes/DSN-ORGANISME/DSN03_20251224-071503.REC";
        String path = (args.length > 0) ? args[0] : defaultPath;

        File file = new File(path);
        if (!file.exists()) {
            System.err.println("!!! Fichier non trouvé : " + path);
            return;
        }

        System.out.println("Processing file: " + file.getName());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        DsnLotParser lotParser = new DsnLotParser();

        try (FileInputStream fis = new FileInputStream(file)) {
            Stream<DsnLotEvent> eventStream = lotParser.streamLot(fis);

            eventStream.forEach(event -> {
                try {
                    if (event instanceof LotHeaderEvent lotHeader) {
                        System.out.println("=== LOT HEADER ===");
                        System.out.println(mapper.writeValueAsString(lotHeader.header().toMap()));
                    } else if (event instanceof DeclarationEvent decl) {
                        System.out.println("\n=== DECLARATION ===");
                        List<String> lines = decl.lines().toList();

                        // Parsing de la déclaration
                        DsnDocument doc = DsnParser.parseAutoDetectAsync(lines).join();

                        // Test de la fonction query (AGNIOSTIQUE À LA VERSION ET PROFONDEUR)
                        System.out.println("    [QUERY TEST] Siret (global deep): " + doc.query("Siret").toValue());
                        System.out.println("    [QUERY TEST] Emetteur Siren: " + doc.query("Emetteur.Siren").toValue());
                        System.out.println(
                                "    [QUERY TEST] Entreprise Siren: " + doc.query("Entreprise.Siren").toValue());
                        System.out
                                .println("    [QUERY TEST] Siren (case-insensitive): " + doc.query("siren").toValue());

                        System.out.println("    [QUERY TEST] Individu: "
                                + doc.query("Individu").asBlock().map(DsnBlockInstance::toMap).orElse(null));

                        // Affichage JSON structuré via la lib (doc.toMap())
                        System.out.println(mapper.writeValueAsString(doc.toMap()));
                    } else if (event instanceof LotFooterEvent lotFooter) {
                        System.out.println("\n=== LOT FOOTER ===");
                        System.out.println(mapper.writeValueAsString(lotFooter.footer().toMap()));
                    } else if (event instanceof ErrorEvent error) {
                        System.err.println("Error event: " + error.error().getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing event: " + e.getMessage());
                }
            });
        }
    }
}
