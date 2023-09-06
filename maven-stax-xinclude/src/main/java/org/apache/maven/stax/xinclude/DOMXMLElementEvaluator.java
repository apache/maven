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

import java.util.Map;
import java.util.Objects;

import com.ctc.wstx.dtd.DTDAttribute;
import com.ctc.wstx.dtd.DTDElement;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.util.PrefixedName;
import org.w3c.dom.Element;

/**
 * This class extends the XMLElementEvaluator to support the DOM implementation in XMLElement.
 * <p>
 * This class is based upon a class of the same name in Apache Woden.
 */
class DOMXMLElementEvaluator extends XMLElementEvaluator<Element> {

    private DTDSubset dtd;

    /**
     * Constructs a new DOMXMLElementEvaluator to evaluate a XPointer on a DOM Element.
     *
     * @param xpointer an XPointer to evaluate
     * @param element an DOM Element to be evaluated
     */
    DOMXMLElementEvaluator(XPointer xpointer, Element element, DTDSubset dtd) {
        super(xpointer, createXMLElement(element));
        this.dtd = dtd;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.internal.xpointer.XMLElementEvaluator#testElementShorthand(org.apache.woden.XMLElement, java.lang.String)
     */
    public boolean testElementShorthand(XMLElement<Element> element, String shorthand) {
        // Simple http://www.w3.org/TR/xml-id/ support for now until we support full scheme based ID's.
        Element domElement = element.getSource();
        String attr = domElement.getAttributeNS("http://www.w3.org/XML/1998/namespace", "id");
        if (Objects.equals(attr, shorthand)) {
            return true;
        }
        if (dtd != null) {
            Map<PrefixedName, DTDElement> map = dtd.getElementMap();
            if (map != null) {
                DTDElement dtdElement = map.get(new PrefixedName(domElement.getPrefix(), domElement.getLocalName()));
                if (dtdElement != null) {
                    DTDAttribute dtdAttribute = dtdElement.getIdAttribute();
                    if (dtdAttribute != null) {
                        attr = domElement.getAttribute(dtdAttribute.getName().getLocalName());
                        if (Objects.equals(attr, shorthand)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Evaluates the XPointer on the root Element and returns the resulting Element or null.
     *
     * @return an Element from the resultant evaluation of the root Element or null if evaluation fails
     */
    public Element evaluateElement() {
        XMLElement<Element> element = evaluate();
        if (element != null) {
            return element.getSource();
        }
        return null;
    }

    // Private methods
    private static XMLElement<Element> createXMLElement(Element element) {
        DOMXMLElement domXMLElement = new DOMXMLElement();
        domXMLElement.setSource(element);
        return domXMLElement;
    }
}
