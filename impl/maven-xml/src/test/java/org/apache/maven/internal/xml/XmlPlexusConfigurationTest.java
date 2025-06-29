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
package org.apache.maven.internal.xml;

import javax.xml.stream.XMLStreamException;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class XmlPlexusConfigurationTest {

    private XmlNode createTestXmlNode() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("attr1", "value1");
        attributes.put("attr2", "value2");

        XmlNode child1 = XmlNode.newInstance("child1", "child1Value", null, null, null);
        XmlNode child2 = XmlNode.newInstance("child2", "child2Value", null, null, null);
        XmlNode child3 = XmlNode.newInstance("child1", "anotherChild1Value", null, null, null);

        return XmlNode.newInstance("root", "rootValue", attributes, List.of(child1, child2, child3), null);
    }

    private XmlNode parseXml(String xml) throws XMLStreamException {
        return XmlService.read(new StringReader(xml));
    }

    @Test
    void testBasicProperties() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        assertEquals("root", config.getName());
        assertEquals("rootValue", config.getValue());
        assertEquals("rootValue", config.getValue("default"));
        assertEquals("rootValue", config.getValue("default")); // Should return actual value, not default
    }

    @Test
    void testAttributes() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        String[] attributeNames = config.getAttributeNames();
        assertEquals(2, attributeNames.length);

        assertEquals("value1", config.getAttribute("attr1"));
        assertEquals("value2", config.getAttribute("attr2"));
        assertNull(config.getAttribute("nonexistent"));

        assertEquals("value1", config.getAttribute("attr1", "default"));
        assertEquals("default", config.getAttribute("nonexistent", "default"));
    }

    @Test
    void testChildren() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        assertEquals(3, config.getChildCount());

        PlexusConfiguration[] children = config.getChildren();
        assertEquals(3, children.length);
        assertEquals("child1", children[0].getName());
        assertEquals("child2", children[1].getName());
        assertEquals("child1", children[2].getName());

        PlexusConfiguration child1 = config.getChild("child1");
        assertNotNull(child1);
        assertEquals("anotherChild1Value", child1.getValue()); // Returns the last child with this name

        PlexusConfiguration child2 = config.getChild("child2");
        assertNotNull(child2);
        assertEquals("child2Value", child2.getValue());

        PlexusConfiguration nonexistent = config.getChild("nonexistent");
        assertNotNull(nonexistent); // Should return empty configuration, not null
        assertEquals("nonexistent", nonexistent.getName());
        assertNull(nonexistent.getValue()); // Empty configuration has null value
        assertEquals(0, nonexistent.getChildCount()); // Empty configuration has no children

        // Test getChild with createChild=false should return null for non-existent child
        PlexusConfiguration nonexistentWithFalse = config.getChild("nonexistent", false);
        assertNull(nonexistentWithFalse);
    }

    @Test
    void testGetChildrenByName() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        PlexusConfiguration[] child1s = config.getChildren("child1");
        assertEquals(2, child1s.length);
        assertEquals("child1Value", child1s[0].getValue());
        assertEquals("anotherChild1Value", child1s[1].getValue());

        PlexusConfiguration[] child2s = config.getChildren("child2");
        assertEquals(1, child2s.length);
        assertEquals("child2Value", child2s[0].getValue());

        PlexusConfiguration[] nonexistent = config.getChildren("nonexistent");
        assertEquals(0, nonexistent.length);
    }

    @Test
    void testGetChildByIndex() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        PlexusConfiguration child0 = config.getChild(0);
        assertNotNull(child0);
        assertEquals("child1", child0.getName());

        PlexusConfiguration child1 = config.getChild(1);
        assertNotNull(child1);
        assertEquals("child2", child1.getName());

        PlexusConfiguration child2 = config.getChild(2);
        assertNotNull(child2);
        assertEquals("child1", child2.getName());

        PlexusConfiguration outOfBounds = config.getChild(10);
        assertNull(outOfBounds);

        PlexusConfiguration negative = config.getChild(-1);
        assertNull(negative);
    }

    @Test
    void testWriteOperations() {
        XmlNode xmlNode = createTestXmlNode();
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        // Test setName
        config.setName("newRoot");
        assertEquals("newRoot", config.getName());
        assertEquals("rootValue", config.getValue()); // Value should be preserved

        // Test setValue
        config.setValue("newValue");
        assertEquals("newValue", config.getValue());
        assertEquals("newRoot", config.getName()); // Name should be preserved

        // Test setValueAndGetSelf
        PlexusConfiguration self = config.setValueAndGetSelf("anotherValue");
        assertSame(config, self);
        assertEquals("anotherValue", config.getValue());

        // Test setAttribute
        config.setAttribute("newAttr", "newAttrValue");
        assertEquals("newAttrValue", config.getAttribute("newAttr"));
        assertEquals("value1", config.getAttribute("attr1")); // Existing attributes should be preserved

        // Test setAttribute with null (remove attribute)
        config.setAttribute("attr1", null);
        assertNull(config.getAttribute("attr1"));

        // Test addChild(String)
        PlexusConfiguration newChild = config.addChild("newChild");
        assertNotNull(newChild);
        assertEquals("newChild", newChild.getName());
        assertNull(newChild.getValue());

        // Test addChild(String, String)
        PlexusConfiguration newChildWithValue = config.addChild("childWithValue", "childValue");
        assertNotNull(newChildWithValue);
        assertEquals("childWithValue", newChildWithValue.getName());
        assertEquals("childValue", newChildWithValue.getValue());

        // Test getChild with createChild=true
        PlexusConfiguration createdChild = config.getChild("createdChild", true);
        assertNotNull(createdChild);
        assertEquals("createdChild", createdChild.getName());
        assertNull(createdChild.getValue());

        // Test addChild(PlexusConfiguration)
        XmlNode anotherNode = XmlNode.newInstance("anotherChild", "anotherValue");
        PlexusConfiguration anotherConfig = new XmlPlexusConfiguration(anotherNode);
        config.addChild(anotherConfig);

        PlexusConfiguration retrievedChild = config.getChild("anotherChild");
        assertNotNull(retrievedChild);
        assertEquals("anotherChild", retrievedChild.getName());
        assertEquals("anotherValue", retrievedChild.getValue());
    }

    @Test
    void testComplexXmlStructure() throws XMLStreamException {
        String xml = "<configuration>" + "  <property name=\"prop1\" value=\"val1\"/>"
                + "  <items>"
                + "    <item>item1</item>"
                + "    <item>item2</item>"
                + "  </items>"
                + "  <nested>"
                + "    <deep>"
                + "      <value>deepValue</value>"
                + "    </deep>"
                + "  </nested>"
                + "</configuration>";

        XmlNode xmlNode = parseXml(xml);
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        assertEquals("configuration", config.getName());
        assertEquals(3, config.getChildCount());

        PlexusConfiguration property = config.getChild("property");
        assertNotNull(property);
        assertEquals("prop1", property.getAttribute("name"));
        assertEquals("val1", property.getAttribute("value"));

        PlexusConfiguration items = config.getChild("items");
        assertNotNull(items);
        assertEquals(2, items.getChildCount());

        PlexusConfiguration[] itemArray = items.getChildren("item");
        assertEquals(2, itemArray.length);
        assertEquals("item1", itemArray[0].getValue());
        assertEquals("item2", itemArray[1].getValue());

        PlexusConfiguration nested = config.getChild("nested");
        assertNotNull(nested);
        PlexusConfiguration deep = nested.getChild("deep");
        assertNotNull(deep);
        PlexusConfiguration value = deep.getChild("value");
        assertNotNull(value);
        assertEquals("deepValue", value.getValue());
    }

    @Test
    void testToString() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = new XmlPlexusConfiguration(xmlNode);

        String result = config.toString();
        assertNotNull(result);
        // Basic checks that the toString contains expected elements
        assert result.contains("<root");
        assert result.contains("attr1=\"value1\"");
        assert result.contains("attr2=\"value2\"");
        assert result.contains("</root>");
    }

    @Test
    void testStaticFactoryMethod() {
        XmlNode xmlNode = createTestXmlNode();
        PlexusConfiguration config = XmlPlexusConfiguration.toPlexusConfiguration(xmlNode);

        assertNotNull(config);
        assertEquals("root", config.getName());
        assertEquals("rootValue", config.getValue());
    }

    @Test
    void testEmptyNode() {
        XmlNode emptyNode = XmlNode.newInstance("empty", null, null, null, null);
        PlexusConfiguration config = new XmlPlexusConfiguration(emptyNode);

        assertEquals("empty", config.getName());
        assertNull(config.getValue());
        assertEquals("default", config.getValue("default"));
        assertEquals(0, config.getChildCount());
        assertEquals(0, config.getAttributeNames().length);
        assertEquals(0, config.getChildren().length);
    }
}
