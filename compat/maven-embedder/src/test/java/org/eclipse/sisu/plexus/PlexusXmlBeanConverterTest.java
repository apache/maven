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
package org.eclipse.sisu.plexus;

import java.util.Set;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlexusXmlBeanConverterTest {

    private static final String XML = "<configuration mode='strict'><value>  text  </value><empty/></configuration>";

    @Test
    void convertsXmlNode() {
        XmlNode node = assertInstanceOf(XmlNode.class, newConverter().convert(TypeLiteral.get(XmlNode.class), XML));

        assertEquals("configuration", node.getName());
        assertEquals("strict", node.getAttribute("mode"));
        assertEquals("text", node.getChild("value").getValue());
        assertNull(node.getChild("empty").getValue());
    }

    @Test
    void convertsXpp3Dom() {
        Xpp3Dom dom = assertInstanceOf(Xpp3Dom.class, newConverter().convert(TypeLiteral.get(Xpp3Dom.class), XML));

        assertEquals("configuration", dom.getName());
        assertEquals("strict", dom.getAttribute("mode"));
        assertEquals("text", dom.getChild("value").getValue());
        assertNull(dom.getChild("empty").getValue());
    }

    private static PlexusXmlBeanConverter newConverter() {
        Injector injector = mock(Injector.class);
        when(injector.getTypeConverterBindings()).thenReturn(Set.of());
        return new PlexusXmlBeanConverter(injector);
    }
}
