package com.github.pfumagalli.demavenizer.maven;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.pfumagalli.demavenizer.Log;
import com.github.pfumagalli.demavenizer.parser.Node;
import com.github.pfumagalli.demavenizer.parser.Parser;

public class Repository {

    private final URI base;
    private final Map<Identifier, Project> cache = new HashMap<>();
    private final Map<Identifier, String> versions = new HashMap<>();

    public Repository(URI base) {
        assert (base != null): "Base URI is null";

        final String path = base.getPath();
        if (path.endsWith("/")) {
            this.base= base;
        } else try {
            this.base = new URI(base.getScheme(),
                                base.getUserInfo(),
                                base.getHost(),
                                base.getPort(),
                                path + "/",
                                base.getQuery(),
                                base.getFragment());
        } catch (final URISyntaxException exception) {
            throw new IllegalArgumentException("Exception normalizing URI " + base.toString());
        }
    }

    public Project getProject(Identifier identifier) {
        if (identifier instanceof Project) return (Project) identifier;
        Project project = cache.get(identifier);
        if (project != null) return project;

        /* Check the version */
        String version = identifier.getVersion();

        /* Check the cache */
        if (version == null) version = versions.get(identifier);

        /* Parse metadata */
        if (version == null) {
            final String path = identifier.getGroupId().replace('.', '/') +
                          '/' + identifier.getArtifactId() +
                          '/' + "maven-metadata.xml";
            final Node metadata = Parser.parse(base.resolve(path));
            if (!"metadata".equals(metadata.getName()))
                throw new IllegalArgumentException("Found <" + metadata.getName() + "/> node");
            for (final Node versioning: metadata.getChildren("versioning")) {
                for (final Node release: versioning.getChildren("release")) {
                    version = release.getText(Collections.emptyMap(), null);
                    versions.put(identifier, version);
                }
            }

            /* Still no version? Well, get the last one of it */
            if (version == null) {
                for (final Node versioning: metadata.getChildren("versioning")) {
                    for (final Node versions: versioning.getChildren("versions")) {
                        for (final Node versionNode: versions.getChildren("version")) {
                            version = versionNode.getText(Collections.emptyMap(), null);
                        }
                    }
                }
            }
        }

        /* Still nothing? */
        if (version == null)
            throw new IllegalStateException("Unable to get version for " + identifier);

        /* Parse our module */
        final String path = identifier.getGroupId().replace('.', '/') +
                      '/' + identifier.getArtifactId() +
                      '/' + version +
                      '/' + identifier.getArtifactId() +
                      '-' + version + ".pom";
        identifier = new Identifier(identifier.getGroupId(), identifier.getArtifactId(), version);
        final URI uri = base.resolve(path);
        Log.info("Parsing " + uri.toString());
        project = new Project(this, identifier, Parser.parse(uri));
        cache.put(identifier, project);
        return project;
    }

}
