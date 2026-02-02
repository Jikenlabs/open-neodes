package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.engine.DsnBlockListener;
import com.jikenlabs.openneodes.engine.DsnHierarchicalParser;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnEnvelope;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Façade pour les opérations de parsing DSN.
 */
public class DsnParser {

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    private DsnParser() {
        // Façade statique
    }

    /**
     * Parse un flux DSN de manière asynchrone.
     *
     * @param is       Le flux d'entrée contenant les données DSN (ISO-8859-1).
     * @param registry Le registre de normes pour valider et structurer les données.
     * @return Un CompletableFuture contenant le document DSN parsé.
     */
    public static CompletableFuture<DsnDocument> parseAsync(InputStream is, DsnNormRegistry registry) {
        return parseAsync(is, registry, List.of(), false);
    }

    /**
     * Parse un flux DSN de manière asynchrone avec des options avancées.
     *
     * @param is        Le flux d'entrée contenant les données DSN (ISO-8859-1).
     * @param registry  Le registre de normes pour valider et structurer les
     *                  données.
     * @param listeners Liste des écouteurs pour le traitement streaming des blocs.
     * @param detach    Si vrai, les objets enfants sont détachés du parent après
     *                  notification (mode streaming pure).
     * @return Un CompletableFuture contenant le document DSN parsé (potentiellement
     *         vide si detach=true).
     */
    public static CompletableFuture<DsnDocument> parseAsync(InputStream is, DsnNormRegistry registry,
            List<DsnBlockListener> listeners, boolean detach) {
        return CompletableFuture.supplyAsync(() -> {
            DsnHierarchicalParser parser = new DsnHierarchicalParser(new DsnLineParser(), registry, listeners, detach);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
            return parser.parse(reader.lines()::iterator);
        });
    }

    /**
     * AC 3: Parsing détaillé de l'enveloppe.
     *
     * @param is       Le flux d'entrée.
     * @param registry Le registre de normes.
     * @return Un CompletableFuture contenant l'enveloppe DSN parsée.
     */
    public static CompletableFuture<DsnEnvelope> parseEnvelopeAsync(InputStream is, DsnNormRegistry registry) {
        return parseEnvelopeAsync(is, registry, List.of(), false);
    }

    /**
     * Parse l'enveloppe DSN de manière asynchrone avec options avancées.
     *
     * @param is        Le flux d'entrée.
     * @param registry  Le registre de normes.
     * @param listeners Liste des écouteurs.
     * @param detach    Détacher les objets après notification.
     * @return Un CompletableFuture contenant l'enveloppe DSN parsée.
     */
    public static CompletableFuture<DsnEnvelope> parseEnvelopeAsync(InputStream is, DsnNormRegistry registry,
            List<DsnBlockListener> listeners, boolean detach) {
        return CompletableFuture.supplyAsync(() -> {
            DsnHierarchicalParser parser = new DsnHierarchicalParser(new DsnLineParser(), registry, listeners, detach);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
            return parser.parseEnvelope(reader.lines()::iterator);
        });
    }

    /**
     * Parse un flux de lignes DSN avec détection automatique de la version.
     *
     * @param lines Les lignes du fichier DSN.
     * @return Un CompletableFuture sur le document DSN.
     */
    public static CompletableFuture<DsnDocument> parseAutoDetectAsync(Iterable<String> lines) {
        return parseAutoDetectAsync(lines, List.of(), false);
    }

    /**
     * Parse un flux de lignes DSN avec détection automatique de la version.
     *
     * @param lines     Les lignes du fichier DSN.
     * @param listeners Liste des écouteurs de blocs.
     * @param detach    Si vrai, les blocs complétés sont détachés pour économiser
     *                  la
     *                  mémoire.
     * @return Un CompletableFuture sur le document DSN.
     */
    public static CompletableFuture<DsnDocument> parseAutoDetectAsync(Iterable<String> lines,
            List<DsnBlockListener> listeners,
            boolean detach) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = null;
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("^S10\\.G00\\.00\\.006,'([^']+)'");

                // Peek for version in the first few lines
                for (String line : lines) {
                    java.util.regex.Matcher m = p.matcher(line);
                    if (m.find()) {
                        version = m.group(1);
                        break;
                    }
                    if (line.startsWith("S20."))
                        break;
                }

                if (version == null) {
                    throw new java.io.IOException("Version DSN (S10.G00.00.006) introuvable dans les lignes fournies.");
                }

                version = sanitizeVersion(version);
                String normPath = "/norm-" + version + ".yaml";
                DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync(normPath).join();

                DsnHierarchicalParser parser = new DsnHierarchicalParser(new DsnLineParser(), registry, listeners,
                        detach);
                return parser.parse(lines);

            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    /**
     * Parse un flux DSN de manière asynchrone en détectant automatiquement la
     * version de la norme.
     * La méthode lit le début du flux pour trouver la rubrique S10.G00.00.006,
     * charge la norme appropriée, puis parse le contenu.
     *
     * @param is Le flux d'entrée. Il sera enveloppé dans un BufferedInputStream
     *           pour permettre le "peek".
     * @return Un CompletableFuture contenant le document DSN.
     */
    public static CompletableFuture<DsnDocument> parseAutoDetectAsync(InputStream is) {
        return parseAutoDetectAsync(is, List.of(), false);
    }

    /**
     * Parse un flux DSN de manière asynchrone avec détection auto de version et
     * options.
     *
     * @param is        Le flux d'entrée.
     * @param listeners Liste des écouteurs.
     * @param detach    Détacher les objets.
     * @return Un CompletableFuture contenant le document DSN.
     */
    public static CompletableFuture<DsnDocument> parseAutoDetectAsync(InputStream is, List<DsnBlockListener> listeners,
            boolean detach) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // S'assurer d'avoir un flux bufférisé qui supporte mark/reset
                java.io.BufferedInputStream bis = (is instanceof java.io.BufferedInputStream)
                        ? (java.io.BufferedInputStream) is
                        : new java.io.BufferedInputStream(is);

                // Détecter la version
                String version = sanitizeVersion(detectVersion(bis));
                String normPath = "/norm-" + version + ".yaml";

                // Charger le registre de manière synchrone (dans le supply async) ou bloquer
                // doucement
                DsnNormRegistry registry = DsnNormRegistry.loadFromYamlAsync(normPath).join();

                // Procéder au parsing avec le même bis (reset effectué dans detectVersion)
                DsnHierarchicalParser parser = new DsnHierarchicalParser(new DsnLineParser(), registry, listeners,
                        detach);
                BufferedReader reader = new BufferedReader(new InputStreamReader(bis, StandardCharsets.ISO_8859_1));
                return parser.parse(reader.lines()::iterator);

            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    private static String detectVersion(java.io.BufferedInputStream bis) throws java.io.IOException {
        // Limite de marquage : 4 Ko devraient suffire pour atteindre l'en-tête
        // S10.G00.00.006
        bis.mark(4096);
        try {
            // Ne PAS fermer ce reader, car cela fermerait le BIS sous-jacent
            BufferedReader peekReader = new BufferedReader(new InputStreamReader(bis, StandardCharsets.ISO_8859_1));
            String line;
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^S10\\.G00\\.00\\.006,'([^']+)'");

            while ((line = peekReader.readLine()) != null) {
                java.util.regex.Matcher m = p.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
                // Arrêt de sécurité si on va trop loin (par ex. dans S20)
                if (line.startsWith("S20.")) {
                    break;
                }
            }
        } finally {
            bis.reset();
        }
        throw new java.io.IOException("Version DSN (S10.G00.00.006) introuvable dans l'en-tête du fichier.");
    }

    private static String sanitizeVersion(String version) {
        if (version == null)
            return null;
        // La version ne doit contenir que des chiffres, lettres, points et tirets
        // et ne doit pas contenir de sequences de remontée de répertoire
        if (!version.matches("^[a-zA-Z0-9.\\-]+$")) {
            throw new IllegalArgumentException("Format de version DSN invalide : " + version);
        }
        return version;
    }
}
