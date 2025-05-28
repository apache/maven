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

import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Parent;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;
import static org.jdom2.filter.Filters.textOnly;

/**
 * Common JDom functions
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 * @author Marc Rohlfs, CoreMedia AG
 */
public final class JDomUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JDomUtils.class);

    private JDomUtils() {
        // noop
    }

    /**
     * Adds an element as new child to the given root element. The position where the element is inserted is calculated
     * using the element order that is defined in the {@link JDomCfg} (see {@link JDomCfg#getElementOrder(String)}). When
     * no order is defined for the element, the new element is append as last element (before the closing tag of the
     * root element). In the root element, the new element is always prepended by a text element containing a linebreak
     * followed by the indentation characters. The indentation of the new element nodes will be reset to the indentation
     * that is (tried to be) detected from the root element (see {@link #detectIndentation(Element)}).
     *
     * @param element the name of the new element.
     * @param root    the root element.
     */
    public static void addElement(JDomCfg jDomCfg, Element element, Element root) {
        addElement(jDomCfg, element, root, calcNewElementIndex(jDomCfg, element.getName(), root));
    }

    /**
     * Inserts a new child element to the given root element at the given index.
     * For details see {@link #addElement(JDomCfg, Element, Element)}
     *
     * @param element the element to add.
     * @param root    the root element.
     * @param index   the index where the element should be inserted.
     */
    public static void addElement(JDomCfg jDomCfg, Element element, Element root, int index) {
        root.addContent(index, element);

        String prependingElementName = ((Element) root.getContent(max(0, index - 1))).getName();
        if (isBlankLineBetweenElements(jDomCfg, prependingElementName, element.getName(), root)) {
            root.addContent(index, new Text("\n\n" + detectIndentation(root)));
        } else {
            root.addContent(index, new Text("\n" + detectIndentation(root)));
        }

        resetIndentations(root, detectIndentation(root));
        resetIndentations(element, detectIndentation(root) + "  ");
    }

    public static int getElementIndex(Element element, Element root) {
        return root.indexOf(element);
    }

    private static int getLastElementIndex(Element root) {
        List<Element> elements = root.getContent(new ElementFilter());

        int size = elements.size();
        return size > 0 ? root.indexOf(elements.get(size - 1)) : -1;
    }

    /**
     * Inserts a new child element to the given root element. The position where the element is inserted is calculated
     * using the element order that is defined in the {@link JDomCfg} (see {@link JDomCfg#getElementOrder(String)}).
     * When no order is defined for the element, the new element is append as last element (before the closing tag of the
     * root element). In the root element, the new element is always prepended by a text element containing a linebreak
     * followed by the indentation characters. The indentation characters are (tried to be) detected from the root element
     * (see {@link #detectIndentation(Element)} ).
     *
     * @param name the name of the new element.
     * @param root the root element.
     * @return the new element.
     */
    public static Element insertNewElement(JDomCfg jDomCfg, String name, Element root) {
        return insertNewElement(jDomCfg, name, root, calcNewElementIndex(jDomCfg, name, root));
    }

    /**
     * Inserts a new child element to the given root element at the given index.
     * For details see {@link #insertNewElement(JDomCfg, String, Element)}
     *
     * @param name  the name of the new element.
     * @param root  the root element.
     * @param index the index where the element should be inserted.
     * @return the new element.
     */
    public static Element insertNewElement(JDomCfg jDomCfg, String name, Element root, int index) {
        Element newElement;

        String indent = detectIndentation(root);

        newElement = new Element(name, root.getNamespace());
        newElement.addContent("\n" + indent);
        root.addContent(index, newElement);

        String prependingElementName = ((Element) root.getContent(max(0, index - 1))).getName();
        if (isBlankLineBetweenElements(jDomCfg, prependingElementName, name, root)) {
            root.addContent(index, new Text("\n\n" + indent));
        } else {
            root.addContent(index, new Text("\n" + indent));
        }

        return newElement;
    }

    /**
     * Inserts nested elements of the given tags into jdomParent.
     *
     * @param jdomParent the parent element
     * @param tags       the names of the new elements
     * @return the innermost element
     */
    public static Element insertNewNestedElements(JDomCfg jDomCfg, Element jdomParent, String... tags) {
        for (String tag : tags) {
            jdomParent = insertNewElement(jDomCfg, tag, jdomParent);
        }
        return jdomParent;
    }

    /**
     * Inserts an element with text, like &lt;version&gt;1.2.3&lt;/version&gt;
     *
     * @param jdomParent the parent element
     * @param tag        the name of the new element
     * @param text       the content of the new element
     */
    public static void insertContentElement(JDomCfg jDomCfg, Element jdomParent, String tag, String text) {
        if (text != null) {
            Element jdomVersion = insertNewElement(jDomCfg, tag, jdomParent);
            jdomVersion.setContent(new Text(text));
        }
    }

    private static int calcNewElementIndex(JDomCfg jDomCfg, String name, Element root) {
        int addIndex = 0;

        List<String> elementOrder = jDomCfg.getElementOrder(root.getName());
        if (elementOrder == null) {
            addIndex = max(0, getLastElementIndex(root) + 1);
        } else {
            for (int i = elementOrder.indexOf(name) - 1; i >= 0; i--) {
                String addAfterElementName = elementOrder.get(i);
                if (!addAfterElementName.isEmpty()) {
                    Element addAfterElement = root.getChild(addAfterElementName, root.getNamespace());
                    if (addAfterElement != null) {
                        addIndex = root.indexOf(addAfterElement) + 1;
                        break;
                    }
                }
            }
        }

        return addIndex;
    }

    private static boolean isBlankLineBetweenElements(JDomCfg jDomCfg, String element1, String element2, Element root) {
        List<String> elementOrder = jDomCfg.getElementOrder(root.getName());
        if (elementOrder != null) {
            return elementOrder
                    .subList(elementOrder.indexOf(element1), elementOrder.indexOf(element2))
                    .contains("");
        }
        return false;
    }

    /**
     * Tries to detect the indentation that is used within the given element and returns it.
     * <p>
     * The method actually returns all characters (supposed to be whitespaces) that occur after the last linebreak in a
     * text element.
     *
     * @param element the element whose contents should be used to detect the indentation.
     * @return the detected indentation or {@code null} if not indentation can be detected.
     */
    public static String detectIndentation(Element element) {

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
                    return indent + "  ";
                }
            }
        }

        Parent parent = element.getParent();
        if (parent instanceof Element) {
            return detectIndentation((Element) parent) + "  ";
        }

        return "";
    }

    /**
     * Creates a new element with the given name. The new element has the same namespace and the same indentation (before)
     * its closing tag) like the given parent element, but is not yet attached to it.
     *
     * @param name   the name of the new e.lement
     * @param parent the parent element.
     * @return the new element.
     */
    public static Element newDetachedElement(String name, Element parent) {
        Element newElement = new Element(name, parent.getNamespace());
        newElement.addContent("\n" + detectIndentation(parent));
        return newElement;
    }

    /**
     * Returns the given elements child element with the specified name.
     *
     * @param name   the name of the child element.
     * @param parent the parent of the requested element - must not be {@code null}.
     * @return the requested element or {@code null}.
     */
    public static Element getChildElement(String name, Element parent) {
        return parent.getChild(name, parent.getNamespace());
    }

    /**
     * Returns the trimmed text value of the given elements child element with the specified name.
     *
     * @param name   the name of the child element.
     * @param parent the parent of the element whose text value is requested - must not be {@code null}.
     * @return the trimmed text value of the element or {@code null}.
     */
    public static String getChildElementTextTrim(String name, Element parent) {
        Element child = getChildElement(name, parent);
        if (child == null) {
            return null;
        } else {
            String text = child.getTextTrim();
            return "null".equals(text) ? null : text;
        }
    }

    /**
     * Remove a child element from the parent.
     *
     * @param parent      the parent element.
     * @param removeChild the child element to be removed.
     */
    public static void removeChildElement(Element parent, Element removeChild) {
        int index = parent.indexOf(removeChild);
        parent.removeContent(index--);
        if (index >= 0 && parent.getContent(index) instanceof Text) {
            // Remove prepending whitespaces (linebreaks and indentation)
            parent.removeContent(index);
        }
        removeChild.detach();
    }

    /**
     * Remove a child of type {@link Content} and its attached comments from the parent.
     *
     * @param parent      the parent element.
     * @param removeChild the child content to be removed.
     */
    public static void removeChildAndItsCommentFromContent(Element parent, Content removeChild) {
        int index = parent.indexOf(removeChild);
        if (index >= 0) {
            LOG.debug("");
            LOG.debug("index [{}] => REMOVE: {}", index, JDomContentHelper.contentAsString(parent.getContent(index)));
            parent.removeContent(index);
            index--;
            Content elementToCheck = JDomContentHelper.getContentWithIndex(index, parent);
            // remove new line
            if (JDomContentHelper.isNewline(elementToCheck) && simpleRemoveAtIndex(index, parent)) {
                int newIndex = index - 1;
                int prevIndex;
                do {
                    prevIndex = newIndex;
                    newIndex = removeContentAtIndexIfContentIsComment(newIndex, parent);
                } while (newIndex >= 0 && newIndex != prevIndex);
            } else if (JDomContentHelper.isMultiNewLine(elementToCheck)) {
                removeFirstNewLineFromMultiline(parent.indexOf(elementToCheck), parent);
            }
            // Now detach removed child
            removeChild.detach();
        }
    }

    /**
     * Remove and detach content at index from parent.
     *
     * @param index  the index of the content
     * @param parent the parent of the content
     */
    private static boolean simpleRemoveAtIndex(int index, Element parent) {
        if (!JDomContentHelper.isIndexValid(index, parent)) {
            return false;
        }
        Content contentToRemove = parent.getContent(index);
        LOG.debug(
                "remove content => {} from parent tag: <{}>",
                JDomContentHelper.contentAsString(contentToRemove),
                parent.getName());
        parent.removeContent(index);
        contentToRemove.detach();
        return true;
    }

    /**
     * Remove and detach content.
     *
     * @param content the content to remove
     */
    static void simpleRemoveAtIndex(Content content) {
        Element parent = content.getParentElement();
        int index = parent.indexOf(content);
        Content contentToRemove = parent.getContent(index);
        LOG.debug(
                "remove content => {} from parent tag: <{}>",
                JDomContentHelper.contentAsString(contentToRemove),
                parent.getName());
        parent.removeContent(index);
        contentToRemove.detach();
    }

    /**
     * Remove first newline from multiline
     *
     * @param index  the index of the multiline content
     * @param parent the parent of the multiline content
     */
    private static void removeFirstNewLineFromMultiline(int index, Element parent) {
        if (!JDomContentHelper.isIndexValid(index, parent)) {
            return;
        }
        Content contentToRemove = parent.getContent(index);
        LOG.debug("       Content to remove  : {}", JDomContentHelper.contentAsString(contentToRemove));

        // Remove first newline
        String text = contentToRemove.getValue().replaceFirst("\n", "");

        // Remove indentation if
        // * predecessor has no newlines or
        // * successor has newlines
        Content predecessor = JDomContentHelper.getPredecessorOfContentWithIndex(index, parent);
        Content successor = JDomContentHelper.getSuccessorOfContentWithIndex(index, parent);
        if (JDomContentHelper.hasNewlines(successor)
                || (predecessor != null && !JDomContentHelper.hasNewlines(predecessor))) {
            // remove indentation
            text = text.replaceAll(" ", "");
            LOG.debug("       Replaced intention : {}", text);
        }

        // Remove multiline content
        simpleRemoveAtIndex(index, parent);

        // Add new Text content
        Text replacement = new Text(text);
        parent.addContent(index, replacement);
        LOG.debug("       Content replacement: {}", JDomContentHelper.contentAsString(replacement));
    }

    /**
     * Remove comment at index position from parent.<br>
     * The return value is:
     * <ul>
     * <li><b>-1</b>: Index is not valid</li>
     * <li><b>-2</b>: Comment has been removed but there is no predecessor</li>
     * <li><b>-3</b>: Comment has been removed but the predecessor is a multi newline (whose first line is removed as well and its indentation - if necessary)</li>
     * <li><b>index</b>: The same index if content at this index is no comment</li>
     * <li><b>index - 1</b>: Content at the given index is a comment and there is no new newline predecessor</li>
     * <li><b>index - 2</b>: Content at the given index is a comment and there is one new newline predecessor (which is removed as well)</li>
     * </ul>
     *
     * @param index  the index of the content to check
     * @param parent the parent of the content
     * @return int the new index
     */
    private static int removeContentAtIndexIfContentIsComment(int index, Element parent) {
        if (!JDomContentHelper.isIndexValid(index, parent)) {
            return -1;
        }
        Content content = parent.getContent(index);
        if (!JDomContentHelper.isComment(content)) {
            LOG.debug("Content at index {} is no comment", index);
            return index;
        }

        // remove comment
        simpleRemoveAtIndex(index, parent);

        // get predecessor
        int prevIndex = index - 1;
        if (prevIndex < 0) {
            return -2;
        }
        Content predecessor = JDomContentHelper.getPredecessorOfContentWithIndex(index, parent);
        if (JDomContentHelper.isNewline(predecessor)) {
            simpleRemoveAtIndex(prevIndex, parent);
            return prevIndex - 1;
        } else if (JDomContentHelper.isMultiNewLine(predecessor)) {
            removeFirstNewLineFromMultiline(prevIndex, parent);
            return -3;
        }
        return prevIndex;
    }

    /**
     * Resets the XML indentations of an element.
     *
     * @param element the element whose indentations should be reset.
     * @param indent  the indentation to be used.
     */
    public static void resetIndentations(Element element, String indent) {
        List<Content> childContents = element.getContent();
        for (int i = 1; i < childContents.size(); i++) {
            Content childContent = childContents.get(i);
            if (childContent instanceof Element) {
                Element childElement = (Element) childContent;

                // Reset indentations of child elements.
                resetIndentation(childContents.get(i - 1), indent);

                // Reset indentations of before closing tags of child elements.
                List<Content> grandChildElements = childElement.getContent();
                if (grandChildElements.size() > 1) {
                    resetIndentation(grandChildElements.get(grandChildElements.size() - 1), indent);
                }
            }
        }
    }

    private static void resetIndentation(Content whitespaceContentBeforeElement, String indent) {
        if (whitespaceContentBeforeElement instanceof Text) {
            Text whitespaceTextContent = (Text) whitespaceContentBeforeElement;
            String whitespaces = whitespaceTextContent.getText();
            int lastLsIndex = StringUtils.lastIndexOfAny(whitespaces, new String[] {"\n", "\r"});
            whitespaceTextContent.setText("\n" + whitespaces.substring(0, lastLsIndex) + indent);
        }
    }

    /**
     * Updates the text value of the given element. The primary purpose of this method is to preserve any whitespace and
     * comments around the original text value.
     *
     * @param element The element to update, must not be <code>null</code>.
     * @param value   The text string to set, must not be <code>null</code>.
     */
    public static void rewriteValue(Element element, String value) {
        Text text = null;
        if (element.getContent() != null) {
            for (Iterator<?> it = element.getContent().iterator(); it.hasNext(); ) {
                Object content = it.next();
                if ((content instanceof Text) && !((Text) content).getTextTrim().isEmpty()) {
                    text = (Text) content;
                    while (it.hasNext()) {
                        content = it.next();
                        if (content instanceof Text) {
                            text.append((Text) content);
                            it.remove();
                        } else {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (text == null) {
            element.addContent(value);
        } else {
            String chars = text.getText();
            String trimmed = text.getTextTrim();
            int idx = chars.indexOf(trimmed);
            String leadingWhitespace = chars.substring(0, idx);
            String trailingWhitespace = chars.substring(idx + trimmed.length());
            text.setText(leadingWhitespace + value + trailingWhitespace);
        }
    }

    public static Element rewriteElement(JDomCfg jDomCfg, String name, String value, Element root) {
        Element tagElement = root.getChild(name, root.getNamespace());
        if (tagElement != null) {
            if (value != null) {
                rewriteValue(tagElement, value);
            } else {
                JDomUtils.removeChildAndItsCommentFromContent(root, tagElement);
            }
        } else {
            if (value != null) {
                Element element = insertNewElement(jDomCfg, name, root);
                element.setText(value);
                tagElement = element;
            }
        }
        return tagElement;
    }
}
