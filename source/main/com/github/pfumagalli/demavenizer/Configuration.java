package com.github.pfumagalli.demavenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.github.pfumagalli.demavenizer.parser.Expression;

public class Configuration extends AbstractMap<String, String> {

    private final Map<String, String> entries = new TreeMap<>();
    private URI location;

    public Configuration()
    throws IOException, URISyntaxException {
        merge(Configuration.class.getResourceAsStream("default.properties"));
        location = Configuration.class.getResource("default.properties").toURI();
    }

    public void merge(String location)
    throws IOException {
        final File file = new File(location);
        if (file.isFile()) {
            this.location = file.toURI();
            Log.info("Parsing properties " + file.getAbsolutePath());
            this.merge(new FileInputStream(location));
        } else {
            Log.warn("Unable to find configuration file " + file);
        }
    }

    private void merge(InputStream input)
    throws IOException {
        final Properties properties = new Properties();
        properties.load(new InputStreamReader(input, Charset.forName("UTF8")));
        for (final Entry<Object, Object> entry: properties.entrySet())
            entries.put(entry.getKey().toString(), entry.getValue().toString());
    }

    @Override
    public String get(Object key) {
        if (key == null) return null;
        final String value = entries.get(key);
        if (value == null) return null;
        return Expression.parse(value, location).evaluate(this);
    }

    public String getResolved(Object key, Map<?, ?> extra) {
        if (key == null) return null;
        final String value = entries.get(key);
        if (value == null) return null;

        final Map<Object, Object> map = new HashMap<Object, Object>();
        map.putAll(entries);
        map.putAll(extra);

        return Expression.parse(value, location).evaluate(map);
    }

    public String getUnresolved(Object key) {
        return entries.get(key);
    }

    @Override
    public String put(String key, String value) {
        return entries.put(key, value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {

            @Override
            public Iterator<Entry<String, String>> iterator() {
                final Iterator<Entry<String, String>> iterator = entries.entrySet().iterator();
                return new Iterator<Entry<String, String>>() {
                    @Override public void remove() { iterator.remove(); }
                    @Override public boolean hasNext() { return iterator.hasNext(); }
                    @Override public Entry<String, String> next() {
                        final Entry<String, String> entry = iterator.next();
                        return new Entry<String, String>() {
                            @Override public String getKey() { return entry.getKey(); }
                            @Override public String getValue() { return get(entry.getKey()); }
                            @Override public String setValue(String value) { return entry.setValue(value); }
                        };
                    }

                };
            }

            @Override
            public int size() {
                return entries.size();
            }

        };
    }

}
