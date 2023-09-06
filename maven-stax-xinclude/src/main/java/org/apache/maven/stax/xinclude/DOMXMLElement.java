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
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class is based upon a class of the same name in Apache Woden.
 */
class DOMXMLElement implements XMLElement<Element> {

    private Element fSource;

    DOMXMLElement() {}

    DOMXMLElement(Element fSource) {
        this.fSource = fSource;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.XMLElement#getSource()
     */
    public final Element getSource() {
        return fSource;
    }

    /*
     * @see org.apache.woden.XMLElement#setSource(java.lang.Object)
     */
    public void setSource(Element elem) {
        fSource = elem;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.XMLElement#getLocalName()
     */
    public final String getLocalName() {
        return fSource != null ? doGetLocalName() : null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.XMLElement#getNextSiblingElement()
     */
    public final XMLElement<Element> getNextSiblingElement() {
        return fSource != null ? doGetNextSiblingElement() : null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.XMLElement#getChildElements()
     */
    public final List<XMLElement<Element>> getChildElements() {
        return fSource != null ? doGetChildElements() : null;
    }

    protected String doGetLocalName() {
        return fSource.getLocalName();
    }

    protected XMLElement<Element> doGetFirstChildElement() {
        for (Node node = fSource.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                return new DOMXMLElement((Element) node);
            }
        }
        return null; // no child element found
    }

    protected XMLElement<Element> doGetNextSiblingElement() {
        for (Node node = fSource.getNextSibling(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                return new DOMXMLElement((Element) node);
            }
        }
        return null; // no sibling element found
    }

    protected List<XMLElement<Element>> doGetChildElements() {
        List<XMLElement<Element>> children = new ArrayList<>();
        XMLElement<Element> temp = doGetFirstChildElement();
        while (temp != null) {
            children.add(temp);
            temp = temp.getNextSiblingElement();
        }
        return children;
    }
}
