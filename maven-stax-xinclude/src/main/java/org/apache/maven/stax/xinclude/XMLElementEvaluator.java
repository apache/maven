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
package org.apache.maven.stax.xinclude;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class evaluates an XPointer on a XMLElement, using the XPointer model.
 * It currently supports shorthand pointer and element() scheme based pointer part.
 * <p>
 * This class is based upon a class of the same name in Apache Woden.
 *
 * @param <T> the wrapped type
 */
abstract class XMLElementEvaluator<T> {
    private final XPointer xpointer;
    private final XMLElement<T> root;

    /**
     * Constructs a new XMLElement abstract class for a XPointer and XMLElement.
     *
     * @param xpointer an XPointer which to evaluate.
     * @param root     an XMLElement which to evaluate the XPointer against.
     */
    XMLElementEvaluator(XPointer xpointer, XMLElement<T> root) {
        this.xpointer = xpointer;
        this.root = root;
    }

    /**
     * Evaluates the XPointer on the root XMLElement and returns the resulting XMLElement or null.
     *
     * @return an XMLElement from the evaluation of the root XMLElement or null if evaluation fails.
     */
    public XMLElement<T> evaluate() {
        if (xpointer.hasPointerParts()) { // Scheme based pointer.
            // Take each pointer part at a time and evaluate it against the root element. The first result found will be
            // returned.
            XMLElement<T> result = null;
            for (PointerPart pointerPart : xpointer.getPointerParts()) {
                // TODO Add extra pointer parts here once we support them.
                if (pointerPart instanceof ElementPointerPart) {
                    result = evaluateElementPointerPart((ElementPointerPart) pointerPart);
                }
                if (result != null) {
                    return result;
                }
            }
        } else if (xpointer.hasShorthandPointer()) { // Shorthand pointer
            // Iterator for XMLElement from root in document order. See http://www.w3.org/TR/xpath#dt-document-order
            return evaluateShorthandPointer(xpointer.getShorthandPointer());
        }
        return null;
    }

    /**
     * Evaluates an element() XPointer scheme based pointer part to the specification at
     * <a href="http://www.w3.org/TR/xptr-element/">http://www.w3.org/TR/xptr-element/</a>
     *
     * @param elementPointerPart an ElementPointerPart to evaluate.
     * @return an XMLElement pointed to by this Element pointer part, or null if none exists.
     */
    private XMLElement<T> evaluateElementPointerPart(ElementPointerPart elementPointerPart) {
        if (elementPointerPart.hasChildSequence() && elementPointerPart.hasNCName()) { // Both NCName and childSequence.
            // Find NCName.
            XMLElement<T> element = evaluateShorthandPointer(elementPointerPart.getNCName());
            if (element == null) {
                return null;
            }
            // Walk through children.
            return evaluateChildSequence(element, elementPointerPart.getChildSequence());
        } else if (elementPointerPart.hasNCName()) { // Only NCName
            return evaluateShorthandPointer(elementPointerPart.getNCName());
        } else { // Only a childSequence
            // XML must only have 1 root element so we can't evaluate it if its > 1
            List<Integer> childSequence = elementPointerPart.getChildSequence();
            if (childSequence.get(0) > 1) {
                return null;
            }
            return evaluateChildSequence(root, childSequence.subList(1, childSequence.size()));
        }
    }

    /**
     * Evaluates an shorthand pointer in an XPointer based on the specification at
     * <a href="http://www.w3.org/TR/xptr-framework/#shorthand">http://www.w3.org/TR/xptr-framework/#shorthand</a>
     *
     * @param shorthand an NCName to evaluate.
     * @return an XMLElement pointed to by this shorthand name, or null if none exists.
     */
    private XMLElement<T> evaluateShorthandPointer(String shorthand) {
        // Iterator for XMLElement from root in document order. See http://www.w3.org/TR/xpath#dt-document-order
        for (Iterator<XMLElement<T>> it = new DocumentOrderIterator<>(root); it.hasNext(); ) {
            XMLElement<T> element = it.next();
            if (testElementShorthand(element, shorthand)) {
                return element;
            }
        }
        return null;
    }

    /**
     * Evaluates a child sequence array of Integers to an XMLElement following XML Document Order.
     * This is a helper method used by other evaluation methods in this class.
     *
     * @param element       an XMLElement to start from.
     * @param childSequence an Integer[] to evaluate from the start XMLElement.
     * @return an XMLElement pointed to by this childSequence, or null if none exists.
     */
    private XMLElement<T> evaluateChildSequence(XMLElement<T> element, List<Integer> childSequence) {
        for (Integer integer : childSequence) {
            // does the iTh child exist?
            List<XMLElement<T>> children = element.getChildElements();
            children = filterNoneElementNodes(children);
            if (integer > children.size()) { // childSequence int out of bounds of child array so does not exist.
                return null;
            } else {
                element = element.getChildElements().get(integer - 1);
            }
        }
        return element;
    }

    // Utility classes

    /**
     * Filters an XMLElement[] for nodes which are not xml tag elements.
     *
     * @param nodes an XMLElement[] of the nodes to filter.
     * @return an XMLElement[] of the remaining nodes.
     */
    private static <T> List<XMLElement<T>> filterNoneElementNodes(List<XMLElement<T>> nodes) {
        return nodes.stream().filter(n -> !n.getLocalName().contains("#")).collect(Collectors.toList());
    }

    // Abstract Methods

    /**
     * Tests the element for an id according to the specification at
     * <a href="http://www.w3.org/TR/xptr-framework/#term-sdi">http://www.w3.org/TR/xptr-framework/#term-sdi</a> and returns a boolean answer.
     *
     * @param element An XMLElement to test for an id.
     * @param id      A String of the id to test for.
     * @return boolean value of whether the id matches or not.
     */
    public abstract boolean testElementShorthand(XMLElement<T> element, String id);

    // Internal classes

    /**
     * DocumentOrderIterator is a implementation of Iterator which iterates in Document Order from a root XMLElement object.
     */
    private static class DocumentOrderIterator<T> implements Iterator<XMLElement<T>> {
        private final Deque<XMLElement<T>> stack;

        DocumentOrderIterator(XMLElement<T> root) {
            stack = new ArrayDeque<>();
            stack.add(root);
        }

        public boolean hasNext() {
            return !stack.isEmpty();
        }

        public XMLElement<T> next() {
            // Get next element.
            XMLElement<T> element = stack.pop();
            // Add children to top of stack in reverse order.
            List<XMLElement<T>> children = new ArrayList<>(element.getChildElements());
            Collections.reverse(children);
            stack.addAll(children);
            return element;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
