package com.github.pfumagalli.demavenizer.maven;

import com.github.pfumagalli.demavenizer.parser.Node;

public class Dependency extends Identifier {

    private final Scope scope;
    private final boolean optional;

    public Dependency(Project project, Node node) {
        super(null, project.getProperties(), node);
        final String scope = node.getChildText("scope", project.getProperties(), "compile");
        this.scope = Scope.valueOf(scope.toUpperCase());
        optional = Boolean.parseBoolean(node.getChildText("optional", project.getProperties(), "false"));
    }

    public Scope getScope() {
        return scope;
    }

    public boolean isOptional() {
        return optional;
    }

}
