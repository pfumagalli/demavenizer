package com.github.pfumagalli.demavenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;

import com.github.pfumagalli.demavenizer.ivy.DependenciesException;
import com.github.pfumagalli.demavenizer.ivy.Descriptor;
import com.github.pfumagalli.demavenizer.ivy.Mapper;
import com.github.pfumagalli.demavenizer.ivy.Marker;
import com.github.pfumagalli.demavenizer.ivy.Serializer;
import com.github.pfumagalli.demavenizer.maven.Dependency;
import com.github.pfumagalli.demavenizer.maven.Identifier;
import com.github.pfumagalli.demavenizer.maven.Project;
import com.github.pfumagalli.demavenizer.maven.Repository;

public class Main {

    private Main() {
        throw new IllegalStateException();
    }

    public static void main(String[] args)
    throws Exception {

        if (args.length < 1) {
            Log.info("Usage: " + Main.class.getName() + " <maven groupId#artifactId#version> [ivy organisation#module#revision]");
            Log.info("");
            Log.info("System properties:");
            Log.info("  licenses.file  : Properties file of accepted licenses");
            Log.info("  libraries.dir  : Directory where all \"ivy.xml\" are found");
            Log.info("  mappings.file  : Properties file for Maven to Maven mappings");
            Log.info("  maven.url      : Base URL of the Maven repository");
            Log.info("");
            return;
        }

        /* Repository URL */
        final String mavenUrlProperty = System.getProperty("maven.url");
        if (mavenUrlProperty == null) {
            Log.error("Property \"maven.url\" not set");
            return;
        }
        final Repository repository = new Repository(URI.create(mavenUrlProperty));

        /* Libraries directory */
        final String librariesDirProperty = System.getProperty("libraries.dir");
        if (librariesDirProperty == null) {
            Log.error("Property \"libraries.dir\" not set");
            return;
        }
        final File librariesDir = new File(librariesDirProperty);
        if (!librariesDir.isDirectory()) {
            Log.error("Libraries directory " + librariesDir + " not found");
            return;
        }

        /* Maven to Maven mappings file */
        final String mappingsFileProperty = System.getProperty("mappings.file");
        if (mappingsFileProperty == null) {
            Log.error("Property \"mappings.file\" not set");
            return;
        }
        final File mappingsFile = new File(mappingsFileProperty);
        if (!mappingsFile.isFile()) {
            Log.error("Mappings file " + mappingsFile + " not found");
            return;
        }

        /* Create our mapper */
        final Mapper mapper = new Mapper(librariesDir, mappingsFile);

        /* Licenses file */
        final String licensesFileProperty = System.getProperty("licenses.file");
        if (licensesFileProperty == null) {
            Log.error("Property \"licenses.file\" not set");
            return;
        }
        final File licensesFile = new File(licensesFileProperty);
        if (!licensesFile.isFile()) {
            Log.error("Licenses file " + licensesFile + " not found");
            return;
        }
        License.load(licensesFile);

        /* Load the original project */
        final Project project = repository.getProject(new Identifier(args[0]));

        /* Get our marker if we need to translate */
        final Marker translated = args.length < 2 ? null : new Marker(project, args[1]);

        /* Create the descriptor */
        final Descriptor descriptor;
        try {
            descriptor = new Descriptor(project, mapper, translated);
        } catch (final DependenciesException exception) {
            Log.error(exception.getMessage());
            for (final Dependency dependency: exception.getMissingDependencies())
            Log.error(" -> " + dependency.asString());
            return;
        }

        Log.info("Translated Maven " + project.asString() + " to Ivy " + descriptor.asString());

        /* Preview! */
        System.out.println();
        Serializer.toXml(System.out, descriptor);
        System.out.println();
        System.out.flush();

        /* Check the Ivy file */
        final File file = new File(new File(new File(new File(librariesDir, descriptor.getOrganisation()), descriptor.getModule()), descriptor.getRevision().toString()), "ivy.xml").getCanonicalFile();
        if (file.exists()) {
            Log.error("Ivy file " + file + " already exists, double confirmation needed! [CRTL-C to exit]");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        /* Wait for acknowledgement and write */
        if (descriptor.getLicenses().isEmpty()) Log.error("No licenses found");
        if (descriptor.getArtifacts().isEmpty()) Log.error("No artifacts found");
        Log.warn("Write to " + file.getAbsolutePath() + " [CRTL-C to exit] ?");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        file.getParentFile().mkdirs();
        final OutputStream out = new FileOutputStream(file);
        Serializer.toXml(out, descriptor);
        out.flush();
        out.close();
    }
}
