package com.jpm;

import java.util.Objects;

/**
 * Immutable model for a Maven dependency.
 */
public final class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = requireText(groupId, "groupId");
        this.artifactId = requireText(artifactId, "artifactId");
        this.version = requireText(version, "version");
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public boolean sameCoordinates(String otherGroupId, String otherArtifactId) {
        return groupId.equals(otherGroupId) && artifactId.equals(otherArtifactId);
    }

    public String coordinatesKey() {
        return groupId + ":" + artifactId;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}

