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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.PomEditor;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;

/**
 * Utility class for XML operations on Maven POM files.
 *
 * <p>This class provides convenience methods that delegate to:
 * <ul>
 * <li>{@link eu.maveniverse.domtrip.maven.PomEditor} - DomTrip's PomEditor</li>
 * <li>{@link eu.maveniverse.domtrip.Element} - DomTrip's Element API</li>
 * </ul>
 *
 * <p>These methods are kept for convenience and backward compatibility.
 * For more advanced operations, consider using ExtendedPomEditor or DomTrip directly.
 *
 * <h2>Using DomTrip Directly</h2>
 * <p>Many operations can be performed directly using DomTrip's Element API:
 * <pre>{@code
 * // Find child element
 * Element child = parent.child("version").orElse(null);
 *
 * // Check if child exists
 * boolean hasVersion = parent.child("version").isPresent();
 *
 * // Get child text content
 * String version = parent.child("version")
 *     .map(Element::textContent)
 *     .orElse(null);
 *
 * // Get trimmed text content
 * String trimmedVersion = parent.child("version")
 *     .map(Element::textContentTrimmed)
 *     .orElse(null);
 *
 * // Set text content (fluent API)
 * element.textContent("4.0.0");
 * }</pre>
 *
 * <h2>When to Use DomUtils</h2>
 * <p>Use DomUtils methods when you need:
 * <ul>
 * <li>Maven-specific element ordering (insertNewElement, insertContentElement)</li>
 * <li>High-level helpers (addGAVElements, createDependency, createPlugin)</li>
 * <li>Null-safe operations (updateElementContent, removeElement)</li>
 * <li>Update-or-create patterns (updateOrCreateChildElement)</li>
 * </ul>
 *
 * @see eu.maveniverse.domtrip.Element
 * @see eu.maveniverse.domtrip.Editor
 * @see eu.maveniverse.domtrip.maven.PomEditor
 */
public class DomUtils {

    private DomUtils() {
        // Utility class
    }

    /**
     * Inserts a new child element to the given parent element with proper Maven POM ordering.
     *
     * @param name the name of the new element
     * @param parent the parent element
     * @return the new element
     *
     */
    public static Element insertNewElement(String name, Element parent) {
        PomEditor editor = new PomEditor(parent.document());
        return editor.insertMavenElement(parent, name);
    }

    /**
     * Inserts a new content element with the given name and text content.
     *
     * @param parent the parent element
     * @param name the name of the new element
     * @param content the text content
     * @return the new element
     *
     */
    public static Element insertContentElement(Element parent, String name, String content) {
        PomEditor editor = new PomEditor(parent.document());
        return editor.insertMavenElement(parent, name, content);
    }

    /**
     * Finds a child element by name under the specified parent.
     *
     * @param parent the parent element
     * @param name the child element name to find
     * @return the child element if found, null otherwise
     *
     */
    public static Element findChildElement(Element parent, String name) {
        return parent.child(name).orElse(null);
    }

    /**
     * Serializes a domtrip Document to XML string with preserved formatting.
     *
     * @param document the domtrip Document
     * @return the XML string with preserved formatting
     *
     */
    public static String toXml(Document document) {
        Editor editor = new Editor(document);
        return editor.toXml();
    }

    /**
     * Removes an element from its parent.
     *
     * @param element the element to remove
     *
     */
    public static void removeElement(Element element) {
        Editor editor = new Editor(element.document());
        editor.removeElement(element);
    }

    /**
     * Convenience method to add GAV (groupId, artifactId, version) elements to a parent.
     *
     * @param parent the parent element (e.g., dependency or plugin)
     * @param groupId the groupId value
     * @param artifactId the artifactId value
     * @param version the version value (can be null to skip)
     *
     */
    public static void addGAVElements(Element parent, String groupId, String artifactId, String version) {
        insertContentElement(parent, GROUP_ID, groupId);
        insertContentElement(parent, ARTIFACT_ID, artifactId);
        if (version != null && !version.isEmpty()) {
            insertContentElement(parent, VERSION, version);
        }
    }

    /**
     * Convenience method to create a dependency element with GAV.
     *
     * @param dependenciesElement the dependencies parent element
     * @param groupId the groupId value
     * @param artifactId the artifactId value
     * @param version the version value (can be null)
     * @return the created dependency element
     *
     */
    public static Element createDependency(
            Element dependenciesElement, String groupId, String artifactId, String version) {
        Element dependency = insertNewElement(DEPENDENCY, dependenciesElement);
        addGAVElements(dependency, groupId, artifactId, version);
        return dependency;
    }

    /**
     * Convenience method to create a plugin element with GAV.
     *
     * @param pluginsElement the plugins parent element
     * @param groupId the groupId value
     * @param artifactId the artifactId value
     * @param version the version value (can be null)
     * @return the created plugin element
     *
     */
    public static Element createPlugin(Element pluginsElement, String groupId, String artifactId, String version) {
        Element plugin = insertNewElement(PLUGIN, pluginsElement);
        addGAVElements(plugin, groupId, artifactId, version);
        return plugin;
    }

    /**
     * Updates or creates a child element with the given content.
     *
     * @param parent the parent element
     * @param childName the child element name
     * @param content the content to set
     * @return the updated or created element
     *
     */
    public static Element updateOrCreateChildElement(Element parent, String childName, String content) {
        PomEditor editor = new PomEditor(parent.document());
        return editor.updateOrCreateChildElement(parent, childName, content);
    }
}
