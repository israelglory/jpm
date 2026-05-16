package com.jpm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Console;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts user-friendly aliases into real Maven coordinates and supports free-text search.
 */
public final class DependencyResolver {

    private static final Map<String, Coordinates> ALIASES = Map.of(
            "postgres", new Coordinates("org.postgresql", "postgresql"),
            "lombok", new Coordinates("org.projectlombok", "lombok"),
            "web", new Coordinates("org.springframework.boot", "spring-boot-starter-web")
    );

    private final MavenCentralClient mavenCentralClient;

    public DependencyResolver(MavenCentralClient mavenCentralClient) {
        this.mavenCentralClient = mavenCentralClient;
    }

    /**
     * Resolves the alias and attaches the latest Maven Central version.
     */
    public Dependency resolve(String alias)
            throws UnknownDependencyAliasException, IOException, DependencyVersionNotFoundException {
        if (alias == null || alias.isBlank()) {
            throw new UnknownDependencyAliasException("Alias must not be blank");
        }

        String key = alias.trim().toLowerCase();
        Coordinates coordinates = ALIASES.get(key);
        if (coordinates != null) {
            Optional<String> version = mavenCentralClient.fetchLatestVersion(coordinates.groupId(), coordinates.artifactId());
            if (version.isEmpty()) {
                throw new DependencyVersionNotFoundException(
                        "Could not find a latest version for " + coordinates.groupId() + ":" + coordinates.artifactId());
            }
            return new Dependency(coordinates.groupId(), coordinates.artifactId(), version.get());
        }

        // Alias not known: perform a free-text search against Maven Central.
        List<MavenCentralClient.ArtifactInfo> results = mavenCentralClient.searchArtifacts(alias.trim(), 20);
        if (results.isEmpty()) {
            throw new UnknownDependencyAliasException("No artifacts found for: " + alias);
        }

        MavenCentralClient.ArtifactInfo chosen;
        if (results.size() == 1) {
            chosen = results.get(0);
        } else {
            chosen = promptUserToChoose(results, alias);
            if (chosen == null) {
                throw new UnknownDependencyAliasException("Selection cancelled");
            }
        }

        String version = chosen.latestVersion();
        if (version == null || version.isBlank()) {
            Optional<String> live = mavenCentralClient.fetchLatestVersion(chosen.groupId(), chosen.artifactId());
            if (live.isEmpty()) {
                throw new DependencyVersionNotFoundException(
                        "Could not find a latest version for " + chosen.groupId() + ":" + chosen.artifactId());
            }
            version = live.get();
        }

        return new Dependency(chosen.groupId(), chosen.artifactId(), version);
    }

    private MavenCentralClient.ArtifactInfo promptUserToChoose(List<MavenCentralClient.ArtifactInfo> results, String query) throws IOException {
        System.out.println("Multiple artifacts found for '" + query + "'. Please pick one:");
        for (int i = 0; i < results.size(); i++) {
            MavenCentralClient.ArtifactInfo r = results.get(i);
            System.out.printf("  %d) %s:%s (latest: %s)%n", i + 1, r.groupId(), r.artifactId(), r.latestVersion());
        }

        Console console = System.console();
        BufferedReader reader = null;
        if (console == null) {
            reader = new BufferedReader(new InputStreamReader(System.in));
        }

        while (true) {
            String line;
            if (console != null) {
                line = console.readLine("Enter number (1-%d) or 'q' to cancel: ", results.size());
            } else {
                System.out.print("Enter number (1-" + results.size() + ") or 'q' to cancel: ");
                line = reader.readLine();
            }

            if (line == null) {
                return null;
            }
            line = line.trim();
            if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("c")) {
                return null;
            }
            try {
                int idx = Integer.parseInt(line);
                if (idx >= 1 && idx <= results.size()) {
                    return results.get(idx - 1);
                }
            } catch (NumberFormatException ignored) {
                // continue loop
            }
            System.out.println("Invalid selection, please try again.");
        }
    }

    private record Coordinates(String groupId, String artifactId) {
    }

    public static final class UnknownDependencyAliasException extends Exception {
        public UnknownDependencyAliasException(String message) {
            super(message);
        }
    }

    public static final class DependencyVersionNotFoundException extends Exception {
        public DependencyVersionNotFoundException(String message) {
            super(message);
        }
    }
}

