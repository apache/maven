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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * XPointer is a class which represents an XPointer defined in the XPointer Framework.
 * This is specified at <a href="http://www.w3.org/TR/xptr-framework/">http://www.w3.org/TR/xptr-framework/</a>
 * <p>
 * This class is based upon a class of the same name in Apache Woden.
 */
class XPointer {
    private static final String NS_URI_XML = "http://www.w3.org/XML/1998/namespace";
    private static final String NS_URI_XMLNS = "http://www.w3.org/2000/xmlns/";
    private static final String NS_PREFIX_XMLNS = "xmlns";

    private final Map<String, String> prefixBindingContex;
    private final Map<String, String> namespaceBindingContex;
    private String shorthandPointer;
    private final List<PointerPart> pointerParts;

    /**
     * Constructs a new XPointer.
     */
    XPointer() {
        pointerParts = new ArrayList<>();
        shorthandPointer = "";

        // Setup prefix/namespace binding context.
        prefixBindingContex = new HashMap<>();
        namespaceBindingContex = new HashMap<>();
        addPrefixNamespaceBinding("xml", NS_URI_XML);
    }

    /**
     * Constructs a new XPointer from the serialised string.
     *
     * @param xpointerString a String form of the XPointer to deserialise
     */
    XPointer(String xpointerString) throws InvalidXPointerException {
        this(); // Construct a new XPointer.
        if (xpointerString == null || xpointerString.isEmpty()) {
            throw new InvalidXPointerException("The XPointer string is either null or empty", "");
        }
        XPointerParser.parseXPointer(
                xpointerString, this); // Parse the string and add the Pointers to the new XPointer.
    }

    /**
     * Appends a pointer part to the end of this XPointer.
     *
     * @param pointerPart the Pointer Part to append
     * @throws UnsupportedOperationException if a Shorthand Pointer is already set
     */
    public void addPointerPart(PointerPart pointerPart) {
        if (!shorthandPointer.isEmpty()) {
            throw new UnsupportedOperationException("A Shortname Pointer already exists for this XPointer.");
        } else {
            pointerParts.add(pointerPart);
        }
    }

    /**
     * Returns the pointer parts in this XPointer.
     *
     * @return a PointerPart[] of type Object[] containing the pointer parts in this XPointer
     * @throws IllegalStateException if this XPointer has a shorthand pointer
     */
    public List<PointerPart> getPointerParts() {
        if (hasPointerParts()) {
            return Collections.unmodifiableList(pointerParts);
        } else {
            throw new IllegalStateException("This XPointer has a shorthand pointer.");
        }
    }

    /**
     * Sets the Shorthand Pointer of this XPointer to the NCName given as an argument.
     *
     * @param shorthandPointer an NCName of the Shorthand Pointer to set
     * @throws UnsupportedOperationException is a PointerPart Pointer is already set
     */
    public void setShorthandPointer(String shorthandPointer) {
        if (hasPointerParts()) {
            throw new UnsupportedOperationException("A PointerPart Pointer already exists for this XPointer");
        }
        if (shorthandPointer == null) {
            throw new NullPointerException("The shorthandPointer argument is null");
        }

        this.shorthandPointer = shorthandPointer;
    }

    /**
     * Returns the shorthandPointer in this XPointer.
     *
     * @return an NCName containing the shorthand pointer for this XPointer
     * @throws IllegalStateException if this XPointer has a shorthand pointer
     */
    public String getShorthandPointer() {
        if (hasShorthandPointer()) {
            return shorthandPointer;
        } else {
            throw new IllegalStateException("This XPointer has scheme based pointers.");
        }
    }

    /**
     * Adds a Prefix/Namespace binding to this XPointers contex.
     *
     * @param prefix    a NCName of the prefix too bind to the namespace
     * @param namespace a String of the namespace to bind to the prefix
     * @throws NullPointerException     if the prefix or namespace arguments are null
     * @throws IllegalArgumentException if the prefix or namespace are invalid as specified at <a href="http://www.w3.org/TR/xptr-framework/#nsContext">http://www.w3.org/TR/xptr-framework/#nsContext</a>
     */
    public void addPrefixNamespaceBinding(String prefix, String namespace) {
        if (prefix == null) {
            throw new NullPointerException("The prefix argument provided has a null pointer.");
        } else if (namespace == null) {
            throw new NullPointerException("The namespace argument provided has a null pointer.");
        } else if (prefix.equals(NS_PREFIX_XMLNS)) {
            throw new IllegalArgumentException("The xmlns prefix must not be bound to any namespace.");
        } else if (namespace.equals(NS_URI_XMLNS)) {
            throw new IllegalArgumentException("The " + NS_URI_XMLNS + " namespace must not be bound to any prefix.");
        } else {
            // It's a valid binding so add it to the binding contex.
            prefixBindingContex.put(prefix, namespace);
            namespaceBindingContex.put(namespace, prefix);
        }
    }

    /**
     * Gets the Namespace the Prefix is bound to if the binding exists,
     * otherwise it will return null.
     *
     * @param prefix a NCName of the prefix bound to the namespace
     * @return a String of the namespace bound to this prefix or null if none exists
     */
    public String getPrefixBinding(String prefix) {
        return prefixBindingContex.get(prefix);
    }

    /**
     * Gets Prefix the Namespace is bound to if the binding exists,
     * otherwise it will return null.
     *
     * @param namespace a String of the prefix bound to the prefix
     * @return a NCName of the prefix bound to this namespace or null if none exists
     */
    public String getNamespaceBinding(String namespace) {
        return namespaceBindingContex.get(namespace);
    }

    /**
     * Checks whether a prefix is bound or not.
     *
     * @param prefix a NCName of the prefix to check
     * @return a boolean value that is true if the binding exists, or false otherwise
     */
    public boolean hasPrefixBinding(String prefix) {
        return prefixBindingContex.containsKey(prefix);
    }

    /**
     * Checks whether a namespace is bound or not.
     *
     * @param namespace a String of the namespace to check
     * @return a boolean value that is true if the binding exists, or false otherwise
     */
    public boolean hasNamespaceBinding(String namespace) {
        return namespaceBindingContex.containsKey(namespace);
    }

    /**
     * Tests whether this XPointer has a shorthand pointer or not.
     *
     * @return a boolean which is true if this XPointer contains a shorthand pointer, false otherwise
     */
    public boolean hasShorthandPointer() {
        return !shorthandPointer.isEmpty();
    }

    /**
     * Tests whether this XPointer has scheme based pointers or not.
     *
     * @return a boolean which is true if this XPointer contains scheme based pointers, false otherwise
     */
    public boolean hasPointerParts() {
        return !pointerParts.isEmpty();
    }

    /**
     * Returns a String serialisation of this XPointer.
     *
     * @return a String containing the serialisation of this XPointer
     */
    public String toString() {
        if (shorthandPointer.isEmpty()) {
            return pointerParts.stream().map(PointerPart::toString).collect(Collectors.joining());
        } else {
            return shorthandPointer;
        }
    }
}
