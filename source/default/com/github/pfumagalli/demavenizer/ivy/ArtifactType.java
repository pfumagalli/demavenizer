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
        final String configured = configuration.get("artifacts.type." + type + ".type");
        return configured == null ? type : configured.trim();
    }

    public String getExtension(Configuration configuration) {
        final String configured = configuration.get("artifacts.type." + type + ".extension");
        return configured == null ? extension : configured.trim();
    }

}
