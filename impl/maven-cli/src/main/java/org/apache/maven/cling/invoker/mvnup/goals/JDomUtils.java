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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.Text;

import static java.util.Arrays.asList;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Indentation;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.CI_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.CLASSIFIER;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.CONFIGURATION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.CONTRIBUTORS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEFAULT_GOAL;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCY_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DESCRIPTION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEVELOPERS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DISTRIBUTION_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.EXCLUSIONS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.EXECUTIONS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.EXTENSIONS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.FINAL_NAME;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GOALS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.INCEPTION_YEAR;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.INHERITED;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ISSUE_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.LICENSES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MAILING_LISTS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODEL_VERSION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.MODULES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.NAME;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.OPTIONAL;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ORGANIZATION;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.OUTPUT_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PACKAGING;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_REPOSITORIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PREREQUISITES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROPERTIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.REPORTING;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.REPOSITORIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SCM;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SCOPE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SCRIPT_SOURCE_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SOURCE_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SYSTEM_PATH;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.TEST_OUTPUT_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.TEST_SOURCE_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.TYPE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.URL;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;
import static org.jdom2.filter.Filters.textOnly;

/**
 * Utility class for JDOM operations.
 */
public class JDomUtils {

    // Element ordering configuration
    private static final Map<String, List<String>> ELEMENT_ORDER = new HashMap<>();

    static {
        // Project element order
        ELEMENT_ORDER.put(
                "project",
                asList(
                        MODEL_VERSION,
                        "",
                        PARENT,
                        "",
                        GROUP_ID,
                        ARTIFACT_ID,
                        VERSION,
                        PACKAGING,
                        "",
                        NAME,
                        DESCRIPTION,
                        URL,
                        INCEPTION_YEAR,
                        ORGANIZATION,
                        LICENSES,
                        "",
                        DEVELOPERS,
                        CONTRIBUTORS,
                        "",
                        MAILING_LISTS,
                        "",
                        PREREQUISITES,
                        "",
                        MODULES,
                        "",
                        SCM,
                        ISSUE_MANAGEMENT,
                        CI_MANAGEMENT,
                        DISTRIBUTION_MANAGEMENT,
                        "",
                        PROPERTIES,
                        "",
                        DEPENDENCY_MANAGEMENT,
                        DEPENDENCIES,
                        "",
                        REPOSITORIES,
                        PLUGIN_REPOSITORIES,
                        "",
                        BUILD,
                        "",
                        REPORTING,
                        "",
                        PROFILES));

        // Build element order
        ELEMENT_ORDER.put(
                BUILD,
                asList(
                        DEFAULT_GOAL,
                        DIRECTORY,
                        FINAL_NAME,
                        SOURCE_DIRECTORY,
                        SCRIPT_SOURCE_DIRECTORY,
                        TEST_SOURCE_DIRECTORY,
                        OUTPUT_DIRECTORY,
                        TEST_OUTPUT_DIRECTORY,
                        EXTENSIONS,
                        "",
                        PLUGIN_MANAGEMENT,
                        PLUGINS));

        // Plugin element order
        ELEMENT_ORDER.put(
                PLUGIN,
                asList(
                        GROUP_ID,
                        ARTIFACT_ID,
                        VERSION,
                        EXTENSIONS,
                        EXECUTIONS,
                        DEPENDENCIES,
                        GOALS,
                        INHERITED,
                        CONFIGURATION));

        // Dependency element order
        ELEMENT_ORDER.put(
                DEPENDENCY,
                asList(GROUP_ID, ARTIFACT_ID, VERSION, CLASSIFIER, TYPE, SCOPE, SYSTEM_PATH, OPTIONAL, EXCLUSIONS));
    }

    private JDomUtils() {
        // noop
    }

    /**
     * Inserts a new child element to the given root element. The position where the element is inserted is calculated
     * using the element order configuration. When no order is defined for the element, the new element is append as
     * last element (before the closing tag of the root element). In the root element, the new element is always
     * prepended by a text element containing a linebreak followed by the indentation characters. The indentation
     * characters are (tried to be) detected from the root element (see {@link #detectIndentation(Element)} ).
     *
     * @param name the name of the new element.
     * @param root the root element.
     * @return the new element.
     */
    public static Element insertNewElement(String name, Element root) {
        return insertNewElement(name, root, calcNewElementIndex(name, root));
    }

    /**
     * Inserts a new child element to the given root element at the given index.
     * For details see {@link #insertNewElement(String, Element)}
     *
     * @param name  the name of the new element.
     * @param root  the root element.
     * @param index the index where the element should be inserted.
     * @return the new element.
     */
    public static Element insertNewElement(String name, Element root, int index) {
        String indent = detectIndentation(root);
        Element newElement = createElement(name, root.getNamespace(), indent);

        // If the parent element only has minimal content (just closing tag indentation),
        // we need to handle it specially to avoid creating whitespace-only lines
        boolean parentHasMinimalContent = root.getContentSize() == 1
                && root.getContent(0) instanceof Text
                && ((Text) root.getContent(0)).getText().trim().isEmpty();

        if (parentHasMinimalContent) {
            // Remove the minimal content and let addAppropriateSpacing handle the formatting
            root.removeContent();
            index = 0; // Reset index since we removed content
        }

        root.addContent(index, newElement);
        addAppropriateSpacing(root, index, name, indent);

        // Ensure both the parent and new element have proper closing tag formatting
        ensureProperClosingTagFormatting(root);
        ensureProperClosingTagFormatting(newElement);

        return newElement;
    }

    /**
     * Creates a new element with proper formatting.
     * This method ensures that both the opening and closing tags are properly indented.
     */
    private static Element createElement(String name, Namespace namespace, String indent) {
        Element newElement = new Element(name, namespace);

        // Add minimal content to prevent self-closing tag and ensure proper formatting
        // This will be handled by ensureProperClosingTagFormatting
        newElement.addContent(new Text(""));

        return newElement;
    }

    /**
     * Adds appropriate spacing before the inserted element.
     */
    private static void addAppropriateSpacing(Element root, int index, String elementName, String indent) {
        // Find the preceding element name for spacing logic
        String prependingElementName = "";
        if (index > 0) {
            Content prevContent = root.getContent(index - 1);
            if (prevContent instanceof Element) {
                prependingElementName = ((Element) prevContent).getName();
            }
        }

        if (isBlankLineBetweenElements(prependingElementName, elementName, root)) {
            // Add a completely empty line followed by proper indentation
            // We need to be careful to ensure the empty line has no spaces
            root.addContent(index, new Text("\n")); // End current line
            root.addContent(index + 1, new Text("\n" + indent)); // Empty line + indentation for next element
        } else {
            root.addContent(index, new Text("\n" + indent));
        }
    }

    /**
     * Ensures that the parent element has proper closing tag formatting.
     * This method checks if the last content of the element is properly indented
     * and adds appropriate whitespace if needed.
     */
    private static void ensureProperClosingTagFormatting(Element parent) {
        List<Content> contents = parent.getContent();

        // Get the parent's indentation level
        String parentIndent = detectParentIndentation(parent);

        // If the element is empty or only contains empty text nodes, handle it specially
        if (contents.isEmpty()
                || (contents.size() == 1
                        && contents.get(0) instanceof Text
                        && ((Text) contents.get(0)).getText().trim().isEmpty())) {
            // For empty elements, add minimal content to ensure proper formatting
            // We add just a newline and parent indentation, which will be the closing tag line
            parent.removeContent();
            parent.addContent(new Text("\n" + parentIndent));
            return;
        }

        // Check if the last content is a Text node with proper indentation
        Content lastContent = contents.get(contents.size() - 1);
        if (lastContent instanceof Text) {
            String text = ((Text) lastContent).getText();
            // If the last text doesn't end with proper indentation for the closing tag
            if (!text.endsWith("\n" + parentIndent)) {
                // If it's only whitespace, replace it; otherwise append
                if (text.trim().isEmpty()) {
                    parent.removeContent(lastContent);
                    parent.addContent(new Text("\n" + parentIndent));
                } else {
                    // Append proper indentation
                    parent.addContent(new Text("\n" + parentIndent));
                }
            }
        } else {
            // If the last content is not a text node, add proper indentation for closing tag
            parent.addContent(new Text("\n" + parentIndent));
        }
    }

    /**
     * Detects the indentation level of the parent element.
     */
    private static String detectParentIndentation(Element element) {
        Parent parent = element.getParent();
        if (parent instanceof Element) {
            return detectIndentation((Element) parent);
        }
        return "";
    }

    /**
     * Inserts a new content element with the given name and text content.
     *
     * @param parent the parent element
     * @param name the name of the new element
     * @param content the text content
     * @return the new element
     */
    public static Element insertContentElement(Element parent, String name, String content) {
        Element element = insertNewElement(name, parent);
        element.setText(content);
        return element;
    }

    /**
     * Detects the indentation used for a given element by analyzing its parent's content.
     * This method examines the whitespace preceding the element to determine the indentation pattern.
     * It supports different indentation styles (2 spaces, 4 spaces, tabs, etc.).
     *
     * @param element the element to analyze
     * @return the detected indentation or a default indentation if none can be detected.
     */
    public static String detectIndentation(Element element) {
        // First try to detect from the current element
        for (Iterator<Text> iterator = element.getContent(textOnly()).iterator(); iterator.hasNext(); ) {
            String text = iterator.next().getText();
            int lastLsIndex = StringUtils.lastIndexOfAny(text, new String[] {"\n", "\r"});
            if (lastLsIndex > -1) {
                String indent = text.substring(lastLsIndex + 1);
                if (iterator.hasNext()) {
                    // This should be the indentation of a child element.
                    return indent;
                } else {
                    // This should be the indentation of the elements end tag.
                    String baseIndent = detectBaseIndentationUnit(element);
                    return indent + baseIndent;
                }
            }
        }

        Parent parent = element.getParent();
        if (parent instanceof Element) {
            String baseIndent = detectBaseIndentationUnit(element);
            return detectIndentation((Element) parent) + baseIndent;
        }

        return "";
    }

    /**
     * Detects the base indentation unit used in the document by analyzing indentation patterns.
     * This method traverses the document tree to find the most common indentation style.
     *
     * @param element any element in the document to analyze
     * @return the detected base indentation unit (e.g., "  ", "    ", "\t")
     */
    public static String detectBaseIndentationUnit(Element element) {
        // Find the root element to analyze the entire document
        Element root = element;
        while (root.getParent() instanceof Element) {
            root = (Element) root.getParent();
        }

        // Collect indentation samples from the document
        Map<String, Integer> indentationCounts = new HashMap<>();
        collectIndentationSamples(root, indentationCounts, "");

        // Analyze the collected samples to determine the base unit
        return analyzeIndentationPattern(indentationCounts);
    }

    /**
     * Recursively collects indentation samples from the document tree.
     */
    private static void collectIndentationSamples(
            Element element, Map<String, Integer> indentationCounts, String parentIndent) {
        for (Iterator<Text> iterator = element.getContent(textOnly()).iterator(); iterator.hasNext(); ) {
            String text = iterator.next().getText();
            int lastLsIndex = StringUtils.lastIndexOfAny(text, new String[] {"\n", "\r"});
            if (lastLsIndex > -1) {
                String indent = text.substring(lastLsIndex + 1);
                if (iterator.hasNext() && !indent.isEmpty()) {
                    // This is indentation before a child element
                    if (indent.length() > parentIndent.length()) {
                        String indentDiff = indent.substring(parentIndent.length());
                        indentationCounts.merge(indentDiff, 1, Integer::sum);
                    }
                }
            }
        }

        // Recursively analyze child elements
        for (Element child : element.getChildren()) {
            String childIndent = detectIndentationForElement(element, child);
            if (childIndent != null && childIndent.length() > parentIndent.length()) {
                String indentDiff = childIndent.substring(parentIndent.length());
                indentationCounts.merge(indentDiff, 1, Integer::sum);
                collectIndentationSamples(child, indentationCounts, childIndent);
            }
        }
    }

    /**
     * Detects the indentation used for a specific child element.
     */
    private static String detectIndentationForElement(Element parent, Element child) {
        int childIndex = parent.indexOf(child);
        if (childIndex > 0) {
            Content prevContent = parent.getContent(childIndex - 1);
            if (prevContent instanceof Text) {
                String text = ((Text) prevContent).getText();
                int lastLsIndex = StringUtils.lastIndexOfAny(text, new String[] {"\n", "\r"});
                if (lastLsIndex > -1) {
                    return text.substring(lastLsIndex + 1);
                }
            }
        }
        return null;
    }

    /**
     * Analyzes the collected indentation patterns to determine the most likely base unit.
     */
    private static String analyzeIndentationPattern(Map<String, Integer> indentationCounts) {
        if (indentationCounts.isEmpty()) {
            return Indentation.TWO_SPACES; // Default to 2 spaces
        }

        // Find the most common indentation pattern
        String mostCommon = indentationCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Indentation.TWO_SPACES);

        // Validate and normalize the detected pattern
        if (mostCommon.matches("^\\s+$")) { // Only whitespace characters
            return mostCommon;
        }

        // If we have mixed patterns, try to find a common base unit
        Set<String> patterns = indentationCounts.keySet();

        // Check for common patterns
        if (patterns.stream().anyMatch(p -> p.equals(Indentation.FOUR_SPACES))) {
            return Indentation.FOUR_SPACES; // 4 spaces
        }
        if (patterns.stream().anyMatch(p -> p.equals(Indentation.TAB))) {
            return Indentation.TAB; // Tab
        }
        if (patterns.stream().anyMatch(p -> p.equals(Indentation.TWO_SPACES))) {
            return Indentation.TWO_SPACES; // 2 spaces
        }

        // Fallback to the most common pattern or default
        return mostCommon.isEmpty() ? Indentation.TWO_SPACES : mostCommon;
    }

    /**
     * Calculates the index where a new element with the given name should be inserted.
     */
    private static int calcNewElementIndex(String elementName, Element parent) {
        List<String> elementOrder = ELEMENT_ORDER.get(parent.getName());
        if (elementOrder == null || elementOrder.isEmpty()) {
            return parent.getContentSize();
        }

        int targetIndex = elementOrder.indexOf(elementName);
        if (targetIndex == -1) {
            return parent.getContentSize();
        }

        // Find the position to insert based on element order
        List<Content> contents = parent.getContent();
        for (int i = contents.size() - 1; i >= 0; i--) {
            Content content = contents.get(i);
            if (content instanceof Element element) {
                int currentIndex = elementOrder.indexOf(element.getName());
                if (currentIndex != -1 && currentIndex <= targetIndex) {
                    return i + 1;
                }
            }
        }

        return 0;
    }

    /**
     * Checks if there should be a blank line between two elements.
     * This method determines spacing based on the element order configuration.
     * Empty strings in the element order indicate where blank lines should be placed.
     */
    private static boolean isBlankLineBetweenElements(
            String prependingElementName, String elementName, Element parent) {
        List<String> elementOrder = ELEMENT_ORDER.get(parent.getName());
        if (elementOrder == null || elementOrder.isEmpty()) {
            return false;
        }

        int prependingIndex = elementOrder.indexOf(prependingElementName);
        int currentIndex = elementOrder.indexOf(elementName);

        if (prependingIndex == -1 || currentIndex == -1) {
            return false;
        }

        // Check if there's an empty string between the two elements in the order
        for (int i = prependingIndex + 1; i < currentIndex; i++) {
            if (elementOrder.get(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
