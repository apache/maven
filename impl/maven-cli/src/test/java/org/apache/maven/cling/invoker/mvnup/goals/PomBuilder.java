/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.cling.invoker.mvnup.goals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

/**
 * Builder for creating test POM documents with fluent API.
 */
public class PomBuilder {

    private String modelVersion = "4.0.0";
    private String namespace = "http://maven.apache.org/POM/4.0.0";
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private Parent parent;
    private final List<Dependency> dependencies = new ArrayList<>();
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<Property> properties = new ArrayList<>();

    public static PomBuilder create() {
        return new PomBuilder();
    }

    public PomBuilder modelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    public PomBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PomBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public PomBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public PomBuilder version(String version) {
        this.version = version;
        return this;
    }

    public PomBuilder packaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public PomBuilder parent(String groupId, String artifactId, String version) {
        this.parent = new Parent(groupId, artifactId, version);
        return this;
    }

    public PomBuilder dependency(String groupId, String artifactId, String version) {
        this.dependencies.add(new Dependency(groupId, artifactId, version, null));
        return this;
    }

    public PomBuilder dependency(String groupId, String artifactId, String version, String scope) {
        this.dependencies.add(new Dependency(groupId, artifactId, version, scope));
        return this;
    }

    public PomBuilder plugin(String groupId, String artifactId, String version) {
        this.plugins.add(new Plugin(groupId, artifactId, version));
        return this;
    }

    public PomBuilder property(String name, String value) {
        this.properties.add(new Property(name, value));
        return this;
    }

    public String build() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<project xmlns=\"").append(namespace).append("\">\n");
        if (modelVersion != null) {
            xml.append("    <modelVersion>").append(modelVersion).append("</modelVersion>\n");
        }

        if (parent != null) {
            xml.append("    <parent>\n");
            xml.append("        <groupId>").append(parent.groupId).append("</groupId>\n");
            xml.append("        <artifactId>").append(parent.artifactId).append("</artifactId>\n");
            xml.append("        <version>").append(parent.version).append("</version>\n");
            xml.append("    </parent>\n");
        }

        if (groupId != null) {
            xml.append("    <groupId>").append(groupId).append("</groupId>\n");
        }
        if (artifactId != null) {
            xml.append("    <artifactId>").append(artifactId).append("</artifactId>\n");
        }
        if (version != null) {
            xml.append("    <version>").append(version).append("</version>\n");
        }
        if (packaging != null) {
            xml.append("    <packaging>").append(packaging).append("</packaging>\n");
        }

        if (!properties.isEmpty()) {
            xml.append("    <properties>\n");
            for (Property property : properties) {
                xml.append("        <")
                        .append(property.name)
                        .append(">")
                        .append(property.value)
                        .append("</")
                        .append(property.name)
                        .append(">\n");
            }
            xml.append("    </properties>\n");
        }

        if (!dependencies.isEmpty()) {
            xml.append("    <dependencies>\n");
            for (Dependency dependency : dependencies) {
                xml.append("        <dependency>\n");
                xml.append("            <groupId>").append(dependency.groupId).append("</groupId>\n");
                xml.append("            <artifactId>")
                        .append(dependency.artifactId)
                        .append("</artifactId>\n");
                xml.append("            <version>").append(dependency.version).append("</version>\n");
                if (dependency.scope != null) {
                    xml.append("            <scope>").append(dependency.scope).append("</scope>\n");
                }
                xml.append("        </dependency>\n");
            }
            xml.append("    </dependencies>\n");
        }

        if (!plugins.isEmpty()) {
            xml.append("    <build>\n");
            xml.append("        <plugins>\n");
            for (Plugin plugin : plugins) {
                xml.append("            <plugin>\n");
                xml.append("                <groupId>").append(plugin.groupId).append("</groupId>\n");
                xml.append("                <artifactId>")
                        .append(plugin.artifactId)
                        .append("</artifactId>\n");
                xml.append("                <version>").append(plugin.version).append("</version>\n");
                xml.append("            </plugin>\n");
            }
            xml.append("        </plugins>\n");
            xml.append("    </build>\n");
        }

        xml.append("</project>\n");
        return xml.toString();
    }

    public Document buildDocument() {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            return saxBuilder.build(new StringReader(build()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build POM document", e);
        }
    }

    private record Parent(String groupId, String artifactId, String version) {}

    private record Dependency(String groupId, String artifactId, String version, String scope) {}

    private record Plugin(String groupId, String artifactId, String version) {}

    private record Property(String name, String value) {}
}
