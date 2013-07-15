package com.github.pfumagalli.demavenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

public final class License {

    private final String id;
    private final String name;
    private final URI uri;

    private License(String id, String name, String uri) {
        this.id = id;
        this.name = name;
        this.uri = URI.create(uri);
    }

    public String id() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getURI() {
        return uri;
    }

    /* ====================================================================== */

    private static final Map<String, License> byName = new HashMap<>();
    private static final Map<String, License> byLocation = new HashMap<>();

    public static void load(File licensesFile) {
        if (!licensesFile.isFile())
            throw new IllegalStateException("Licenses file \"" + licensesFile + "\" not found");

        Log.info("Loading licenses from " + licensesFile);
        try {
            final InputStream input = new FileInputStream(licensesFile);
            final Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
            final Properties properties = new Properties();
            properties.load(reader);
            reader.close();
            input.close();

            final String licenses = properties.getProperty("licenses");
            final StringTokenizer tokenizer = new StringTokenizer(licenses, ",");
            while (tokenizer.hasMoreTokens()) {
                final String id = tokenizer.nextToken().trim().toLowerCase();
                final String name = properties.getProperty(id + ".name");
                final String location = properties.getProperty(id + ".location");

                if (name == null) throw new IllegalStateException("No name found for license \"" + id + "\"");
                if (location == null) throw new IllegalStateException("No location found for license \"" + id + "\"");

                final License license = new License(id, name, location);
                byName.put(name.trim().toLowerCase(), license);
                byLocation.put(location.trim().toLowerCase(), license);

                final String nameKey = id + ".name.";
                final String locationKey = id + ".location.";

                for (final Entry<?, ?> entry: properties.entrySet()) {
                    final String key = entry.getKey().toString();
                    final String value = entry.getValue().toString();

                    if (key.startsWith(nameKey)) {
                        byName.put(value.trim().toLowerCase(), license);

                    } else if (key.startsWith(locationKey)) {
                        byLocation.put(value.trim().toLowerCase(), license);
                    }
                }

            }

        } catch (final IOException exception) {
            final Error error = new InternalError("Unable to load licenses resource");
            throw (Error) error.initCause(exception);
        }
    }

    public static License fromName(String name) {
        final License license = byName.get(name.trim().toLowerCase());
        if (license != null) return license;
        throw new IllegalArgumentException("Invalid license name \"" + name.trim() + "\"");
    }

    public static License fromLocation(String location) {
        final License license = byLocation.get(location.trim().toLowerCase());
        if (license != null) return license;
        throw new IllegalArgumentException("Invalid license location \"" + location.trim() + "\"");
    }

}
