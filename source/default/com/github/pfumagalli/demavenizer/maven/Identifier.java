package com.github.pfumagalli.demavenizer.maven;

import java.util.Map;

import com.github.pfumagalli.demavenizer.parser.Node;

public class Identifier implements Comparable<Identifier> {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public Identifier(Identifier identifier, Map<?, ?> context, Node node) {
        groupId = node.getChildText("groupId",  context,  identifier != null ? identifier.getGroupId() : null);
        artifactId = node.getChildText("artifactId",  context,  identifier != null ? identifier.getArtifactId() : null);
        version = node.getChildText("version",  context,  identifier != null ? identifier.getVersion() : null);;
        validate();
    }

    public Identifier(String groupId, String artifactId, String version) {
        this.groupId = groupId == null ? null : groupId.trim();
        this.artifactId = artifactId == null ? null : artifactId.trim();
        this.version = version == null ? null : version.trim();
        validate();
    }

    public Identifier(String identifier) {
        final int hash1 = identifier.indexOf('#');
        final int hash2 = identifier.indexOf('#', hash1 + 1);
        if (hash1 < 0) throw new IllegalArgumentException("Invalid identifier \"" + identifier + "\"");
        groupId = identifier.substring(0, hash1).trim();
        if (hash2 < 0) {
            artifactId = identifier.substring(hash1 + 1).trim();
            version = null;
        } else {
            artifactId = identifier.substring(hash1 + 1, hash2).trim();
            version = identifier.substring(hash2 + 1).trim();
        }
        validate();
    }

    private void validate() {

        /* Check nulls */
        if (groupId == null) throw new NullPointerException("Null group ID in " + this);
        if (artifactId == null) throw new NullPointerException("Null artifact ID in " + this);

        /* Check empties */
        if ("".equals(groupId)) throw new IllegalArgumentException("Empty group ID in " + this);
        if ("".equals(artifactId)) throw new IllegalArgumentException("Empty artifact ID in " + this);
        if ("".equals(version)) throw new IllegalArgumentException("Empty version in " + this);

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

    public String asString() {
        final StringBuilder builder = new StringBuilder(groupId).append('#').append(artifactId);
        if (version != null) builder.append('#').append(version);
        return builder.toString();
    }

    /* ====================================================================== */

    @Override
    public int compareTo(Identifier identifier) {
        if (identifier == null) throw new NullPointerException();
        final int organisation = groupId.compareTo(identifier.groupId);
        if (organisation != 0) return organisation;
        final int module = artifactId.compareTo(identifier.artifactId);
        if (module != 0) return module;
        if (version == null) return identifier.version == null ? 0 : -1;
        if (identifier.version == null) return 1;
        return version.compareTo(identifier.version);
    }

    @Override
    public int hashCode() {
        return asString().hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + asString() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        try {
            final Identifier identifier = (Identifier) object;
            return identifier.groupId.equals(groupId) &&
                   identifier.getArtifactId().equals(artifactId) &&
                   (identifier.getVersion() == null ? version == null :
                       identifier.getVersion().equals(version));
        } catch (final ClassCastException exception) {
            return false;
        }
    }
}
