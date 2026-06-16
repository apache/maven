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
import java.io.StringWriter;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlNodeImplTest {

    @Test
    void testCombineChildrenAppend() throws Exception {
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

        XmlNode mergeResult = XmlService.merge(leftDom, rightDom);

        assertEquals(toXmlNode(result).toString(), mergeResult.toString());
        assertEquals(toXmlNode(result), mergeResult);
    }

    @Test
    void testAppend() throws Exception {
        String lhs = """
                <?xml version="1.0" encoding="UTF-8"?>
                <compilerArgs combine.children="append">
                    <arg>-Xmaxerrs</arg>
                    <arg>100</arg>
                    <arg>-Xmaxwarns</arg>
                    <arg>100</arg>
                </compilerArgs>
                """;
        String result = """
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
        String lhs = """
                <top combine.children="append">
                  <topsub1>t1s1Value</topsub1>
                  <topsub1>t1s2Value</topsub1>
                </top>
                """;
        String rhs = """
                <top>
                    <topsub1>t2s1Value</topsub1>
                    <topsub1>t2s2Value</topsub1>
                </top>
                """;
        XmlNode leftDom = XmlService.read(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNode rightDom = XmlService.read(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode result = XmlService.merge(leftDom, rightDom);
        assertEquals(4, getChildren(result, "topsub1").size());
        assertEquals("t2s1Value", getChildren(result, "topsub1").get(0).value());
        assertEquals("t2s2Value", getChildren(result, "topsub1").get(1).value());
        assertEquals("t1s1Value", getChildren(result, "topsub1").get(2).value());
        assertEquals("t1s2Value", getChildren(result, "topsub1").get(3).value());

        assertEquals("left", result.inputLocation());
        assertEquals("right", getChildren(result, "topsub1").get(0).inputLocation());
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
     * <p>testEqualsComplex.</p>
     */
    @Test
    void testEqualsComplex() throws XMLStreamException, XmlPullParserException, IOException {
        String testDom = "<configuration><items thing='blah'><item>one</item><item>two</item></items></configuration>";
        XmlNode dom1 = XmlService.read(new StringReader(testDom));
        XmlNode dom2 = XmlNodeBuilder.build(new StringReader(testDom));

        assertEquals(dom1, dom2);
    }

    /**
     * <p>testEqualsWithDifferentStructures.</p>
     */
    @Test
    void testEqualsWithDifferentStructures() throws XMLStreamException, IOException {
        String testDom = "<configuration><items thing='blah'><item>one</item><item>two</item></items></configuration>";
        XmlNode dom = toXmlNode(testDom);

        // Create a different DOM structure with different attributes and children
        Map<String, String> attributes = new HashMap<>();
        attributes.put("differentAttribute", "differentValue");
        List<XmlNode> childList = new ArrayList<>();
        childList.add(XmlNode.newInstance("differentChild", "differentValue", null, null, null));
        Xpp3Dom dom2 = new Xpp3Dom(XmlNode.newInstance(dom.name(), "differentValue", attributes, childList, null));

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

        XmlNode item = items.children().get(0);
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

        assertNull(items.children().get(0).value());
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
        String dominant = "<relocations>\n" + "  <relocation>\n"
                + "    <pattern>org.apache.shiro.crypto.CipherService</pattern>\n"
                + "    <shadedPattern>org.apache.shiro.crypto.cipher.CipherService</shadedPattern>\n"
                + "  </relocation>\n"
                + "</relocations>";
        String recessive = "<relocations combine.children=\"append\">\n"
                + "  <relocation>\n"
                + "    <pattern>javax.faces</pattern>\n"
                + "    <shadedPattern>jakarta.faces</shadedPattern>\n"
                + "  </relocation>\n"
                + "</relocations>";
        String expected = "<relocations combine.children=\"append\">\n"
                + "  <relocation>\n"
                + "    <pattern>javax.faces</pattern>\n"
                + "    <shadedPattern>jakarta.faces</shadedPattern>\n"
                + "  </relocation>\n"
                + "  <relocation>\n"
                + "    <pattern>org.apache.shiro.crypto.CipherService</pattern>\n"
                + "    <shadedPattern>org.apache.shiro.crypto.cipher.CipherService</shadedPattern>\n"
                + "  </relocation>\n"
                + "</relocations>";

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

        @Override
        public Object toInputLocation(XMLStreamReader parser) {
            return location;
        }
    }

    // ========================================================================================
    // Namespace context - Parsing tests
    // ========================================================================================

    @Test
    void testParseNamespaceContextSinglePrefixOnRoot() throws Exception {
        String xml = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child/>
                </root>
                """;
        XmlNode node = toXmlNode(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", node.namespaces().get("mvn"));
    }

    @Test
    void testParseNamespaceContextMultiplePrefixes() throws Exception {
        String xml = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0"
                      xmlns:custom="http://example.com/custom"
                      xmlns:other="http://example.com/other">
                    <child/>
                </root>
                """;
        XmlNode node = toXmlNode(xml);
        assertEquals(3, node.namespaces().size());
        assertEquals("http://maven.apache.org/POM/4.0.0", node.namespaces().get("mvn"));
        assertEquals("http://example.com/custom", node.namespaces().get("custom"));
        assertEquals("http://example.com/other", node.namespaces().get("other"));
    }

    @Test
    void testParseNamespaceContextInheritedByChild() throws Exception {
        String xml = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child mvn:combine.children="append"/>
                </root>
                """;
        XmlNode node = toXmlNode(xml);
        XmlNode child = node.child("child");
        assertNotNull(child);
        // Child inherits parent's namespace context
        assertEquals("http://maven.apache.org/POM/4.0.0", child.namespaces().get("mvn"));
        // Child does NOT have xmlns:mvn in its own attributes
        assertNull(child.attribute("xmlns:mvn"));
    }

    @Test
    void testParseNamespaceContextInheritedAcrossThreeLevels() throws Exception {
        String xml = """
                <root xmlns:a="http://example.com/a">
                    <level1 xmlns:b="http://example.com/b">
                        <level2 a:x="1" b:y="2">
                            <leaf/>
                        </level2>
                    </level1>
                </root>
                """;
        XmlNode root = toXmlNode(xml);
        XmlNode level1 = root.child("level1");
        XmlNode level2 = level1.child("level2");
        XmlNode leaf = level2.child("leaf");

        // root has only "a"
        assertEquals("http://example.com/a", root.namespaces().get("a"));
        assertNull(root.namespaces().get("b"));

        // level1 has both "a" (inherited) and "b" (own)
        assertEquals("http://example.com/a", level1.namespaces().get("a"));
        assertEquals("http://example.com/b", level1.namespaces().get("b"));

        // level2 inherits both
        assertEquals("http://example.com/a", level2.namespaces().get("a"));
        assertEquals("http://example.com/b", level2.namespaces().get("b"));

        // leaf also inherits both
        assertEquals("http://example.com/a", leaf.namespaces().get("a"));
        assertEquals("http://example.com/b", leaf.namespaces().get("b"));
    }

    @Test
    void testParseDefaultNamespaceNotInNamespacesMap() throws Exception {
        String xml = """
                <root xmlns="http://maven.apache.org/POM/4.0.0">
                    <child/>
                </root>
                """;
        XmlNode node = toXmlNode(xml);
        // Default namespace (no prefix) should NOT be in the namespaces map
        // since namespaces() tracks prefix→URI bindings for resolving prefixed attributes
        assertNull(node.namespaces().get(""));
        assertNull(node.namespaces().get("xmlns"));
        // The default namespace is stored as an attribute instead
        assertEquals("http://maven.apache.org/POM/4.0.0", node.attribute("xmlns"));
    }

    @Test
    void testParseNamespaceContextChildOverridesPrefix() throws Exception {
        String xml = """
                <root xmlns:ns="http://example.com/original">
                    <child xmlns:ns="http://example.com/overridden" ns:attr="val">
                        <grandchild ns:attr2="val2"/>
                    </child>
                </root>
                """;
        XmlNode root = toXmlNode(xml);
        XmlNode child = root.child("child");
        XmlNode grandchild = child.child("grandchild");

        // Root has original binding
        assertEquals("http://example.com/original", root.namespaces().get("ns"));
        // Child overrides
        assertEquals("http://example.com/overridden", child.namespaces().get("ns"));
        // Grandchild inherits the overridden version
        assertEquals("http://example.com/overridden", grandchild.namespaces().get("ns"));
    }

    @Test
    void testParseNoNamespaceDeclarationsProducesEmptyMap() throws Exception {
        String xml = "<root><child attr=\"value\"/></root>";
        XmlNode root = toXmlNode(xml);
        assertTrue(root.namespaces().isEmpty());
        XmlNode child = root.child("child");
        assertNotNull(child);
        assertTrue(child.namespaces().isEmpty());
    }

    @Test
    void testParseNamespacesMapIsImmutable() throws Exception {
        String xml = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child/>
                </root>
                """;
        XmlNode node = toXmlNode(xml);
        assertThrows(
                UnsupportedOperationException.class, () -> node.namespaces().put("foo", "bar"));
    }

    // ========================================================================================
    // Namespace context - Writing tests
    // ========================================================================================

    @Test
    void testWriteWithNamespaceDeclarationsAndPrefixedAttributes() throws Exception {
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <compilerArgs mvn:combine.children="append">
                        <arg>-Xlint:deprecation</arg>
                    </compilerArgs>
                </project>
                """;

        XmlNode node = toXmlNode(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", node.attribute("xmlns:mvn"));

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        XmlNode reRead = toXmlNode(output);
        assertNotNull(reRead);
    }

    @Test
    void testWriteStripsOrphanedPrefixOnAttributes() throws Exception {
        XmlNode node = XmlNode.newBuilder()
                .name("compilerArgs")
                .attributes(Map.of("mvn:combine.children", "append"))
                .children(List.of(XmlNode.newBuilder()
                        .name("arg")
                        .value("-Xlint:deprecation")
                        .build()))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        assertFalse(output.contains("mvn:combine"), "Output should not contain orphaned mvn: prefix");
        assertTrue(output.contains("combine.children=\"append\""), "Attribute should be written unprefixed");

        XmlNode reRead = toXmlNode(output);
        assertNotNull(reRead);
        assertEquals("append", reRead.attribute("combine.children"));
    }

    @Test
    void testWriteForeignNamespaceAttributeRoundTrip() throws Exception {
        XmlNode node = XmlNode.newBuilder()
                .name("compilerArgs")
                .attributes(Map.of(
                        "xmlns:custom", "http://example.com/custom",
                        "custom:myattr", "value"))
                .children(List.of(XmlNode.newBuilder()
                        .name("arg")
                        .value("-Xlint:deprecation")
                        .build()))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        XmlNode reRead = toXmlNode(output);
        assertNotNull(reRead);
        assertEquals("value", reRead.attribute("custom:myattr"));
        assertEquals("http://example.com/custom", reRead.attribute("xmlns:custom"));
    }

    @Test
    void testWritePreservesPrefixFromInheritedNamespaceContext() throws Exception {
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:custom="http://example.com/custom">
                    <compilerArgs custom:myattr="value">
                        <arg>-Xlint:deprecation</arg>
                    </compilerArgs>
                </project>
                """;

        XmlNode node = toXmlNode(xml);
        XmlNode compilerArgs = node.child("compilerArgs");
        assertNotNull(compilerArgs);
        assertEquals("value", compilerArgs.attribute("custom:myattr"));
        assertNull(compilerArgs.attribute("xmlns:custom"), "xmlns:custom should be on parent, not child");
        assertEquals("http://example.com/custom", compilerArgs.namespaces().get("custom"));

        StringWriter writer = new StringWriter();
        XmlService.write(compilerArgs, writer);
        String output = writer.toString();

        XmlNode reRead = toXmlNode(output);
        assertNotNull(reRead);
        assertEquals("value", reRead.attribute("custom:myattr"));
    }

    @Test
    void testWriteStripsOrphanedPrefixWithoutNamespaceContext() throws Exception {
        XmlNode node = XmlNode.newBuilder()
                .name("compilerArgs")
                .attributes(Map.of("mvn:combine.children", "append"))
                .children(List.of(XmlNode.newBuilder()
                        .name("arg")
                        .value("-Xlint:deprecation")
                        .build()))
                .build();

        assertTrue(node.namespaces().isEmpty(), "No namespace context");

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        assertFalse(output.contains("mvn:combine"), "Output should not contain orphaned mvn: prefix");
        assertTrue(output.contains("combine.children=\"append\""), "Attribute should be written unprefixed");

        XmlNode reRead = toXmlNode(output);
        assertNotNull(reRead);
        assertEquals("append", reRead.attribute("combine.children"));
    }

    @Test
    void testWriteMultiplePrefixedAttributesFromDifferentNamespaces() throws Exception {
        String xml = """
                <root xmlns:a="http://example.com/a" xmlns:b="http://example.com/b">
                    <child a:x="1" b:y="2"/>
                </root>
                """;
        XmlNode root = toXmlNode(xml);
        XmlNode child = root.child("child");
        assertNotNull(child);

        // Write only the child (which has prefixed attrs but no local xmlns:)
        StringWriter writer = new StringWriter();
        XmlService.write(child, writer);
        String output = writer.toString();

        // Both namespace declarations should be auto-declared
        assertTrue(output.contains("xmlns:a="), "Should auto-declare xmlns:a");
        assertTrue(output.contains("xmlns:b="), "Should auto-declare xmlns:b");

        // Round-trip should preserve attributes
        XmlNode reRead = toXmlNode(output);
        assertEquals("1", reRead.attribute("a:x"));
        assertEquals("2", reRead.attribute("b:y"));
    }

    @Test
    void testWriteLocalXmlnsOverridesNamespaceContext() throws Exception {
        // Build a node where the local attribute has xmlns:ns with one URI
        // but the namespace context has a different URI for the same prefix.
        // The local declaration should win.
        XmlNode node = XmlNode.newBuilder()
                .name("elem")
                .attributes(Map.of(
                        "xmlns:ns", "http://example.com/local",
                        "ns:attr", "value"))
                .namespaces(Map.of("ns", "http://example.com/context"))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        // The local xmlns:ns should be used, not the one from context
        assertTrue(output.contains("http://example.com/local"), "Local xmlns: should take precedence");

        XmlNode reRead = toXmlNode(output);
        assertEquals("value", reRead.attribute("ns:attr"));
        assertEquals("http://example.com/local", reRead.attribute("xmlns:ns"));
    }

    @Test
    void testWriteXmlSpaceAttributeRoundTrip() throws Exception {
        String xml = """
                <root xml:space="preserve">  content with spaces  </root>
                """;
        XmlNode node = toXmlNode(xml);
        assertEquals("preserve", node.attribute("xml:space"));

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        // xml: prefix should be handled without explicit declaration
        assertFalse(output.contains("xmlns:xml"), "xml: prefix must not be declared");
        XmlNode reRead = toXmlNode(output);
        assertEquals("preserve", reRead.attribute("xml:space"));
        assertEquals("  content with spaces  ", reRead.value());
    }

    @Test
    void testWriteUnprefixedAttributeUnchanged() throws Exception {
        XmlNode node = XmlNode.newBuilder()
                .name("elem")
                .attributes(Map.of("simple", "value", "another", "val2"))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        XmlNode reRead = toXmlNode(output);
        assertEquals("value", reRead.attribute("simple"));
        assertEquals("val2", reRead.attribute("another"));
    }

    @Test
    void testWriteNamespaceNotDeclaredTwice() throws Exception {
        // When xmlns:mvn is both in attributes AND namespace context,
        // it should only be declared once
        XmlNode node = XmlNode.newBuilder()
                .name("elem")
                .attributes(Map.of(
                        "xmlns:mvn", "http://maven.apache.org/POM/4.0.0",
                        "mvn:combine.children", "append"))
                .namespaces(Map.of("mvn", "http://maven.apache.org/POM/4.0.0"))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        // Count occurrences of xmlns:mvn - should be exactly 1
        int count = 0;
        int idx = 0;
        while ((idx = output.indexOf("xmlns:mvn", idx)) != -1) {
            count++;
            idx += "xmlns:mvn".length();
        }
        assertEquals(1, count, "xmlns:mvn should be declared exactly once");

        XmlNode reRead = toXmlNode(output);
        assertEquals("append", reRead.attribute("mvn:combine.children"));
    }

    @Test
    void testWriteChildInheritsContextAndWritesStandalone() throws Exception {
        // Parse a 3-level structure, then write the grandchild standalone
        String xml = """
                <root xmlns:a="http://example.com/a">
                    <mid xmlns:b="http://example.com/b">
                        <leaf a:x="1" b:y="2" plain="3"/>
                    </mid>
                </root>
                """;
        XmlNode root = toXmlNode(xml);
        XmlNode leaf = root.child("mid").child("leaf");

        StringWriter writer = new StringWriter();
        XmlService.write(leaf, writer);
        String output = writer.toString();

        XmlNode reRead = toXmlNode(output);
        assertEquals("1", reRead.attribute("a:x"));
        assertEquals("2", reRead.attribute("b:y"));
        assertEquals("3", reRead.attribute("plain"));
    }

    // ========================================================================================
    // Namespace context - Merge tests
    // ========================================================================================

    @Test
    void testMergePreservesDominantNamespaces() throws Exception {
        String dominant = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child mvn:combine.children="append">
                        <item>dom</item>
                    </child>
                </root>
                """;
        String recessive = """
                <root>
                    <child>
                        <item>rec</item>
                    </child>
                </root>
                """;
        XmlNode merged = XmlService.merge(toXmlNode(dominant), toXmlNode(recessive));

        // The merged root should keep dominant's namespace context
        assertEquals("http://maven.apache.org/POM/4.0.0", merged.namespaces().get("mvn"));

        // The merged child should also have the namespace context
        XmlNode child = merged.child("child");
        assertNotNull(child);
        assertEquals("http://maven.apache.org/POM/4.0.0", child.namespaces().get("mvn"));
    }

    @Test
    void testMergeCombineChildrenAppendPreservesNamespaces() throws Exception {
        String dominant = """
                <root xmlns="http://maven.apache.org/POM/4.0.0" xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <items combine.children="append">
                        <item>a</item>
                    </items>
                </root>
                """;
        String recessive = """
                <root xmlns="http://maven.apache.org/POM/4.0.0">
                    <items>
                        <item>b</item>
                    </items>
                </root>
                """;
        XmlNode merged = XmlService.merge(toXmlNode(dominant), toXmlNode(recessive));
        XmlNode items = merged.child("items");

        assertEquals(2, items.children().size(), "append should merge children");
        // Namespace context should be preserved on the merged element
        assertEquals("http://maven.apache.org/POM/4.0.0", items.namespaces().get("mvn"));
    }

    @Test
    void testMergeCombineSelfOverridePreservesNamespaces() throws Exception {
        String dominant = """
                <root xmlns:ns="http://example.com/ns">
                    <child combine.self="override" ns:attr="dominant">
                        <item>dom</item>
                    </child>
                </root>
                """;
        String recessive = """
                <root>
                    <child>
                        <item>rec1</item>
                        <item>rec2</item>
                    </child>
                </root>
                """;
        XmlNode merged = XmlService.merge(toXmlNode(dominant), toXmlNode(recessive));
        XmlNode child = merged.child("child");

        // override means dominant completely replaces recessive
        assertEquals(1, child.children().size());
        assertEquals("dom", child.children().get(0).value());
        // Namespace context preserved
        assertEquals("http://example.com/ns", child.namespaces().get("ns"));
    }

    @Test
    void testMergedNodeWriteProducesValidXml() throws Exception {
        String dominant = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child mvn:combine.children="append">
                        <item>a</item>
                    </child>
                </root>
                """;
        String recessive = """
                <root>
                    <child>
                        <item>b</item>
                    </child>
                </root>
                """;
        XmlNode merged = XmlService.merge(toXmlNode(dominant), toXmlNode(recessive));

        // Write the merged child alone - it should produce valid XML
        // because it has the namespace context from the dominant
        XmlNode child = merged.child("child");
        StringWriter writer = new StringWriter();
        XmlService.write(child, writer);
        String output = writer.toString();

        // mvn:combine.children should be preserved with namespace declaration
        assertTrue(output.contains("mvn:combine.children"), "Prefix should be preserved from context");
        assertTrue(output.contains("xmlns:mvn="), "Namespace should be auto-declared");

        XmlNode reRead = toXmlNode(output);
        assertEquals("append", reRead.attribute("mvn:combine.children"));
    }

    // ========================================================================================
    // Namespace context - Builder tests
    // ========================================================================================

    @Test
    void testBuilderWithExplicitNamespaces() throws Exception {
        XmlNode node = XmlNode.newBuilder()
                .name("elem")
                .attributes(Map.of("ns:attr", "value"))
                .namespaces(Map.of("ns", "http://example.com/ns"))
                .build();

        assertEquals("http://example.com/ns", node.namespaces().get("ns"));

        StringWriter writer = new StringWriter();
        XmlService.write(node, writer);
        String output = writer.toString();

        assertTrue(output.contains("xmlns:ns="), "Namespace should be auto-declared from builder context");
        XmlNode reRead = toXmlNode(output);
        assertEquals("value", reRead.attribute("ns:attr"));
    }

    @Test
    void testBuilderWithNullNamespacesDefaultsToEmpty() {
        XmlNode node = XmlNode.newBuilder().name("elem").build();
        assertNotNull(node.namespaces());
        assertTrue(node.namespaces().isEmpty());
    }

    @Test
    void testBuilderNamespacesAreImmutable() {
        Map<String, String> mutableNs = new HashMap<>(Map.of("ns", "http://example.com"));
        XmlNode node = XmlNode.newBuilder().name("elem").namespaces(mutableNs).build();

        // Mutating the original map should not affect the node
        mutableNs.put("other", "http://other.com");
        assertNull(node.namespaces().get("other"));

        // The namespaces map itself should be immutable
        assertThrows(
                UnsupportedOperationException.class, () -> node.namespaces().put("foo", "bar"));
    }

    @Test
    void testDefaultNamespacesMethodReturnsEmptyMap() {
        // XmlNode built with newInstance (which doesn't set namespaces)
        // should return empty map from the default namespaces() method
        XmlNode node = XmlNode.newInstance("test");
        assertNotNull(node.namespaces());
        assertTrue(node.namespaces().isEmpty());
    }

    // ========================================================================================
    // Namespace context - Round-trip fidelity tests
    // ========================================================================================

    @Test
    void testRoundTripPreservesNamespaceContext() throws Exception {
        String xml = """
                <root xmlns:a="http://example.com/a" xmlns:b="http://example.com/b">
                    <child a:x="1" b:y="2"/>
                </root>
                """;
        XmlNode original = toXmlNode(xml);

        StringWriter writer = new StringWriter();
        XmlService.write(original, writer);
        XmlNode reRead = toXmlNode(writer.toString());

        // Root namespace context should be preserved
        assertEquals(original.namespaces().get("a"), reRead.namespaces().get("a"));
        assertEquals(original.namespaces().get("b"), reRead.namespaces().get("b"));

        // Child namespace context should be preserved
        XmlNode origChild = original.child("child");
        XmlNode reReadChild = reRead.child("child");
        assertEquals(origChild.namespaces().get("a"), reReadChild.namespaces().get("a"));
        assertEquals(origChild.namespaces().get("b"), reReadChild.namespaces().get("b"));
    }

    @Test
    void testRoundTripDeepNestedStructure() throws Exception {
        String xml = """
                <root xmlns:ns="http://example.com/ns">
                    <level1>
                        <level2>
                            <level3 ns:deep="value">text</level3>
                        </level2>
                    </level1>
                </root>
                """;
        XmlNode original = toXmlNode(xml);

        StringWriter writer = new StringWriter();
        XmlService.write(original, writer);
        XmlNode reRead = toXmlNode(writer.toString());

        XmlNode level3 = reRead.child("level1").child("level2").child("level3");
        assertEquals("value", level3.attribute("ns:deep"));
        assertEquals("text", level3.value());
        assertEquals("http://example.com/ns", level3.namespaces().get("ns"));
    }

    @Test
    void testRoundTripWithOverriddenNamespace() throws Exception {
        String xml = """
                <root xmlns:ns="http://example.com/v1">
                    <child xmlns:ns="http://example.com/v2" ns:attr="val"/>
                </root>
                """;
        XmlNode original = toXmlNode(xml);
        XmlNode child = original.child("child");
        assertEquals("http://example.com/v2", child.namespaces().get("ns"));

        // Write and re-read just the child
        StringWriter writer = new StringWriter();
        XmlService.write(child, writer);
        XmlNode reRead = toXmlNode(writer.toString());

        assertEquals("val", reRead.attribute("ns:attr"));
        assertEquals("http://example.com/v2", reRead.namespaces().get("ns"));
    }

    // ========================================================================================
    // Namespace context - Consumer POM simulation tests
    // ========================================================================================

    @Test
    void testConsumerPomScenarioPrefixFromContext() throws Exception {
        // Simulate: parse a full POM with xmlns:mvn on project, mvn:combine.children on child
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <build>
                        <plugins>
                            <plugin>
                                <configuration>
                                    <compilerArgs mvn:combine.children="append">
                                        <arg>-Xlint</arg>
                                    </compilerArgs>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
        XmlNode project = toXmlNode(xml);
        XmlNode compilerArgs = project.child("build")
                .child("plugins")
                .child("plugin")
                .child("configuration")
                .child("compilerArgs");
        assertNotNull(compilerArgs);
        assertEquals("append", compilerArgs.attribute("mvn:combine.children"));
        assertEquals(
                "http://maven.apache.org/POM/4.0.0", compilerArgs.namespaces().get("mvn"));

        // Simulate consumer POM: write only the configuration subtree
        XmlNode config = project.child("build").child("plugins").child("plugin").child("configuration");
        StringWriter writer = new StringWriter();
        XmlService.write(config, writer);
        String output = writer.toString();

        // Should produce valid XML with auto-declared xmlns:mvn
        XmlNode reRead = toXmlNode(output);
        XmlNode reReadArgs = reRead.child("compilerArgs");
        assertEquals("append", reReadArgs.attribute("mvn:combine.children"));
    }

    @Test
    void testConsumerPomScenarioNoContextFallback() throws Exception {
        // Simulate: programmatically-built XmlNode without namespace context
        // (as might happen if someone builds configuration in code)
        XmlNode config = XmlNode.newBuilder()
                .name("configuration")
                .children(List.of(XmlNode.newBuilder()
                        .name("compilerArgs")
                        .attributes(Map.of("mvn:combine.children", "append"))
                        .children(List.of(
                                XmlNode.newBuilder().name("arg").value("-Xlint").build()))
                        .build()))
                .build();

        StringWriter writer = new StringWriter();
        XmlService.write(config, writer);
        String output = writer.toString();

        // Without namespace context, prefix should be stripped
        assertFalse(output.contains("mvn:"), "No mvn: prefix without context");
        XmlNode reRead = toXmlNode(output);
        assertEquals("append", reRead.child("compilerArgs").attribute("combine.children"));
    }

    // ========================================================================================
    // Namespace context - Merge directive interaction tests
    // ========================================================================================

    @Test
    void testPrefixedCombineChildrenDoesNotMerge() throws Exception {
        String dominant = """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <compilerArgs mvn:combine.children="append">
                        <arg>-Xlint:deprecation</arg>
                    </compilerArgs>
                </project>
                """;

        String recessive = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <compilerArgs>
                        <arg>-Xlint:unchecked</arg>
                    </compilerArgs>
                </project>
                """;

        XmlNode dominantNode = toXmlNode(dominant);
        XmlNode recessiveNode = toXmlNode(recessive);
        XmlNode merged = XmlService.merge(dominantNode, recessiveNode);

        XmlNode compilerArgs = merged.child("compilerArgs");
        assertNotNull(compilerArgs);
        assertEquals(
                1,
                compilerArgs.children().size(),
                "mvn:combine.children should not trigger append; only unprefixed combine.children works");
    }

    @Test
    void testUnprefixedCombineChildrenStillWorks() throws Exception {
        String dominant = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <compilerArgs combine.children="append">
                        <arg>-Xlint:deprecation</arg>
                    </compilerArgs>
                </project>
                """;

        String recessive = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <compilerArgs>
                        <arg>-Xlint:unchecked</arg>
                    </compilerArgs>
                </project>
                """;

        XmlNode dominantNode = toXmlNode(dominant);
        XmlNode recessiveNode = toXmlNode(recessive);
        XmlNode merged = XmlService.merge(dominantNode, recessiveNode);

        XmlNode compilerArgs = merged.child("compilerArgs");
        assertNotNull(compilerArgs);
        assertEquals(2, compilerArgs.children().size(), "Unprefixed combine.children=append should work");
    }

    @Test
    void testPrefixedCombineSelfDoesNotOverride() throws Exception {
        String dominant = """
                <root xmlns:mvn="http://maven.apache.org/POM/4.0.0">
                    <child mvn:combine.self="override">
                        <item>dom</item>
                    </child>
                </root>
                """;
        String recessive = """
                <root>
                    <child>
                        <item>rec</item>
                        <extra>bonus</extra>
                    </child>
                </root>
                """;
        XmlNode merged = XmlService.merge(toXmlNode(dominant), toXmlNode(recessive));
        XmlNode child = merged.child("child");

        // mvn:combine.self should NOT trigger override (only unprefixed combine.self works)
        // Default merge behavior merges children by name
        assertEquals("dom", child.child("item").value());
        // The "extra" child from recessive should survive since combine.self wasn't triggered
        assertNotNull(child.child("extra"), "Recessive children should survive since mvn:combine.self is ignored");
    }

    public static Xpp3Dom build(Reader reader) throws XmlPullParserException, IOException {
        try (Reader closeMe = reader) {
            return new Xpp3Dom(XmlNodeBuilder.build(reader, true, null));
        }
    }
}
