package com.github.pfumagalli.demavenizer.parser;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Stack;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Parser extends DefaultHandler {

    public static Node parse(URI uri)
    throws ParseException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        try {
            final URL url = uri.toURL();
            final URLConnection connection = url.openConnection();
            final Date lastModified = new Date(connection.getLastModified());

            final NodeHandler handler = new NodeHandler(lastModified, uri);

            final InputStream input = connection.getInputStream();
            factory.newSAXParser().parse(input, handler);
            input.close();

            return handler.rootNode;

        } catch (final Exception exception) {
            throw new ParseException(uri, exception);
        }

    }

    private static class NodeHandler extends DefaultHandler {

        private final Stack<Node> nodes = new Stack<>();
        private final Date lastModified;
        private final URI uri;

        private Node rootNode = null;

        private NodeHandler(Date lastModified, URI uri) {
            this.lastModified = lastModified;
            this.uri = uri;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
        throws SAXException {
            final Node node = new Node(localName, this.uri, lastModified);

            for (int x = 0; x < atts.getLength(); x ++) {
                final String aName = atts.getLocalName(x);
                final String aValue = atts.getValue(x);
                node.addAttribute(aName, aValue);
            }

            if (nodes.isEmpty()) {
                rootNode = node;
            } else {
                nodes.peek().addChild(node);
            }
            nodes.push(node);

        }

        @Override
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            nodes.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length)
        throws SAXException {
            nodes.peek().addCharacters(ch, start, length);
        }
    }

}
