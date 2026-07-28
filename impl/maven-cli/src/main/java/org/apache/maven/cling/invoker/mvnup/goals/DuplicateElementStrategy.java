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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

/**
 * Strategy for removing duplicate XML elements in POM files.
 *
 * <p>Maven 4's stricter POM parser rejects duplicate XML elements (such as
 * {@code <artifactId>}, {@code <properties>}, {@code <version>}, etc.) that
 * Maven 3 silently accepted using last-wins semantics. This strategy scans
 * each POM element's children and removes duplicates, keeping only the last
 * occurrence of each element name.
 *
 * <p>This strategy only targets "scalar" elements — elements that should appear
 * at most once within their parent according to the Maven POM schema. Elements
 * inside list containers (e.g., {@code <dependency>} inside {@code <dependencies>},
 * {@code <plugin>} inside {@code <plugins>}) are not affected by this strategy
 * since duplicate dependencies and plugins are handled by
 * {@link DeduplicateDependenciesStrategy}.
 *
 * @see <a href="https://github.com/apache/maven/issues/12530">#12530</a>
 */
@Named
@Singleton
@Priority(21)
public class DuplicateElementStrategy extends AbstractUpgradeStrategy {

    /**
     * Parent element names whose children are expected to repeat (list containers).
     * These are skipped when checking for duplicate children since their child
     * elements are naturally repeated (e.g., multiple {@code <dependency>} in
     * {@code <dependencies>}).
     */
    static final Set<String> LIST_CONTAINER_ELEMENTS = Set.of(
            "dependencies",
            "plugins",
            "modules",
            "subprojects",
            "profiles",
            "repositories",
            "pluginRepositories",
            "extensions",
            "exclusions",
            "executions",
            "resources",
            "testResources",
            "notifiers",
            "contributors",
            "developers",
            "licenses",
            "mailingLists",
            "goals",
            "otherArchives",
            "includes",
            "excludes",
            "filters",
            "roles",
            "reports");

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Removing duplicate XML elements";
    }

    @Override
    protected UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking for duplicate XML elements)");
            context.indent();

            try {
                boolean hasIssues = removeDuplicateElements(pomDocument.root(), context);

                if (hasIssues) {
                    context.success("Duplicate XML elements removed");
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No duplicate XML elements found");
                }
            } catch (Exception e) {
                context.failure("Failed to remove duplicate XML elements: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Recursively scans an element's children for duplicates and removes them.
     * Uses last-wins semantics (consistent with Maven 3's behavior).
     *
     * @param element the element to scan
     * @param context the upgrade context for logging
     * @return true if any duplicates were removed
     */
    private boolean removeDuplicateElements(Element element, UpgradeContext context) {
        boolean removed = false;

        // Skip list container elements — their children naturally repeat
        if (LIST_CONTAINER_ELEMENTS.contains(element.name())) {
            // Still recurse into each child to check for duplicates within them
            for (Element child : element.childElements().toList()) {
                removed |= removeDuplicateElements(child, context);
            }
            return removed;
        }

        // Collect children, tracking last occurrence of each name
        List<Element> children = element.childElements().toList();
        Map<String, Element> lastSeen = new HashMap<>();
        List<Element> duplicates = new ArrayList<>();

        for (Element child : children) {
            String name = child.name();
            Element previous = lastSeen.put(name, child);
            if (previous != null) {
                // Previous occurrence is the duplicate (last-wins)
                duplicates.add(previous);
            }
        }

        // Remove duplicates
        for (Element duplicate : duplicates) {
            String elementPath = buildElementPath(element);
            context.detail("Removed duplicate <" + duplicate.name() + "> element in " + elementPath);
            DomUtils.removeElement(duplicate);
            removed = true;
        }

        // Recurse into surviving children
        List<Element> survivingChildren = element.childElements().toList();
        for (Element child : survivingChildren) {
            removed |= removeDuplicateElements(child, context);
        }

        return removed;
    }

    /**
     * Builds a human-readable path for the element for logging purposes.
     */
    private String buildElementPath(Element element) {
        List<String> segments = new ArrayList<>();
        Element current = element;
        while (current != null) {
            segments.add(0, current.name());
            current = current.parent() instanceof Element p ? p : null;
        }
        return String.join("/", segments);
    }
}
