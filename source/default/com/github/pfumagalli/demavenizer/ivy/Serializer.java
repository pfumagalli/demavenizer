package com.github.pfumagalli.demavenizer.ivy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import com.github.pfumagalli.demavenizer.License;
import com.github.pfumagalli.demavenizer.maven.Dependency;
import com.github.pfumagalli.demavenizer.maven.Identifier;

public class Serializer {

    public Serializer() {
        // TODO Auto-generated constructor stub
    }

    public static void toXml(OutputStream output, Descriptor descriptor)
    throws IOException {
        final Writer writer = new OutputStreamWriter(output, "UTF-8");
        final PrintWriter out = new PrintWriter(writer);

        // I'm nuts, writing XML by hand, but I like "proper" indenting...
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println();
        out.println("<ivy-module version=\"2.0\"");
        out.println("            xmlns:e=\"http://ant.apache.org/ivy/extra\"");
        out.println("            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("            xsi:noNamespaceSchemaLocation=\"http://ant.apache.org/ivy/schemas/ivy.xsd\">");
        out.println();
        out.println("  <info e:maven=\"" + escape(descriptor.getMavenId()) + "\"");
        out.println("        organisation=\"" + escape(descriptor.getOrganisation()) + "\"");
        out.println("        module=\""+ escape(descriptor.getModule()) + "\"");
        out.println("        revision=\"" + escape(descriptor.getRevision().toString()) + "\"");
        out.println("        publication=\"" + escape(descriptor.getPublicationDateString()) + "\"");

        final Map<License, URI> licenses = descriptor.getLicenses();
        final URI homePageURI = descriptor.getHomePage();
        final String description = descriptor.getDescription();

        if (licenses.isEmpty() && homePageURI == null && description == null) {
            out.println("        status=\"release\"/>");
        } else {
            out.println("        status=\"release\">");

            if (!licenses.isEmpty()) {
                out.println();
                for (final Entry<License, URI> entry: licenses.entrySet()) {
                    final License license = entry.getKey();
                    final URI licenseURI = entry.getValue();
                    if ((licenseURI != null) && (!licenseURI.equals(license.getURI()))) {
                        out.println("    <!-- " + escape(licenseURI.toASCIIString()) + " -->");
                    }
                    out.println("    <license name=\"" + escape(license.getName()) + "\"");
                    out.println("             url=\"" + escape(license.getURI().toASCIIString()) + "\"/>");
                }
            }

            if (homePageURI != null || description != null) {
                out.println();
                out.print("    <description");
                if (homePageURI != null) out.print(" homepage=\"" + escape(homePageURI.toASCIIString()) + "\"");
                if (description == null) {
                    out.println("/>");
                } else {
                    out.println(">");
                    out.println("      " + escape(description));
                    out.println("    </description>");
                }
            }

            out.println();
            out.println("  </info>");
        }

        out.println();
        out.println("  <configurations>");
        out.println("    <conf name=\"default\"/>");
        out.println("  </configurations>");

        final Map<ArtifactType, URI> artifacts = descriptor.getArtifacts();
        if (!artifacts.isEmpty()) {
            out.println();
            out.println("  <publications>");
            for (final Entry<ArtifactType, URI> entry: artifacts.entrySet()) {
                final ArtifactType type = entry.getKey();
                out.println("    <artifact name=\"" + descriptor.getModule() +
                                       "\" type=\"" + type.name().toLowerCase() +
                                        "\" ext=\"" + (type == ArtifactType.BIN ? "jar" : "zip") +
                                        "\" url=\"" + entry.getValue().toASCIIString() +
                                        "\"/>");
            }
            out.println("  </publications>");
        }

        final Map<Marker, Dependency> dependencies = descriptor.getDependencies();
        final Map<Dependency, Marker> optional = descriptor.getOptionalDependencies();

        out.println();
        if (dependencies.isEmpty() && optional.isEmpty()) {
            out.println("  <dependencies/>");
        } else {
            out.println("  <dependencies>");
            for (final Entry<Marker, Dependency> entry: dependencies.entrySet()) {
                final Marker marker = entry.getKey();
                final Dependency dependency = entry.getValue();
                out.print("    <dependency org=\""+ escape(marker.getOrganisation()) +
                                           "\" name=\"" + escape(marker.getModule()) +
                                           "\" rev=\"" + escape(marker.getRevision().toString()));
                if (dependency == null) {
                    out.println("\"/>");
                } else {
                    out.print("\"/> <!-- maven requested=\"" + escape(dependency.asString()));
                    final Identifier identifier = marker.getIdentifier();
                    if ((identifier != null) && (!identifier.asString().equals(dependency.asString()))) {
                        out.print("\" mapped=\"" + escape(identifier.asString()));
                    }
                    out.println("\" -->");
                }
            }

            if (!(dependencies.isEmpty() || optional.isEmpty())) {
                out.println();
            }

            for (final Entry<Dependency, Marker> entry: optional.entrySet()) {
                final Dependency dependency = entry.getKey();
                final Marker marker = entry.getValue();

                out.print("    <!-- dependency maven=\"" + escape(dependency.asString()));
                if (marker != null) {
                    out.print("\" org=\""+ escape(marker.getOrganisation()) +
                              "\" name=\"" + escape(marker.getModule()) +
                              "\" rev=\"" + escape(marker.getRevision().toString()));
                }

                out.print("\" conf=\"" + dependency.getScope().toString().toLowerCase());
                if (dependency.isOptional()) out.print(",optional");
                out.println("\" -->");
            }

            out.println("  </dependencies>");
        }



        out.println();
        out.println("</ivy-module>");

        out.flush();
        writer.flush();
        output.flush();
    }

    private static String escape(String string) {
        if (string == null) return null;
        return string.replace("&", "&amp;")
                     .replace("\"", "&quot;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;");
    }

}
