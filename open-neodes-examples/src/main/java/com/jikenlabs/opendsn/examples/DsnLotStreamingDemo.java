package com.jikenlabs.openneodes.examples;

import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnLotEvent;
import com.jikenlabs.openneodes.model.DsnLotEvent.*;
import com.jikenlabs.openneodes.facade.DsnLotParser;
import com.jikenlabs.openneodes.facade.DsnParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Classe de démonstration pour tester le streaming du DsnLotParser sur un
 * fichier réel ou un dossier complet avec mesures de performance et extraction
 * de données métier via le DsnParser (parsing structurel).
 */
public class DsnLotStreamingDemo {

    public static void main(String[] args) throws Exception {
        String defaultPath = "normes/DSN-ORGANISME/DSN03_20251224-071503.REC";
        String path = (args.length > 0) ? args[0] : defaultPath;

        File target = new File(path);
        if (!target.exists()) {
            System.err.println("!!! Chemin non trouvé : " + path);
            System.out.println(
                    "Usage : mvn exec:java -Dexec.mainClass=\"...\" -Dexec.args=\"/chemin/vers/fichier_ou_dossier\"");
            return;
        }

        List<File> filesToProcess = new ArrayList<>();
        if (target.isDirectory()) {
            File[] list = target.listFiles(f -> f.isFile());
            if (list != null) {
                for (File f : list) {
                    filesToProcess.add(f);
                }
            }
            System.out.println(
                    ">>> Traitement du dossier : " + path + " (" + filesToProcess.size() + " fichiers trouvés)");
        } else {
            filesToProcess.add(target);
        }

        // Mesures initiales
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        DsnLotParser parser = new DsnLotParser();
        AtomicInteger totalDeclarations = new AtomicInteger(0);

        filesToProcess.parallelStream().forEach(file -> {
            processFile(file, parser, totalDeclarations);
        });

        long endTime = System.currentTimeMillis();
        System.gc();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("RÉSULTATS DE PERFORMANCE GLOBALE");
        System.out.println("=".repeat(70));
        System.out.println("Fichiers traités    : " + filesToProcess.size());
        System.out.println("Temps d'exécution   : " + (endTime - startTime) + " ms");
        System.out.println("Mémoire initiale    : " + (memBefore / 1024) + " KB");
        System.out.println("Mémoire finale      : " + (memAfter / 1024) + " KB");
        System.out.println("Différence (Heap)   : " + ((memAfter - memBefore) / 1024) + " KB");
        System.out.println("Total Déclarations  : " + totalDeclarations.get());
        System.out.println("=".repeat(70));
    }

    private static void processFile(File file, DsnLotParser parser, AtomicInteger declarationCount) {
        String filePath = file.getAbsolutePath();
        System.out.println("\n>>> Parsing du fichier : " + file.getName());

        try (InputStream is = new FileInputStream(filePath)) {
            Stream<DsnLotEvent> lotStream = parser.streamLot(is);

            lotStream.forEach(event -> {
                if (event instanceof LotHeaderEvent lotHeader) {
                    System.out.println("[LOT HEADER] Version: " + lotHeader.header().getValue("VersionNorme")
                            + " | Numéro: " + lotHeader.header().getValue("NumeroLot"));
                } else if (event instanceof GroupHeaderEvent groupHeader) {
                    System.out.println("  [GROUP HEADER] Code: " + groupHeader.header().getValue("CodeGroupe"));
                } else if (event instanceof DeclarationEvent decl) {
                    int id = declarationCount.incrementAndGet();

                    try {
                        // On transforme le flux de lignes en liste pour le DsnParser
                        List<String> lines = decl.lines().toList();

                        // Parsing structurel de la déclaration seule
                        DsnDocument doc = DsnParser.parseAutoDetectAsync(lines).join();

                        String siren = "Inconnu";
                        String raisonSociale = "Inconnu";
                        String concentrateurUser = "N/A";

                        // 1. Recherche du Concentrateur (S10.G00.95)
                        List<DsnBlockInstance> concentrateurs = new ArrayList<>();
                        for (DsnBlockInstance root : doc.getRootBlocks()) {
                            concentrateurs.addAll(findBlocks(root, "S10.G00.95"));
                        }
                        if (!concentrateurs.isEmpty()) {
                            DsnBlockInstance conc = concentrateurs.get(0);
                            concentrateurUser = conc.getValue("prenomUtilisateur") + " "
                                    + conc.getValue("nomUtilisateur");
                            // On peut aussi récupérer la raison sociale du concentrateur si besoin
                            // String siret = (String) conc.getValue("siret");
                            // raisonSociale = (String) conc.getValue("raisonSociale");
                        }

                        // 1b. Recherche des métadonnées techniques déclaration (S20.G00.96)
                        List<DsnBlockInstance> techMetas = new ArrayList<>();
                        for (DsnBlockInstance root : doc.getRootBlocks()) {
                            techMetas.addAll(findBlocks(root, "S20.G00.96"));
                        }
                        String idTech = "N/A";
                        if (!techMetas.isEmpty()) {
                            idTech = (String) techMetas.get(0).getValue("idTechniqueDeclaration");
                        }

                        // 2. Recherche de l'Entreprise (S21.G00.06)
                        List<DsnBlockInstance> entreprises = new ArrayList<>();
                        for (DsnBlockInstance root : doc.getRootBlocks()) {
                            entreprises.addAll(findBlocks(root, "S21.G00.06"));
                        }

                        if (!entreprises.isEmpty()) {
                            DsnBlockInstance ent = entreprises.get(0);
                            siren = (String) ent.getValue("Siren");
                            raisonSociale = (String) ent.getValue("NomExtension");
                            if (raisonSociale == null || raisonSociale.isBlank()) {
                                raisonSociale = (String) ent.getValue("Nom");
                            }
                        }

                        // 3. Fallback sur S10.G00.01 (Emetteur)
                        if (raisonSociale == null || raisonSociale.isBlank() || "Inconnu".equals(raisonSociale)) {
                            List<DsnBlockInstance> emetteurs = new ArrayList<>();
                            for (DsnBlockInstance root : doc.getRootBlocks()) {
                                emetteurs.addAll(findBlocks(root, "S10.G00.01"));
                            }
                            if (!emetteurs.isEmpty()) {
                                if ("Inconnu".equals(siren))
                                    siren = (String) emetteurs.get(0).getValue("Siren");
                                if ("Inconnu".equals(raisonSociale))
                                    raisonSociale = (String) emetteurs.get(0).getValue("Nom");
                            }
                        }

                        System.out.println("    Déclaration #" + id + " : [SIREN: " + siren + "] [ENTREPRISE: "
                                + raisonSociale + "] (" + lines.size() + " lignes) [TECH: " + concentrateurUser
                                + " | ID: " + idTech + "]");

                    } catch (Exception e) {
                        System.err.println(
                                "    [ERROR] Erreur lors du parsing de la déclaration dans " + file.getName() + ": "
                                        + e.getMessage());
                    }
                } else if (event instanceof ErrorEvent error) {
                    if (error.error() != null) {
                        System.err.println("!!! ERROR dans " + file.getName() + ": " + error.error().getMessage());
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("!!! Impossible de lire le fichier " + file.getName() + " : " + e.getMessage());
        }
    }

    private static List<DsnBlockInstance> findBlocks(DsnBlockInstance parent, String key) {
        List<DsnBlockInstance> found = new ArrayList<>();
        if (key.equals(parent.getKey())) {
            found.add(parent);
        }
        for (List<DsnBlockInstance> childList : parent.getChildren().values()) {
            for (DsnBlockInstance child : childList) {
                found.addAll(findBlocks(child, key));
            }
        }
        return found;
    }
}
