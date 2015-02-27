package com.github.pfumagalli.demavenizer.ivy;

import com.github.pfumagalli.demavenizer.maven.Dependency;

public class DependenciesException extends RuntimeException {

    private final Iterable<Dependency> missing;

    public DependenciesException(Marker marker, Iterable<Dependency> missing) {
        super("Unresolved dependencies for " + marker.asString());
        this.missing = missing;
    }

    public Iterable<Dependency> getMissingDependencies() {
        return missing;
    }
}
