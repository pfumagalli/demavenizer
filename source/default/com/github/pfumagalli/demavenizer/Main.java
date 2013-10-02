package com.github.pfumagalli.demavenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map.Entry;

import com.github.pfumagalli.demavenizer.ivy.ArtifactType;
import com.github.pfumagalli.demavenizer.ivy.DependenciesException;
import com.github.pfumagalli.demavenizer.ivy.Descriptor;
import com.github.pfumagalli.demavenizer.ivy.Mapper;
import com.github.pfumagalli.demavenizer.ivy.Marker;
import com.github.pfumagalli.demavenizer.ivy.Serializer;
import com.github.pfumagalli.demavenizer.maven.Dependency;
import com.github.pfumagalli.demavenizer.maven.Identifier;
import com.github.pfumagalli.demavenizer.maven.Project;
import com.github.pfumagalli.demavenizer.maven.Repository;
import com.github.pfumagalli.demavenizer.parser.Expression;

public class Main {

    private Main() {
        throw new IllegalStateException();
    }

    private static void help(Configuration configuration) {
        Log.info("");
        Log.warn("Usage: " + Main.class.getName() + " [-options ...] <maven id> [ivy module]");
        Log.info("");

        int maxlen = 0;
        for (final String key: configuration.keySet())
            if (key.length() > maxlen) maxlen = key.length();
        final String format = "  %-" + maxlen + "s : %s";

        Log.info(String.format(format, "<maven id>",   "The Maven artifact in the format group#artifact#version"));
        Log.info(String.format(format, "<ivy module>", "The Ivy module in the format organization#module#revision"));
        Log.info(String.format(format, "",             "This defaults to the maven id, and revision is optional"));

        Log.info("");
        Log.warn("Options:");

        Log.info(String.format(format, "-config file", "Load properties from the specified file"));
        Log.info(String.format(format, "-library dir", "Locate the library in the specified directory (defaults to ${user.dir})"));
        Log.info(String.format(format, "-update",      "Update an already mapped library automatically fetching the latest version"));
        Log.info(String.format(format, "-fetch",       "Fetch binaries rather than simply linking them"));
        Log.info(String.format(format, "-help",        "Show this help page"));
        Log.info("");
        Log.warn("Properties:");

        for (final String key: configuration.keySet()) {
            final Expression expression = Expression.parse(configuration.getUnresolved(key), null);
            Log.info(String.format(format, key, expression.evaluate(configuration, true)));
        }
        Log.info("");
        System.exit(1);
    }

    public static void main(String[] args)
    throws Exception {
        final Configuration configuration = new Configuration();

        if (args.length < 1) help(configuration);

        /* Process command line options */
        boolean update = false;
        int x = 0;
        while (x < args.length) {
            if ("-help".equals(args[x])) help(configuration);
            else if ("-update".equals(args[x])) update = true;
            else if ("-fetch".equals(args[x])) configuration.put("artifacts.fetch", "true");
            else if ("-library".equals(args[x])) configuration.put("library.dir", args[++x]);
            else if ("-config".equals(args[x])) configuration.put("config.file", args[++x]);
            else break;
            x ++;
        }

        /* Do we still have some options? */
        if (args.length <= x) help(configuration);

        /* Merge configurations, if they exist */
        configuration.merge(configuration.get("config.file"));

        /* Repository URL */
        final String mavenUrlProperty = configuration.get("maven.url");
        if (mavenUrlProperty == null) {
            Log.error("Property \"maven.url\" not set");
            return;
        }
        final Repository repository = new Repository(URI.create(mavenUrlProperty));

        /* Libraries directory */
        final String librariesDirProperty = configuration.get("library.dir");
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
        final String mappingsFileProperty = configuration.get("mappings.file");
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
        final String licensesFileProperty = configuration.get("licenses.file");
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
        Serializer.toXml(System.out, descriptor, configuration);
        System.out.println();
        System.out.flush();

        /* Check the Ivy file */
        final File file = new File(configuration.getResolved("artifacts.ivy.pattern", descriptor.asMap())).getCanonicalFile();
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

        if (Boolean.parseBoolean(configuration.get("artifacts.fetch"))) {
            for (final Entry<ArtifactType, URI> entry: descriptor.getArtifacts().entrySet()) {
                final ArtifactType type = entry.getKey();
                final URI uri = entry.getValue();
                final File artifact = new File(file.getParentFile(), descriptor.getModule() + "-" + type.name().toLowerCase() + type.getExtension(configuration));
                Log.warn("Fetching " + uri);
                Log.warn("      to " + artifact);
            }
        }

        System.exit(0);

        final OutputStream out = new FileOutputStream(file);
        Serializer.toXml(out, descriptor, configuration);
        out.flush();
        out.close();
    }

}
