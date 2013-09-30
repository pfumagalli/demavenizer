package com.github.pfumagalli.demavenizer.ivy;

import com.github.pfumagalli.demavenizer.maven.Identifier;

public class Marker implements Comparable<Marker> {

    private final Identifier identifier;
    private final String organisation;
    private final String module;
    private final Revision revision;

    public Marker(Identifier identifier, String organisation, String module, String revision) {
        this.identifier = identifier;
        this.organisation = organisation;
        this.module = module;
        this.revision = new Revision(revision);
        validate();
    }

    public Marker(Identifier identifier, String marker) {
        this.identifier = identifier;
        final int hash1 = marker.indexOf('#');
        final int hash2 = marker.indexOf('#', hash1 + 1);
        if (hash1 < 0) throw new IllegalArgumentException("Invalid marker \"" + marker + "\"");
        organisation = marker.substring(0, hash1).trim();
        if (hash2 < 0) {
            module = marker.substring(hash1 + 1).trim();
            revision = new Revision(identifier.getVersion());
        } else {
            module = marker.substring(hash1 + 1, hash2).trim();
            revision = new Revision(marker.substring(hash2 + 1).trim());
        }
        validate();
    }

    private void validate() {
        /* Check nulls */
        if (organisation == null) throw new NullPointerException("Null organisation in " + this);
        if (module == null) throw new NullPointerException("Null module in " + this);
        if (revision == null) throw new NullPointerException("Null revision in " + this);

        /* Check empties */
        if ("".equals(organisation)) throw new IllegalArgumentException("Empty organisation in " + this);
        if ("".equals(module)) throw new IllegalArgumentException("Empty module in " + this);
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getModule() {
        return module;
    }

    public Revision getRevision() {
        return revision;
    }

    public String asString() {
        final StringBuilder builder = new StringBuilder(organisation).append('#')
                                                .append(module).append('#')
                                                .append(revision);
        return builder.toString();
    }

    /* ====================================================================== */

    @Override
    public int compareTo(Marker marker) {
        if (marker == null) throw new NullPointerException();
        final int organisation = this.organisation.compareTo(marker.organisation);
        if (organisation != 0) return organisation;
        final int module = this.organisation.compareTo(marker.module);
        if (module != 0) return module;
        return revision.compareTo(revision);
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
            final Marker marker = (Marker) object;
            return marker.organisation.equals(organisation) &&
                   marker.getModule().equals(module) &&
                   (marker.getRevision() == null ? revision == null :
                       marker.getRevision().equals(revision));
        } catch (final ClassCastException exception) {
            return false;
        }
    }
}
