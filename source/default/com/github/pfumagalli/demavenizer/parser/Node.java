package com.github.pfumagalli.demavenizer.parser;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Node {

    private final StringBuilder characters = new StringBuilder();
    private final Map<String, String> attributes = new HashMap<>();
    private final List<Node> children = new ArrayList<>();
    private final Date lastModified;
    private final String name;
    private final URI uri;

    public Node(String name, URI uri, Date lastModified) {
        this.name = name;
        this.uri = uri;
        this.lastModified = lastModified;
    }

    /* ====================================================================== */

    void addCharacters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    void addChild(Node node) {
        children.add(node);
    }

    void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    /* ====================================================================== */

    public URI getURI() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public List<Node> getChildren() {
        return children;
    }

    public List<Node> getChildren(String name) {
        final List<Node> children = new ArrayList<>();
        for (final Node child: this.children) {
            if (name.equals(child.name)) children.add(child);
        }
        return children;
    }

    public String getText(Map<?, ?> context, String defaultValue) {
        final String text = characters.toString().trim();
        if (text.isEmpty()) return defaultValue;
        try {
            return Expression.parse(text, uri).evaluate(context);
        } catch (final Exception exception) {
            throw new IllegalStateException("Evaluation exception in " + uri, exception);
        }
    }

    public String getChildText(String name, Map<?, ?> context, String defaultValue) {

        for (final Node child: children) {
            if (name.equals(child.name)) {
                return child.getText(context, defaultValue);
            }
        }
        return defaultValue;
    }

    /* ====================================================================== */

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        final StringBuilder builder = new StringBuilder();
        for (int x = 0; x < indent; x ++) builder.append(' ');
        builder.append(name);
        builder.append('[');
        boolean first = true;
        for (final Entry<String, String> entry: attributes.entrySet()) {
            if (!first) builder.append(',');
            builder.append(entry.getKey());
            builder.append('=');
            builder.append(entry.getValue());
            first = false;
        }
        builder.append(']');
        builder.append('{');
        builder.append(characters.toString().trim());
        builder.append('}');
        for (final Node child: children) {
            builder.append('\n');
            builder.append(child.toString(indent + 1));
        }
        return builder.toString();
    }
}
