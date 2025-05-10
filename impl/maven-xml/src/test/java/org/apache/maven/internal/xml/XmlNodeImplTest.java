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
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class XmlNodeImplTest {

    @Test
    void testCombineChildrenAppend() throws Exception {
        String lhs = """
                <configuration>
                    <plugins>
                        <plugin>
                            <groupId>foo.bar</groupId>
                            <artifactId>foo-bar-plugin</artifactId>
                            <configuration>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-compiler-plugin</artifactId>
                                    </plugin>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-surefire-plugin</artifactId>
                                        <foo>
                                            <properties combine.children="append">
                                                <property>
                                                    <name>prop2</name>
                                                    <value>value2</value>
                                                </property>
                                            </properties>
                                        </foo>
                                    </plugin>
                                </plugins>
                            </configuration>
                        </plugin>
                    </plugins>
                </configuration>""";

        String rhs = """
                <configuration>
                    <plugins>
                        <plugin>
                            <groupId>foo.bar</groupId>
                            <artifactId>foo-bar-plugin</artifactId>
                            <configuration>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-compiler-plugin</artifactId>
                                        <bar>
                                            <value>foo</value>
                                        </bar>
                                    </plugin>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-surefire-plugin</artifactId>
                                        <foo>
                                            <properties>
                                                <property>
                                                    <name>prop1</name>
                                                    <value>value1</value>
                                                </property>
                                            </properties>
                                        </foo>
                                    </plugin>
                                </plugins>
                            </configuration>
                        </plugin>
                    </plugins>
                </configuration>""";

        String result = """
                <configuration>
                    <plugins>
                        <plugin>
                            <groupId>foo.bar</groupId>
                            <artifactId>foo-bar-plugin</artifactId>
                            <configuration>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-compiler-plugin</artifactId>
                                        <bar>
                                            <value>foo</value>
                                        </bar>
                                    </plugin>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-surefire-plugin</artifactId>
                                        <foo>
                                            <properties combine.children="append">
                                                <property>
                                                    <name>prop1</name>
                                                    <value>value1</value>
                                                </property>
                                                <property>
                                                    <name>prop2</name>
                                                    <value>value2</value>
                                                </property>
                                            </properties>
                                        </foo>
                                    </plugin>
                                </plugins>
                            </configuration>
                        </plugin>
                    </plugins>
                </configuration>""";

        XmlNode leftDom = toXmlNode(lhs);
        XmlNode rightDom = toXmlNode(rhs);

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom);

        assertEquals(toXmlNode(result).toString(), mergeResult.toString());
        assertEquals(toXmlNode(result), mergeResult);
    }

    @Test
    void testAppend() throws Exception {
        String lhs =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <compilerArgs combine.children="append">
                    <arg>-Xmaxerrs</arg>
                    <arg>100</arg>
                    <arg>-Xmaxwarns</arg>
                    <arg>100</arg>
                </compilerArgs>
                """;
        String result =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <compilerArgs combine.children="append">
                  <arg>-Xmaxerrs</arg>
                  <arg>100</arg>
                  <arg>-Xmaxwarns</arg>
                  <arg>100</arg>
                  <arg>-Xmaxerrs</arg>
                  <arg>100</arg>
                  <arg>-Xmaxwarns</arg>
                  <arg>100</arg>
                </compilerArgs>""";

        XmlNode dom = toXmlNode(lhs);
        XmlNode res = toXmlNode(result);

        XmlNode mergeResult1 = XmlService.merge(dom, dom, false);
        assertEquals(res, mergeResult1);
        XmlNode mergeResult2 = XmlService.merge(dom, dom, (Boolean) null);
        assertEquals(res, mergeResult2);
        XmlNode mergeResult3 = XmlService.merge(dom, dom, true);
        assertEquals(dom, mergeResult3);
    }

    /**
     * <p>testCombineId.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testCombineId() throws Exception {
        String lhs = "<props>" + "<property combine.id='LHS-ONLY'><name>LHS-ONLY</name><value>LHS</value></property>"
                + "<property combine.id='TOOVERWRITE'><name>TOOVERWRITE</name><value>LHS</value></property>"
                + "</props>";

        String rhs = "<props>" + "<property combine.id='RHS-ONLY'><name>RHS-ONLY</name><value>RHS</value></property>"
                + "<property combine.id='TOOVERWRITE'><name>TOOVERWRITE</name><value>RHS</value></property>"
                + "</props>";

        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom, true);
        assertEquals(3, getChildren(mergeResult, "property").size());

        XmlNode p0 = getNthChild(mergeResult, "property", 0);
        assertEquals("LHS-ONLY", p0.child("name").value());
        assertEquals("left", p0.child("name").inputLocation());
        assertEquals("LHS", p0.child("value").value());
        assertEquals("left", p0.child("value").inputLocation());

        XmlNode p1 = getNthChild(mergeResult, "property", 1);
        assertEquals(
                "TOOVERWRITE",
                getNthChild(mergeResult, "property", 1).child("name").value());
        assertEquals("left", p1.child("name").inputLocation());
        assertEquals(
                "LHS", getNthChild(mergeResult, "property", 1).child("value").value());
        assertEquals("left", p1.child("value").inputLocation());

        XmlNode p2 = getNthChild(mergeResult, "property", 2);
        assertEquals(
                "RHS-ONLY",
                getNthChild(mergeResult, "property", 2).child("name").value());
        assertEquals("right", p2.child("name").inputLocation());
        assertEquals(
                "RHS", getNthChild(mergeResult, "property", 2).child("value").value());
        assertEquals("right", p2.child("value").inputLocation());
    }

    /**
     * <p>testCombineKeys.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testCombineKeys() throws Exception {
        String lhs = "<props combine.keys='key'>"
                + "<property key=\"LHS-ONLY\"><name>LHS-ONLY</name><value>LHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>LHS</value></property>" + "</props>";

        String rhs = "<props combine.keys='key'>"
                + "<property key=\"RHS-ONLY\"><name>RHS-ONLY</name><value>RHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>RHS</value></property>" + "</props>";

        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom, true);
        assertEquals(3, getChildren(mergeResult, "property").size());

        XmlNode p0 = getNthChild(mergeResult, "property", 0);
        assertEquals("LHS-ONLY", p0.child("name").value());
        assertEquals("left", p0.child("name").inputLocation());
        assertEquals("LHS", p0.child("value").value());
        assertEquals("left", p0.child("value").inputLocation());

        XmlNode p1 = getNthChild(mergeResult, "property", 1);
        assertEquals(
                "TOOVERWRITE",
                getNthChild(mergeResult, "property", 1).child("name").value());
        assertEquals("left", p1.child("name").inputLocation());
        assertEquals(
                "LHS", getNthChild(mergeResult, "property", 1).child("value").value());
        assertEquals("left", p1.child("value").inputLocation());

        XmlNode p2 = getNthChild(mergeResult, "property", 2);
        assertEquals(
                "RHS-ONLY",
                getNthChild(mergeResult, "property", 2).child("name").value());
        assertEquals("right", p2.child("name").inputLocation());
        assertEquals(
                "RHS", getNthChild(mergeResult, "property", 2).child("value").value());
        assertEquals("right", p2.child("value").inputLocation());
    }

    @Test
    void testPreserveDominantBlankValue() throws XMLStreamException, IOException {
        String lhs = "<parameter xml:space=\"preserve\"> </parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom, true);
        assertEquals(" ", mergeResult.value());
    }

    @Test
    void testPreserveDominantEmptyNode() throws XMLStreamException, IOException {
        String lhs = "<parameter></parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom, true);
        assertEquals("", mergeResult.value());
    }

    @Test
    void testPreserveDominantEmptyNode2() throws XMLStreamException, IOException {
        String lhs = "<parameter/>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom, true);
        assertNull(mergeResult.value());
    }

    /**
     * <p>testShouldPerformAppendAtFirstSubElementLevel.</p>
     */
    @Test
    void testShouldPerformAppendAtFirstSubElementLevel() throws XMLStreamException {
        String lhs =
                """
                <top combine.children="append">
                  <topsub1>t1s1Value</topsub1>
                  <topsub1>t1s2Value</topsub1>
                </top>
                """;
        String rhs =
                """
                <top>
                    <topsub1>t2s1Value</topsub1>
                    <topsub1>t2s2Value</topsub1>
                </top>
                """;
        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode result = XmlService.merge(leftDom, rightDom);
        assertEquals(4, getChildren(result, "topsub1").size());
        assertEquals("t2s1Value", getChildren(result, "topsub1").getFirst().value());
        assertEquals("t2s2Value", getChildren(result, "topsub1").get(1).value());
        assertEquals("t1s1Value", getChildren(result, "topsub1").get(2).value());
        assertEquals("t1s2Value", getChildren(result, "topsub1").get(3).value());

        assertEquals("left", result.inputLocation());
        assertEquals("right", getChildren(result, "topsub1").getFirst().inputLocation());
        assertEquals("right", getChildren(result, "topsub1").get(1).inputLocation());
        assertEquals("left", getChildren(result, "topsub1").get(2).inputLocation());
        assertEquals("left", getChildren(result, "topsub1").get(3).inputLocation());
    }

    /**
     * <p>testShouldOverrideAppendAndDeepMerge.</p>
     */
    @Test
    void testShouldOverrideAppendAndDeepMerge() {
        // create the dominant DOM
        Xpp3Dom t1 = new Xpp3Dom("top");
        t1.setAttribute(Xpp3Dom.CHILDREN_COMBINATION_MODE_ATTRIBUTE, Xpp3Dom.CHILDREN_COMBINATION_APPEND);
        t1.setInputLocation("t1top");

        Xpp3Dom t1s1 = new Xpp3Dom("topsub1");
        t1s1.setValue("t1s1Value");
        t1s1.setInputLocation("t1s1");

        t1.addChild(t1s1);

        // create the recessive DOM
        Xpp3Dom t2 = new Xpp3Dom("top");
        t2.setInputLocation("t2top");

        Xpp3Dom t2s1 = new Xpp3Dom("topsub1");
        t2s1.setValue("t2s1Value");
        t2s1.setInputLocation("t2s1");

        t2.addChild(t2s1);

        // merge and check results.
        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(t1, t2, Boolean.TRUE);

        assertEquals(1, result.getChildren("topsub1").length);
        assertEquals("t1s1Value", result.getChildren("topsub1")[0].getValue());

        assertEquals("t1top", result.getInputLocation());
        assertEquals("t1s1", result.getChildren("topsub1")[0].getInputLocation());
    }

    /**
     * <p>testShouldPerformSelfOverrideAtTopLevel.</p>
     */
    @Test
    void testShouldPerformSelfOverrideAtTopLevel() {
        // create the dominant DOM
        Xpp3Dom t1 = new Xpp3Dom("top");
        t1.setAttribute("attr", "value");
        t1.setInputLocation("t1top");

        t1.setAttribute(Xpp3Dom.SELF_COMBINATION_MODE_ATTRIBUTE, Xpp3Dom.SELF_COMBINATION_OVERRIDE);

        // create the recessive DOM
        Xpp3Dom t2 = new Xpp3Dom("top");
        t2.setAttribute("attr2", "value2");
        t2.setValue("t2Value");
        t2.setInputLocation("t2top");

        // merge and check results.
        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(t1, t2);

        assertEquals(2, result.getAttributeNames().length);
        assertNull(result.getValue());
        assertEquals("t1top", result.getInputLocation());
    }

    /**
     * <p>testShouldMergeValuesAtTopLevelByDefault.</p>
     */
    @Test
    void testShouldNotMergeValuesAtTopLevelByDefault() {
        // create the dominant DOM
        Xpp3Dom t1 = new Xpp3Dom("top");
        t1.setAttribute("attr", "value");
        t1.setInputLocation("t1top");

        // create the recessive DOM
        Xpp3Dom t2 = new Xpp3Dom("top");
        t2.setAttribute("attr2", "value2");
        t2.setValue("t2Value");
        t2.setInputLocation("t2top");

        // merge and check results.
        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(t1, t2);

        // this is still 2, since we're not using the merge-control attribute.
        assertEquals(2, result.getAttributeNames().length);

        assertNull(result.getValue());
        assertEquals("t1top", result.getInputLocation());
    }

    /**
     * <p>testShouldMergeValuesAtTopLevel.</p>
     */
    @Test
    void testShouldNotMergeValuesAtTopLevel() {
        // create the dominant DOM
        Xpp3Dom t1 = new Xpp3Dom("top");
        t1.setAttribute("attr", "value");

        t1.setAttribute(Xpp3Dom.SELF_COMBINATION_MODE_ATTRIBUTE, Xpp3Dom.SELF_COMBINATION_MERGE);

        // create the recessive DOM
        Xpp3Dom t2 = new Xpp3Dom("top");
        t2.setAttribute("attr2", "value2");
        t2.setValue("t2Value");

        // merge and check results.
        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(t1, t2);

        assertEquals(3, result.getAttributeNames().length);
        assertNull(result.getValue());
    }

    /**
     * <p>testEquals.</p>
     */
    @Test
    void testEquals() {
        XmlNode dom = XmlNode.newInstance("top");

        assertEquals(dom, dom);
        assertNotEquals(dom, null);
        assertNotEquals(dom, XmlNode.newInstance(""));
    }

    /**
     * <p>testEqualsIsNullSafe.</p>
     */
    @Test
    void testEqualsIsNullSafe() throws XMLStreamException, IOException {
        String testDom = "<configuration><items thing='blah'><item>one</item><item>two</item></items></configuration>";
        XmlNode dom = toXmlNode(testDom);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("nullValue", null);
        attributes.put(null, "nullKey");
        List<XmlNode> childList = new ArrayList<>();
        childList.add(null);
        Xpp3Dom dom2 = new Xpp3Dom(XmlNode.newInstance(dom.name(), null, attributes, childList, null));

        assertNotEquals(dom, dom2);
        assertNotEquals(dom2, dom);
    }

    /**
     * <p>testShouldOverwritePluginConfigurationSubItemsByDefault.</p>
     */
    @Test
    void testShouldOverwritePluginConfigurationSubItemsByDefault() throws XMLStreamException, IOException {
        String parentConfigStr = "<configuration><items><item>one</item><item>two</item></items></configuration>";
        XmlNode parentConfig = toXmlNode(parentConfigStr, new FixedInputLocationBuilder("parent"));

        String childConfigStr = "<configuration><items><item>three</item></items></configuration>";
        XmlNode childConfig = toXmlNode(childConfigStr, new FixedInputLocationBuilder("child"));

        XmlNode result = XmlService.merge(childConfig, parentConfig);
        XmlNode items = result.child("items");

        assertEquals(1, items.children().size());

        XmlNode item = items.children().getFirst();
        assertEquals("three", item.value());
        assertEquals("child", item.inputLocation());
    }

    /**
     * <p>testShouldMergePluginConfigurationSubItemsWithMergeAttributeSet.</p>
     */
    @Test
    void testShouldMergePluginConfigurationSubItemsWithMergeAttributeSet() throws XMLStreamException, IOException {
        String parentConfigStr = "<configuration><items><item>one</item><item>two</item></items></configuration>";
        XmlNode parentConfig = toXmlNode(parentConfigStr, new FixedInputLocationBuilder("parent"));

        String childConfigStr =
                "<configuration><items combine.children=\"append\"><item>three</item></items></configuration>";
        XmlNode childConfig = toXmlNode(childConfigStr, new FixedInputLocationBuilder("child"));

        XmlNode result = XmlService.merge(childConfig, parentConfig);
        assertNotNull(result);
        XmlNode items = result.child("items");
        assertNotNull(result);

        XmlNode[] item = items.children().toArray(new XmlNode[0]);
        assertEquals(3, item.length);
        assertEquals("one", item[0].value());
        assertEquals("parent", item[0].inputLocation());
        assertEquals("two", item[1].value());
        assertEquals("parent", item[1].inputLocation());
        assertEquals("three", item[2].value());
        assertEquals("child", item[2].inputLocation());
    }

    /**
     * <p>testShouldNotChangeUponMergeWithItselfWhenFirstOrLastSubItemIsEmpty.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testShouldNotChangeUponMergeWithItselfWhenFirstOrLastSubItemIsEmpty() throws Exception {
        String configStr = "<configuration><items><item/><item>test</item><item/></items></configuration>";
        XmlNode dominantConfig = toXmlNode(configStr);
        XmlNode recessiveConfig = toXmlNode(configStr);

        XmlNode result = XmlService.merge(dominantConfig, recessiveConfig);
        XmlNode items = result.child("items");

        assertEquals(3, items.children().size());

        assertNull(items.children().getFirst().value());
        assertEquals("test", items.children().get(1).value());
        assertNull(items.children().get(2).value());
    }

    /**
     * <p>testShouldCopyRecessiveChildrenNotPresentInTarget.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testShouldCopyRecessiveChildrenNotPresentInTarget() throws Exception {
        String dominantStr = "<configuration><foo>x</foo></configuration>";
        String recessiveStr = "<configuration><bar>y</bar></configuration>";
        Xpp3Dom dominantConfig = build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = build(new StringReader(recessiveStr));

        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(dominantConfig, recessiveConfig);

        assertEquals(2, result.getChildCount());

        assertEquals("x", result.getChild("foo").getValue());
        assertEquals("y", result.getChild("bar").getValue());
        assertNotSame(result.getChild("bar"), recessiveConfig.getChild("bar"));
    }

    /**
     * <p>testDupeChildren.</p>
     */
    @Test
    void testDupeChildren() throws IOException, XMLStreamException {
        String dupes = "<configuration><foo>x</foo><foo>y</foo></configuration>";
        XmlNode dom = toXmlNode(new StringReader(dupes));
        assertNotNull(dom);
        assertEquals("y", dom.child("foo").value());
    }

    /**
     * <p>testShouldRemoveEntireElementWithAttributesAndChildren.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testShouldRemoveEntireElementWithAttributesAndChildren() throws Exception {
        String dominantStr = "<config><service combine.self=\"remove\"/></config>";
        String recessiveStr = "<config><service><parameter>parameter</parameter></service></config>";
        Xpp3Dom dominantConfig = build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = build(new StringReader(recessiveStr));

        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(dominantConfig, recessiveConfig);

        assertEquals(0, result.getChildCount());
        assertEquals("config", result.getName());
    }

    /**
     * <p>testShouldRemoveDoNotRemoveTagWhenSwappedInputDOMs.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testShouldRemoveDoNotRemoveTagWhenSwappedInputDOMs() throws Exception {
        String dominantStr = "<config><service combine.self=\"remove\"/></config>";
        String recessiveStr = "<config><service><parameter>parameter</parameter></service></config>";
        Xpp3Dom dominantConfig = build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = build(new StringReader(recessiveStr));

        // same DOMs as testShouldRemoveEntireElementWithAttributesAndChildren(), swapping dominant <--> recessive
        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(recessiveConfig, dominantConfig);

        assertEquals(recessiveConfig.toString(), result.toString());
    }

    @Test
    void testMergeCombineChildrenAppendOnRecessive() throws XMLStreamException, IOException {
        String dominant = """
                <relocations>
                  <relocation>
                    <pattern>org.apache.shiro.crypto.CipherService</pattern>
                    <shadedPattern>org.apache.shiro.crypto.cipher.CipherService</shadedPattern>
                  </relocation>
                </relocations>""";
        String recessive = """
                <relocations combine.children="append">
                  <relocation>
                    <pattern>javax.faces</pattern>
                    <shadedPattern>jakarta.faces</shadedPattern>
                  </relocation>
                </relocations>""";
        String expected = """
                <relocations combine.children="append">
                  <relocation>
                    <pattern>javax.faces</pattern>
                    <shadedPattern>jakarta.faces</shadedPattern>
                  </relocation>
                  <relocation>
                    <pattern>org.apache.shiro.crypto.CipherService</pattern>
                    <shadedPattern>org.apache.shiro.crypto.cipher.CipherService</shadedPattern>
                  </relocation>
                </relocations>""";

        XmlNode d = toXmlNode(dominant);
        XmlNode r = toXmlNode(recessive);
        XmlNode m = XmlService.merge(d, r, null);
        assertEquals(expected, m.toString().replaceAll("\r\n", "\n"));
    }

    private static List<XmlNode> getChildren(XmlNode node, String name) {
        return node.children().stream().filter(n -> n.name().equals(name)).toList();
    }

    private static XmlNode getNthChild(XmlNode node, String name, int nth) {
        return node.children().stream()
                .filter(n -> n.name().equals(name))
                .skip(nth)
                .findFirst()
                .orElse(null);
    }

    private static XmlNode toXmlNode(String xml) throws XMLStreamException, IOException {
        return toXmlNode(xml, null);
    }

    private static XmlNode toXmlNode(String xml, XmlService.InputLocationBuilder locationBuilder)
            throws XMLStreamException, IOException {
        return toXmlNode(new StringReader(xml), locationBuilder);
    }

    private static XmlNode toXmlNode(Reader reader) throws XMLStreamException, IOException {
        return toXmlNode(reader, null);
    }

    private static XmlNode toXmlNode(Reader reader, XmlService.InputLocationBuilder locationBuilder)
            throws XMLStreamException, IOException {
        return XmlService.read(reader, locationBuilder);
    }

    private static class FixedInputLocationBuilder implements XmlService.InputLocationBuilder {
        private final Object location;

        FixedInputLocationBuilder(Object location) {
            this.location = location;
        }

        public Object toInputLocation(XMLStreamReader parser) {
            return location;
        }
    }

    public static Xpp3Dom build(Reader reader) throws XmlPullParserException, IOException {
        try (Reader closeMe = reader) {
            return new Xpp3Dom(XmlNodeBuilder.build(reader, true, null));
        }
    }
}
