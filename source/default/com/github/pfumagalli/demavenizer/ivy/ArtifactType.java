package com.github.pfumagalli.demavenizer.ivy;

import com.github.pfumagalli.demavenizer.Configuration;


public enum ArtifactType {

    BIN, SRC, DOC;

    private final String type;

    private ArtifactType() {
        type = name().toLowerCase();
    }

    public String getType(Configuration configuration) {
        return configuration.get("artifacts.type." + type + ".type").trim();
    }

    public String getExtension(Configuration configuration) {
        return configuration.get("artifacts.type." + type + ".extension").trim();
    }

    public String getArtifactFile(Configuration configuration, Marker marker) {
        return configuration.getResolved("artifacts.type." + type + ".pattern", marker.asMap());
    }
}
