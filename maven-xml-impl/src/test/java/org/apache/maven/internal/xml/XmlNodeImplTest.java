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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class XmlNodeImplTest {

    @Test
    public void testCombineChildrenAppend() throws Exception {
        String lhs = "<configuration>\n"
                + "    <plugins>\n"
                + "        <plugin>\n"
                + "            <groupId>foo.bar</groupId>\n"
                + "            <artifactId>foo-bar-plugin</artifactId>\n"
                + "            <configuration>\n"
                + "                <plugins>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-compiler-plugin</artifactId>\n"
                + "                    </plugin>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-surefire-plugin</artifactId>\n"
                + "                        <foo>\n"
                + "                            <properties combine.children=\"append\">\n"
                + "                                <property>\n"
                + "                                    <name>prop2</name>\n"
                + "                                    <value>value2</value>\n"
                + "                                </property>\n"
                + "                            </properties>\n"
                + "                        </foo>\n"
                + "                    </plugin>\n"
                + "                </plugins>\n"
                + "            </configuration>\n"
                + "        </plugin>\n"
                + "    </plugins>\n"
                + "</configuration>";

        String rhs = "<configuration>\n"
                + "    <plugins>\n"
                + "        <plugin>\n"
                + "            <groupId>foo.bar</groupId>\n"
                + "            <artifactId>foo-bar-plugin</artifactId>\n"
                + "            <configuration>\n"
                + "                <plugins>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-compiler-plugin</artifactId>\n"
                + "                        <bar>\n"
                + "                            <value>foo</value>\n"
                + "                        </bar>\n"
                + "                    </plugin>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-surefire-plugin</artifactId>\n"
                + "                        <foo>\n"
                + "                            <properties>\n"
                + "                                <property>\n"
                + "                                    <name>prop1</name>\n"
                + "                                    <value>value1</value>\n"
                + "                                </property>\n"
                + "                            </properties>\n"
                + "                        </foo>\n"
                + "                    </plugin>\n"
                + "                </plugins>\n"
                + "            </configuration>\n"
                + "        </plugin>\n"
                + "    </plugins>\n"
                + "</configuration>";

        String result = "<configuration>\n"
                + "    <plugins>\n"
                + "        <plugin>\n"
                + "            <groupId>foo.bar</groupId>\n"
                + "            <artifactId>foo-bar-plugin</artifactId>\n"
                + "            <configuration>\n"
                + "                <plugins>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-compiler-plugin</artifactId>\n"
                + "                        <bar>\n"
                + "                            <value>foo</value>\n"
                + "                        </bar>\n"
                + "                    </plugin>\n"
                + "                    <plugin>\n"
                + "                        <groupId>org.apache.maven.plugins</groupId>\n"
                + "                        <artifactId>maven-surefire-plugin</artifactId>\n"
                + "                        <foo>\n"
                + "                            <properties combine.children=\"append\">\n"
                + "                                <property>\n"
                + "                                    <name>prop1</name>\n"
                + "                                    <value>value1</value>\n"
                + "                                </property>\n"
                + "                                <property>\n"
                + "                                    <name>prop2</name>\n"
                + "                                    <value>value2</value>\n"
                + "                                </property>\n"
                + "                            </properties>\n"
                + "                        </foo>\n"
                + "                    </plugin>\n"
                + "                </plugins>\n"
                + "            </configuration>\n"
                + "        </plugin>\n"
                + "    </plugins>\n"
                + "</configuration>";

        XmlNode leftDom = toXmlNode(lhs);
        XmlNode rightDom = toXmlNode(rhs);

        XmlNode mergeResult = leftDom.merge(rightDom);

        assertEquals(toXmlNode(result), mergeResult);
    }

    /**
     * <p>testCombineId.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    public void testCombineId() throws Exception {
        String lhs = "<props>" + "<property combine.id='LHS-ONLY'><name>LHS-ONLY</name><value>LHS</value></property>"
                + "<property combine.id='TOOVERWRITE'><name>TOOVERWRITE</name><value>LHS</value></property>"
                + "</props>";

        String rhs = "<props>" + "<property combine.id='RHS-ONLY'><name>RHS-ONLY</name><value>RHS</value></property>"
                + "<property combine.id='TOOVERWRITE'><name>TOOVERWRITE</name><value>RHS</value></property>"
                + "</props>";

        XmlNodeImpl leftDom = XmlNodeBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals(3, getChildren(mergeResult, "property").size());

        XmlNode p0 = getNthChild(mergeResult, "property", 0);
        assertEquals("LHS-ONLY", p0.getChild("name").getValue());
        assertEquals("left", p0.getChild("name").getInputLocation());
        assertEquals("LHS", p0.getChild("value").getValue());
        assertEquals("left", p0.getChild("value").getInputLocation());

        XmlNode p1 = getNthChild(mergeResult, "property", 1);
        assertEquals(
                "TOOVERWRITE",
                getNthChild(mergeResult, "property", 1).getChild("name").getValue());
        assertEquals("left", p1.getChild("name").getInputLocation());
        assertEquals(
                "LHS", getNthChild(mergeResult, "property", 1).getChild("value").getValue());
        assertEquals("left", p1.getChild("value").getInputLocation());

        XmlNode p2 = getNthChild(mergeResult, "property", 2);
        assertEquals(
                "RHS-ONLY",
                getNthChild(mergeResult, "property", 2).getChild("name").getValue());
        assertEquals("right", p2.getChild("name").getInputLocation());
        assertEquals(
                "RHS", getNthChild(mergeResult, "property", 2).getChild("value").getValue());
        assertEquals("right", p2.getChild("value").getInputLocation());
    }

    /**
     * <p>testCombineKeys.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    public void testCombineKeys() throws Exception {
        String lhs = "<props combine.keys='key'>"
                + "<property key=\"LHS-ONLY\"><name>LHS-ONLY</name><value>LHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>LHS</value></property>" + "</props>";

        String rhs = "<props combine.keys='key'>"
                + "<property key=\"RHS-ONLY\"><name>RHS-ONLY</name><value>RHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>RHS</value></property>" + "</props>";

        XmlNodeImpl leftDom = XmlNodeBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals(3, getChildren(mergeResult, "property").size());

        XmlNode p0 = getNthChild(mergeResult, "property", 0);
        assertEquals("LHS-ONLY", p0.getChild("name").getValue());
        assertEquals("left", p0.getChild("name").getInputLocation());
        assertEquals("LHS", p0.getChild("value").getValue());
        assertEquals("left", p0.getChild("value").getInputLocation());

        XmlNode p1 = getNthChild(mergeResult, "property", 1);
        assertEquals(
                "TOOVERWRITE",
                getNthChild(mergeResult, "property", 1).getChild("name").getValue());
        assertEquals("left", p1.getChild("name").getInputLocation());
        assertEquals(
                "LHS", getNthChild(mergeResult, "property", 1).getChild("value").getValue());
        assertEquals("left", p1.getChild("value").getInputLocation());

        XmlNode p2 = getNthChild(mergeResult, "property", 2);
        assertEquals(
                "RHS-ONLY",
                getNthChild(mergeResult, "property", 2).getChild("name").getValue());
        assertEquals("right", p2.getChild("name").getInputLocation());
        assertEquals(
                "RHS", getNthChild(mergeResult, "property", 2).getChild("value").getValue());
        assertEquals("right", p2.getChild("value").getInputLocation());
    }

    @Test
    public void testPreserveDominantBlankValue() throws XmlPullParserException, IOException {
        String lhs = "<parameter xml:space=\"preserve\"> </parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals(" ", mergeResult.getValue());
    }

    @Test
    public void testPreserveDominantEmptyNode() throws XmlPullParserException, IOException {
        String lhs = "<parameter></parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals("", mergeResult.getValue());
    }

    @Test
    public void testPreserveDominantEmptyNode2() throws XmlPullParserException, IOException {
        String lhs = "<parameter/>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertNull(mergeResult.getValue());
    }

    private static List<XmlNode> getChildren(XmlNode node, String name) {
        return node.getChildren().stream().filter(n -> n.getName().equals(name)).collect(Collectors.toList());
    }

    private static XmlNode getNthChild(XmlNode node, String name, int nth) {
        return node.getChildren().stream()
                .filter(n -> n.getName().equals(name))
                .skip(nth)
                .findFirst()
                .orElse(null);
    }

    private static XmlNode toXmlNode(String xml) throws XmlPullParserException, IOException {
        return toXmlNode(xml, null);
    }

    private static XmlNode toXmlNode(String xml, XmlNodeBuilder.InputLocationBuilder locationBuilder)
            throws XmlPullParserException, IOException {
        return XmlNodeBuilder.build(new StringReader(xml), locationBuilder);
    }

    private static class FixedInputLocationBuilder implements XmlNodeBuilder.InputLocationBuilder {
        private final Object location;

        public FixedInputLocationBuilder(Object location) {
            this.location = location;
        }

        public Object toInputLocation(XmlPullParser parser) {
            return location;
        }
    }
}
