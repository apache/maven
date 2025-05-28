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
package org.apache.maven.cling.invoker.mvnup.jdom;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;

/**
 * JDom cleanup methods for:
 * <ol>
 * <li>profiles</li>
 * </ol>
 */
public class JDomPomCleanupHelper {

    /**
     * Generic cleanup useful after removal of things.
     */
    public static void cleanup(Element rootElement) {
        // Remove empty elements
        for (String cleanUpEmptyElement : List.of(
                JDomPomCfg.POM_ELEMENT_MODULES,
                JDomPomCfg.POM_ELEMENT_PROPERTIES,
                JDomPomCfg.POM_ELEMENT_PLUGINS,
                JDomPomCfg.POM_ELEMENT_PLUGIN_MANAGEMENT,
                JDomPomCfg.POM_ELEMENT_DEPENDENCIES,
                JDomPomCfg.POM_ELEMENT_DEPENDENCY_MANAGEMENT)) {
            JDomPomCleanupHelper.cleanupEmptyElements(rootElement, cleanUpEmptyElement);
        }
        // Remove empty (i.e. with no elements) profile and profiles tag
        JDomPomCleanupHelper.cleanupEmptyProfiles(rootElement, List.of(JDomPomCfg.POM_ELEMENT_PROJECT));
    }

    /**
     * Remove all empty profiles and profile tags.<br>
     * Empty is defined as
     * <ul>
     * <li>for tag <b>profiles</b>: there are no profile tags</li>
     * <li>for tag <b>profile</b>: Either there are only the tags activation and id present or all other tags are empty</li>
     * </ul>
     * Thus, empty tags may contain comments which will be removed as well.<br>
     * Only those {@code profiles} tags are removed whose parent stated via {@code profilesParents}
     *
     * @param rootElement     the root element.
     * @param profilesParents list of allowed parents for {@code profiles} tags
     */
    public static void cleanupEmptyProfiles(Element rootElement, List<String> profilesParents) {
        IteratorIterable<Element> filteredElements =
                rootElement.getDescendants(new ElementFilter(JDomPomCfg.POM_ELEMENT_PROFILES));
        List<Element> profiles = new ArrayList<>();
        for (Element profilesElement : filteredElements) {
            profiles.add(profilesElement);
        }
        for (Element profilesElement : profiles) {
            if (!profilesParents.contains(profilesElement.getParentElement().getName())) {
                continue;
            }
            removeElementWithEmptyChildren(
                    profilesElement,
                    JDomPomCfg.POM_ELEMENT_PROFILE,
                    Arrays.asList(JDomPomCfg.POM_ELEMENT_ID, JDomPomCfg.POM_ELEMENT_ACTIVATION));
            if (!profilesElement
                    .getDescendants(new ElementFilter(JDomPomCfg.POM_ELEMENT_PROFILE))
                    .hasNext()) {
                JDomUtils.removeChildAndItsCommentFromContent(profilesElement.getParentElement(), profilesElement);
            }
        }
    }

    /**
     * Remove empty element tags and their empty child tags.<br>
     * Empty tags may contain comments which will be removed as well.
     *
     * @param rootElement the root element.
     * @param tag         Tag to check.
     */
    public static void cleanupEmptyElements(Element rootElement, String tag) {
        IteratorIterable<Element> filteredElements = rootElement.getDescendants(new ElementFilter(tag));
        List<Element> elementsToRemoveIfEmpty = new ArrayList<>();
        for (Element element : filteredElements) {
            elementsToRemoveIfEmpty.add(element);
        }
        for (Element elementToRemove : elementsToRemoveIfEmpty) {
            List<Element> children = elementToRemove.getChildren();
            if (children.isEmpty()) {
                JDomUtils.removeChildAndItsCommentFromContent(elementToRemove.getParentElement(), elementToRemove);
            }
        }
    }

    /**
     * Squash multiple consecutive newlines into a single newline.<br>
     * Indentations are preserved.
     *
     * @param rootElement the root element.
     */
    public static void squashMultilines(Element rootElement) {
        // Compute groups of consecutive sibling content with only newlines (and whitespace)
        List<List<Text>> newLineGroups = new ArrayList<>();
        List<Text> currentGroup = new ArrayList<>();
        for (Content descendant : rootElement.getDescendants()) {
            if (JDomContentHelper.hasNewlines(descendant)) {
                if (!currentGroup.isEmpty()) {
                    Element parent = currentGroup.get(0).getParent();
                    if (!parent.equals(descendant.getParentElement())) {
                        newLineGroups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                    }
                }
                currentGroup.add((Text) descendant);
            } else {
                if (!currentGroup.isEmpty()) {
                    newLineGroups.add(currentGroup);
                    currentGroup = new ArrayList<>();
                }
            }
        }
        if (!currentGroup.isEmpty()) {
            newLineGroups.add(currentGroup);
        }

        // For every group keep the last element (because it might be followed by whitespace which we want to keep for
        // indentation)
        // and set its text to two newlines (+ whitespace).
        // Delete all other predecessor elements in the group.
        for (List<Text> group : newLineGroups) {
            int newlineCount = 0;
            for (Text text : group) {
                newlineCount += StringUtils.countMatches(text.getText(), "\n");
            }

            if (newlineCount > 2) {
                Text last = group.get(group.size() - 1);
                last.setText("\n\n" + last.getText().replaceAll("\n", ""));
                group.remove(last);
                for (Text text : group) {
                    text.getParentElement().removeContent(text);
                    text.detach();
                }
            }
        }
    }

    /**
     * Remove all empty children with tag name from parent element.<br>
     * The child is considered as empty if the child has either:
     * <ul>
     * <li>no children itself or</li>
     * <li>only children which are ignored or</li>
     * <li>empty children itself which are not ignored</li>
     * </ul>
     *
     * @param parent         the parent element.
     * @param tagName        the tag name to search for.
     * @param ignoreChildren List of children tag names which are ignored when searching for child elements.
     */
    private static void removeElementWithEmptyChildren(Element parent, String tagName, List<String> ignoreChildren) {
        // Example:
        // parent === 'profiles' Element
        // tagName === 'profile'
        // ignoreChildren === ['id', 'activation']

        // filteredElements === direct children of parent which are of type Element and matching 'tagName'
        IteratorIterable<Element> filteredElements = parent.getDescendants(new ElementFilter(tagName));

        List<Content> contentToBeRemoved = new ArrayList<>();
        for (Element filteredElement : filteredElements) {
            boolean empty = true;
            for (Element child : filteredElement.getChildren()) {
                if (!ignoreChildren.contains(child.getName())
                        && !child.getChildren().isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                contentToBeRemoved.add(filteredElement);
                contentToBeRemoved.addAll(getAttachedComments(filteredElement));
            }
        }
        for (Content elementToBeRemoved : contentToBeRemoved) {
            JDomUtils.removeChildAndItsCommentFromContent(parent, elementToBeRemoved);
        }
    }

    /**
     * Get all comments attached of the element.
     *
     * @param element the element to consider.
     * @return List of {@link Comment}s
     */
    private static List<Content> getAttachedComments(Element element) {
        List<Content> contents = new ArrayList<>();
        List<Content> siblings = getDirectContents(element.getParentElement());
        int indexOfElement = siblings.indexOf(element);
        for (int i = indexOfElement - 1; i >= 0; i--) {
            if (JDomContentHelper.isNewline(siblings.get(i))) {
                contents.add(siblings.get(i));
                i--;
            }
            if (i >= 0 && siblings.get(i) instanceof Comment) {
                contents.add(siblings.get(i));
                continue;
            }
            if (i >= 0 && JDomContentHelper.isMultiNewLine(siblings.get(i))) {
                contents.add(siblings.get(i));
            }
            break;
        }
        return contents;
    }

    /**
     * Get all direct children of the element.
     *
     * @param element the element to consider.
     * @return List of {@link Content}s
     */
    private static List<Content> getDirectContents(Element element) {
        // get all direct children
        List<Content> children = new ArrayList<>();
        Element parentElement = element.getParentElement();
        if (null == parentElement) {
            return children;
        }
        for (Content descendant : parentElement.getDescendants()) {
            if (!descendant.getParent().equals(element)) {
                // Only consider direct children
                continue;
            }
            children.add(descendant);
        }
        return children;
    }
}
