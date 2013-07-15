package com.github.pfumagalli.demavenizer.ivy;

public final class Revision implements Comparable<Revision> {

    private final int major;
    private final int minor;
    private final int build;
    private final String mark;

    public Revision(String revision) {
        final int dot1 = revision.indexOf('.');
        final int dot2 = revision.indexOf('.', dot1 + 1);

        /* Figure out the marker */
        final int dash = revision.indexOf('-');
        if (dash < 0) {
            mark = null;
        } else {
            final String marker = revision.substring(dash + 1);
            mark = marker.isEmpty() ? null : marker.toLowerCase();
            revision = revision.substring(0, dash);
        }

        /* Figure out the main version numbers */
        if (dot1 < 0) throw new IllegalArgumentException("Invalid identifier \"" + revision + "\"");
        major = Integer.parseInt(revision.substring(0, dot1).trim());
        if (dot2 < 0) {
            minor = Integer.parseInt(revision.substring(dot1 + 1).trim());
            build = -1;
        } else {
            minor = Integer.parseInt(revision.substring(dot1 + 1, dot2).trim());
            build = Integer.parseInt(revision.substring(dot2 + 1).trim());
        }
    }

    @Override
    public int compareTo(Revision revision) {
        if (revision == null) throw new NullPointerException();

        final int major = Integer.compare(this.major, revision.major);
        if (major != 0) return major;
        final int minor = Integer.compare(this.minor, revision.minor);
        if (minor != 0) return minor;
        final int build = Integer.compare(this.build, revision.build);
        if (build != 0) return build;

        /* The null mark indicates no funky revisions (beta, release candidate, ...) */
        if (mark == null) {
            /* This "null" marked version is above any marked version */
            return revision.mark == null ? 0 : 1;
        } else {
            /* Two marked version simply get compared lexically (and pray) */
            return revision.mark == null ? -1 : mark.compareTo(revision.mark);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (object == null) return false;
        try {
            final Revision revision = (Revision) object;
            return revision.major == major &&
                   revision.minor == minor &&
                   revision.build == build;
        } catch (final ClassCastException exception) {
            return false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor);
        if (build >= 0) builder.append('.').append(build);
        if (mark != null) builder.append('-').append(mark);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
