package com.github.pfumagalli.demavenizer.ivy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.github.pfumagalli.demavenizer.Log;
import com.github.pfumagalli.demavenizer.maven.Identifier;
import com.github.pfumagalli.demavenizer.parser.Node;
import com.github.pfumagalli.demavenizer.parser.Parser;

public class Mapper {

    private final Map<Identifier, Identifier> mavenMappings = new HashMap<>();
    private final Map<Identifier, Marker> ivyMappings = new HashMap<>();
    private final Map<Identifier, Marker> latestVersion = new HashMap<>();

    public Mapper(File repository, File mappings) {
        if (!repository.isDirectory())
            throw new IllegalArgumentException("Invalid directory " + repository);
        Log.info("Initializing repository mappings from " + repository);
        read(repository);

        if (!mappings.isFile())
            throw new IllegalArgumentException("Invalid mappings file " + mappings);
        load(mappings);

        calculateLatest();
    }

    public Marker getLatest(String identifier) {
        return getLatest(new Identifier(identifier));
    }

    public Marker getLatest(Identifier identifier) {
        final Identifier unversioned = identifier.unversioned();
        if (latestVersion.containsKey(unversioned)) return latestVersion.get(unversioned);
        for (final Marker marker: latestVersion.values()) {
            if ((!marker.getOrganisation().equals(identifier.getGroupId())) |
                (!marker.getModule().equals(identifier.getArtifactId()))) continue;
            return marker;
        }
        return null;
    }

    public Marker getIvyMarker(Identifier identifier) {

        /* Forced mapping? */
        final Identifier mapped = mavenMappings.get(identifier);
        if (mapped != null) {
            Log.warn("Using mapped " + mapped.asString() + " instead of " + identifier.asString());
            identifier = mapped;
        }

        /* Forced mapping catching all versions?? */
        final Identifier unversioned = mavenMappings.get(identifier.unversioned());
        if (unversioned != null) {
            Log.warn("Using mapped " + unversioned.asString() + " instead of " + identifier.asString());
            identifier = unversioned;
        }

        /* Need to get up latest version */
        if (identifier.getVersion() == null){
            final Marker marker = latestVersion.get(identifier);
            if (marker != null) {
                Log.warn("Using latest version " + marker.asString() + " for " + identifier.asString());
                return marker;
            }
        }

        /* Check if we have a mapping */
        return ivyMappings.get(identifier);
    }

    private void calculateLatest() {
        for (final Entry<Identifier, Marker> entry: ivyMappings.entrySet()) {

            final Identifier identifier = entry.getKey();
            final Identifier unversioned = identifier.unversioned();

            final Marker marker = entry.getValue();
            final Marker found = latestVersion.get(unversioned);
            if (found == null) {
                latestVersion.put(unversioned, marker);
            } else {
                if (found.getOrganisation().equals(marker.getOrganisation()) &&
                    found.getModule().equals(marker.getModule())) {
                    if (found.getRevision().compareTo(marker.getRevision()) < 0)
                        latestVersion.put(unversioned, marker);
                } else {
                    throw new IllegalStateException("Organization/module mismatch comparing versions for " + marker.asString() + " and " + found.asString());
                }
            }
        }

        for (final Entry<Identifier, Marker> entry: latestVersion.entrySet()) {
            Log.info("Latest version of " + entry.getKey().asString() + " is " + entry.getValue().asString());
        }
    }

    private void load(File file) {
        final Properties properties = new Properties();
        try {
            final InputStream input = new FileInputStream(file);
            final Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
            properties.load(reader);
            reader.close();
            input.close();
        } catch (final IOException exception) {
            throw new IllegalStateException("I/O error parsing " + file, exception);
        }

        for (final Entry<Object, Object> entry: properties.entrySet()) {
            final Identifier key = new Identifier(entry.getKey().toString());
            final Identifier value = new Identifier(entry.getValue().toString());
            mavenMappings.put(key, value);
            Log.info("Added mapping from Maven " + key.asString() + " to Maven " + value.asString());
        }
    }

    private void read(File file) {
        if (file.isDirectory()) {
            for (final File child: file.listFiles()) read(child);
        } else if (file.isFile() && file.getName().equals("ivy.xml")) {
            final Node node = Parser.parse(file.toURI());
            String maven = null;
            String organisation = null;
            String module = null;
            String revision = null;
            for (final Node info: node.getChildren("info")) {
                for (final Node mavenNode: info.getChildren("maven")) {
                    maven = mavenNode.getAttribute("id");
                }
                organisation = info.getAttribute("organisation");
                module = info.getAttribute("module");
                revision = info.getAttribute("revision");
            }
            if (maven == null) {
                Log.warn("No maven info found in " + file);
            } else {
                final Identifier identifier = new Identifier(maven);
                final Marker marker = new Marker(identifier, organisation, module, revision);
                ivyMappings.put(identifier, marker);
                Log.info("Added mapping from Maven " + identifier.asString() + " to Ivy " + marker.asString());
            }
        }
    }

}
