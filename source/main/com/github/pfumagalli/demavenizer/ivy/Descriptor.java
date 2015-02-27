package com.github.pfumagalli.demavenizer.ivy;

import static com.github.pfumagalli.demavenizer.ivy.ArtifactType.BIN;
import static com.github.pfumagalli.demavenizer.ivy.ArtifactType.DOC;
import static com.github.pfumagalli.demavenizer.ivy.ArtifactType.SRC;

import java.net.HttpURLConnection;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.pfumagalli.demavenizer.License;
import com.github.pfumagalli.demavenizer.Log;
import com.github.pfumagalli.demavenizer.maven.Dependency;
import com.github.pfumagalli.demavenizer.maven.Project;

public class Descriptor extends Marker {

    private final Date publication;
    private final URI homePage;
    private final String description;
    private final String mavenId;
    private final Map<License, URI> licenses;
    private final Map<Marker, Dependency> dependencies = new TreeMap<>(); // sorted
    private final Map<Dependency, Marker> optionalDependencies = new TreeMap<>(); // sorted
    private final Map<ArtifactType, URI> artifacts = new EnumMap<>(ArtifactType.class);

    public Descriptor(Project project, Mapper mapper, Marker translated) {
        super(project,
              translated == null ? project.getGroupId() : translated.getOrganisation(),
              translated == null ? project.getArtifactId() : translated.getModule(),
              translated == null ? project.getVersion() : translated.getRevision().toString());
        publication = project.getLastModifiedDate();
        homePage = project.getURI();
        description = project.getName();
        licenses = project.getLicenses();
        mavenId = project.asString();

        /* Find and resolve dependencies */
        final List<Dependency> missing = new ArrayList<>();
        for (final Dependency dependency: project.getDependencies()) {
            final Marker marker = mapper.getIvyMarker(dependency);

            if (dependency.isOptional()) {
                optionalDependencies.put(dependency, marker);
            } else switch (dependency.getScope()) {
                case COMPILE:
                case RUNTIME:
                    if (marker == null) {
                        missing.add(dependency);
                    } else {
                        dependencies.put(mapper.getIvyMarker(dependency), dependency);
                    }
                    break;
                default: // ignore PROVIDED, SYSTEM, TEST
                    optionalDependencies.put(dependency, marker);
                    break;
            }
        }

        /* Check if we resolved all dependencies */
        if (!missing.isEmpty()) throw new DependenciesException(this, missing);

        /* Check if we have the various artifacts */
        checkArtifact(project.getJarURI(), BIN);
        checkArtifact(project.getSourcesURI(), SRC);
        checkArtifact(project.getJavadocURI(), DOC);
    }

    public String getMavenId() {
        return mavenId;
    }

    public Date getPublicationDate() {
        return publication;
    }

    public String getPublicationDateString() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(publication);
    }

    public URI getHomePage() {
        return homePage;
    }

    public String getDescription() {
        return description;
    }

    public Map<License, URI> getLicenses() {
        return licenses;
    }

    public Map<ArtifactType, URI> getArtifacts() {
        return artifacts;
    }

    public Map<Marker, Dependency> getDependencies() {
        return dependencies;
    }

    public Map<Dependency, Marker> getOptionalDependencies() {
        return optionalDependencies;
    }

    /* Do a HEAD request against a URI */
    private void checkArtifact(URI uri, ArtifactType publication) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                artifacts.put(publication, connection.getURL().toURI());
                Log.info("Artifact URI " + uri.toASCIIString() + " found");
            } else {
                Log.warn("Artifact URI " + uri.toASCIIString() + " not accessible");
            }
        } catch (final Exception exception) {
            Log.error("I/O error calling HEAD on " + uri, exception);
        }

    }
}
