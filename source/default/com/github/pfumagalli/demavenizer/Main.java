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

    private static void help() {
        Log.info("");
        Log.warn("Usage: " + Main.class.getName() + " [-fetch] [-update] <maven groupId#artifactId#version> [ivy organisation#module#revision]");
        Log.info("  -update        : Update an already mapped library");
        Log.info("  -fetch         : Fetch binaries rather than linking them");
        Log.info("  -help          : Show this help page");
        Log.info("");
        Log.warn("System properties:");
        Log.info("  licenses.file  : Properties file of accepted licenses");
        Log.info("  libraries.dir  : Directory where all \"ivy.xml\" are found");
        Log.info("  mappings.file  : Properties file for Maven to Maven mappings");
        Log.info("  maven.url      : Base URL of the Maven repository");
        Log.info("");
        System.exit(1);
    }

    public static void main(String[] args)
    throws Exception {

        if (args.length < 1) help();

        /* Process command line options */
        boolean update = false;
        boolean fetch = false;
        int x = 0;
        while (x < args.length) {
            if ("-help".equals(args[x])) help();
            else if ("-update".equals(args[x])) update = true;
            else if ("-fetch".equals(args[x])) fetch = true;
            else break;
            x ++;
        }

        /* Do we still have some options? */
        if (args.length <= x) help();

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

        /* If we're updating to the last version, we have some work to do */
        final Project project;
        final Marker translated;

        if (update) {

            /* Get the latest version of the required module */
            final Marker marker = mapper.getLatest(args[x++]);
            if (marker == null) {
                Log.error("Unable to locate " + args[x - 1] + " in repository");
                return;
            }

            /* Start a project from the original identifer (unversioned) */
            project = repository.getProject(marker.getIdentifier().unversioned());

            /* If we have specified a translation, use it, otherwise default to the marker */
            if (x < args.length) {
                translated = new Marker(project, args[x]);
            } else {
                translated = new Marker(project, marker.getOrganisation() + "#" + marker.getModule());
            }

        } else {

            /* We're not updating, so we construct everything as normal */
            project = repository.getProject(new Identifier(args[x++]));
            translated = x < args.length ? new Marker(project, args[x]) : null;
        }

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
