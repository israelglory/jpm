package com.jpm;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small HTTP client for Maven Central.
 */
public final class MavenCentralClient {

    private static final String SEARCH_URL =
            "https://search.maven.org/solrsearch/select?q=%s&rows=1&wt=json";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern LATEST_VERSION_PATTERN =
            Pattern.compile("\"latestVersion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NUM_FOUND_PATTERN =
            Pattern.compile("\"numFound\"\\s*:\\s*(\\d+)");
    private static final Pattern DOC_ENTRY_PATTERN = Pattern.compile(
            "\\{[^}]*\\\"g\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^}]*\\\"a\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^}]*\\\"latestVersion\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^}]*\\}",
            Pattern.DOTALL);

    private final HttpClient httpClient;

    public MavenCentralClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Returns the newest version Maven Central knows about.
     */
    public Optional<String> fetchLatestVersion(String groupId, String artifactId) throws IOException {
        URI uri = buildSearchUri(groupId, artifactId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() != 200) {
                    if (attempts >= 3) {
                        throw new IOException("Maven Central returned HTTP " + response.statusCode());
                    } else {
                        Thread.sleep(250L * attempts);
                        continue;
                    }
                }

                String body = response.body();
                if (body == null || body.isBlank()) {
                    return Optional.empty();
                }

                int numFound = extractNumFound(body);
                if (numFound == 0) {
                    return Optional.empty();
                }

                return extractLatestVersion(body);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("HTTP request was interrupted", ex);
            } catch (IOException ex) {
                if (attempts >= 3) {
                    throw ex;
                }
                try {
                    Thread.sleep(250L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
            }
        }
    }

    private URI buildSearchUri(String groupId, String artifactId) {
        String query = "g:" + groupId + " AND a:" + artifactId;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return URI.create(String.format(SEARCH_URL, encodedQuery));
    }

    private Optional<String> extractLatestVersion(String json) {
        Matcher matcher = LATEST_VERSION_PATTERN.matcher(json);
        if (matcher.find()) {
            String version = matcher.group(1).trim();
            if (!version.isEmpty()) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    private int extractNumFound(String json) {
        Matcher matcher = NUM_FOUND_PATTERN.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /**
     * Performs a general search on Maven Central and returns up to `rows` artifact
     * results.
     */
    public List<ArtifactInfo> searchArtifacts(String query, int rows) throws IOException {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("https://search.maven.org/solrsearch/select?q=%s&rows=%d&wt=json", encoded, rows);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Maven Central returned HTTP " + response.statusCode());
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return List.of();
            }

            List<ArtifactInfo> results = new ArrayList<>();
            Matcher m = DOC_ENTRY_PATTERN.matcher(body);
            while (m.find()) {
                String g = m.group(1).trim();
                String a = m.group(2).trim();
                String v = m.group(3).trim();
                results.add(new ArtifactInfo(g, a, v));
            }
            return results;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request was interrupted", ex);
        }
    }

    /**
     * Lightweight DTO for search results.
     */
    public static final record ArtifactInfo(String groupId, String artifactId, String latestVersion) {
    }
}

