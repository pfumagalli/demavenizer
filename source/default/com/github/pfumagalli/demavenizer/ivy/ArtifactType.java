package com.github.pfumagalli.demavenizer.ivy;

import com.github.pfumagalli.demavenizer.Configuration;


public enum ArtifactType {

    BIN(".jar"), SRC(".zip"), DOC(".zip");

    private final String extension;
    private final String type;

    private ArtifactType(String extension) {
        this.extension = extension;
        type = name().toLowerCase();
    }

    public String getType(Configuration configuration) {
        return configuration.get("artifacts.type." + type + ".type");
    }

    public String getExtension(Configuration configuration) {
        return configuration.get("artifacts.type." + type + ".extension");
    }

    public String getArtifactFile(Configuration configuration, Marker marker) {
        return configuration.getResolved("artifacts.type." + type + ".pattern", marker.asMap());
    }
}
