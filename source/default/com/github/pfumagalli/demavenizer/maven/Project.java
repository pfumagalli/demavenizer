package com.github.pfumagalli.demavenizer.maven;

import static com.github.pfumagalli.demavenizer.ivy.Normalizer.normalizeWhitespace;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.pfumagalli.demavenizer.License;
import com.github.pfumagalli.demavenizer.Log;
import com.github.pfumagalli.demavenizer.parser.Node;

public class Project extends Identifier {

    private final Map<String, Object> properties = new HashMap<>();
    private final Set<Dependency> dependencies = new HashSet<>();
    private final Map<License, URI> licenses = new HashMap<>();
    private final Date lastModifiedDate;

    private final URI uri;
    private final URI pomURI;
    private final URI jarURI;
    private final URI sourcesURI;
    private final URI javadocURI;
    private final String name;
    private final String description;

    public Project(Repository repository, Identifier identifier, Node node) {
        super(identifier, null, node);

        /* Basic checks */
        if (!"project".equals(node.getName()))
            throw new IllegalArgumentException("Found <" + node.getName() + "/> node");
        if (getVersion() == null)
            throw new IllegalStateException("Version unknown for " + this);

        /* Remember our URIs */
        pomURI = node.getURI();
        jarURI = node.getURI().resolve(getArtifactId() + "-" + getVersion() + ".jar");
        sourcesURI = node.getURI().resolve(getArtifactId() + "-" + getVersion() + "-sources.jar");
        javadocURI = node.getURI().resolve(getArtifactId() + "-" + getVersion() + "-javadoc.jar");

        /* Remember the release date */
        lastModifiedDate = node.getLastModified();

        /* Remember ourselves */
        properties.put("project", this);
        properties.put("pom", this);

        /* Basic variables */
        URI uri = null;
        String name = null;
        String description = null;

        /* Parents */
        for (final Node parent: node.getChildren("parent")) {
            final Project project = repository.getProject(new Identifier(null, properties, parent));

            /* Copy licenses, properties and dependencies */
            licenses.putAll(project.getLicenses());
            properties.putAll(project.getProperties());
            dependencies.addAll(project.getDependencies());

            /* Copy basics */
            uri = project.getURI();
            name = normalizeWhitespace(project.getName());
            description = normalizeWhitespace(project.getDescription());
        }

        /* Re-contextualize ourselves after parents */
        properties.put("project", this);
        properties.put("pom", this);

        /* Local properties */
        for (final Node properties: node.getChildren("properties")) {
            for (final Node property: properties.getChildren()) {
                final String value = property.getText(this.properties, null);
                this.properties.put(property.getName(), value);
            }
        }

        /* Group ID, artifact ID and version? */
        if (! properties.containsKey("groupId")) properties.put("groupId", identifier.getGroupId());
        if (! properties.containsKey("artifactId")) properties.put("artifactId", identifier.getArtifactId());
        if (! properties.containsKey("version")) properties.put("version", identifier.getVersion());

        /* Freeze basics */
        final String uriString = node.getChildText("url", properties, null);
        this.uri = uriString == null ? uri == null ? null : uri : URI.create(uriString);
        this.name = node.getChildText("name", properties, name);
        this.description = node.getChildText("name", properties, description);

        /* Dependencies */
        for (final Node dependencies: node.getChildren("dependencies")) {
            for (final Node dependency: dependencies.getChildren()) try {
                this.dependencies.add(new Dependency(this, dependency));
            } catch (final Exception exception) {
                throw new IllegalStateException("Exception in dependency of " + this, exception);
            }
        }

        /* Licenses */
        for (final Node licenses: node.getChildren("licenses")) {
            for (final Node license: licenses.getChildren()) {
                final String licenseUrl = license.getChildText("url", properties, null);
                final String licenseName = license.getChildText("name", properties, null);

                License licenseId = null;
                if (licenseUrl != null) try {
                    licenseId = License.fromLocation(licenseUrl);
                } catch (final Exception exception) {
                    Log.warn("License for " + this + " not found by URL " + licenseUrl);
                    if (licenseName != null) try {
                        licenseId = License.fromName(licenseName);
                    } catch (final Exception exception2) {
                        Log.warn("License for " + this + " not found by name " + licenseName);
                        licenseId = null;
                    }
                } else if (licenseName != null) try {
                    licenseId = License.fromName(licenseName);
                } catch (final Exception exception2) {
                    Log.warn("License for " + this + " not found by name " + licenseName);
                    licenseId = null;
                }

                if (licenseId == null) {
                    throw new IllegalStateException("Null license for name=\"" + licenseName+ "\" url=\"" + licenseUrl + "\"");
                }
                this.licenses.put(licenseId, licenseUrl == null ? null : URI.create(normalizeWhitespace(licenseUrl)));
            }
        }
    }

    public URI getPomURI() {
        return pomURI;
    }

    public URI getJarURI() {
        return jarURI;
    }

    public URI getSourcesURI() {
        return sourcesURI;
    }

    public URI getJavadocURI() {
        return javadocURI;
    }

    public URI getURI() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Map<License, URI> getLicenses() {
        return licenses;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Set<Dependency> getDependencies() {
        return dependencies;
    }

}
