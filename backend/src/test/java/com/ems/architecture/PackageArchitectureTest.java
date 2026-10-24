package com.ems.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-level package/import guardrails; API integration tests verify runtime wiring. */
class PackageArchitectureTest {
    private static final Path SOURCE = Path.of("src/main/java");
    private static final Set<String> LAYERS = Set.of("controller", "service", "repository", "entity", "dto");
    private static final Map<String, Set<String>> ALLOWED = Map.of(
        "controller", Set.of("service", "dto", "entity"),
        "service", Set.of("service", "repository", "entity", "dto"),
        "repository", Set.of("repository", "entity"),
        "entity", Set.of("entity"),
        "dto", Set.of("dto", "entity"));

    private List<Path> sources() throws Exception {
        try (var paths = Files.walk(SOURCE.resolve("com/ems"))) {
            return paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    @Test void productionClassesLiveInFeatureLayers() throws Exception {
        var sources = sources();
        assertFalse(sources.isEmpty(), "Production sources must be available to this guardrail");
        for (var file : sources) {
            var match = Pattern.compile("(?m)^package ([\\w.]+);").matcher(Files.readString(file));
            assertTrue(match.find(), file + " needs a package declaration");
            String pkg = match.group(1);
            assertEquals(SOURCE.resolve(pkg.replace('.', '/')).resolve(file.getFileName()), file);
            if (pkg.equals("com.ems") && file.getFileName().toString().equals("EmployeeManagementApplication.java")) continue;
            if (pkg.startsWith("com.ems.common.")) continue;
            String[] parts = pkg.split("\\.");
            assertTrue(parts.length >= 4 && LAYERS.contains(parts[3]), file + " must belong to a feature/layer");
            String name = file.getFileName().toString();
            for (String suffix : List.of("Controller", "Service", "Repository")) {
                if (name.endsWith(suffix + ".java")) assertEquals(suffix.toLowerCase(), parts[3], file.toString());
            }
        }
    }

    @Test void projectReferencesRespectLayerDirections() throws Exception {
        // Match fully-qualified project references too, not only import statements.
        Pattern references = Pattern.compile("com\\.ems\\.([a-z][a-z0-9]*)\\.([a-z][a-z0-9]*)\\.[\\w.*]+");
        for (var file : sources()) {
            String source = Files.readString(file);
            var pkg = Pattern.compile("(?m)^package com\\.ems\\.[a-z][a-z0-9]*\\.([a-z][a-z0-9]*);").matcher(source);
            if (!pkg.find() || !ALLOWED.containsKey(pkg.group(1))) continue;
            String layer = pkg.group(1);
            var referencesInSource = references.matcher(source.replaceFirst("(?m)^package [^;]+;", ""));
            while (referencesInSource.find()) {
                String feature = referencesInSource.group(1), target = referencesInSource.group(2);
                assertFalse(referencesInSource.group().endsWith(".*"), file + " must use explicit project imports");
                if (feature.equals("common")) continue;
                assertTrue(ALLOWED.get(layer).contains(target), file + " cannot depend on " + referencesInSource.group());
            }
            if (Set.of("entity", "repository", "dto").contains(layer)) {
                assertFalse(source.contains("org.springframework.web."), file + " must not depend on HTTP infrastructure");
            }
        }
    }
}
