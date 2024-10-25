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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        XmlNode mergeResult = leftDom.merge(rightDom);

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

        XmlNode mergeResult1 = dom.merge(dom, false);
        assertEquals(res, mergeResult1);
        XmlNode mergeResult2 = dom.merge(dom, (Boolean) null);
        assertEquals(res, mergeResult2);
        XmlNode mergeResult3 = dom.merge(dom, true);
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

        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

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
    void testCombineKeys() throws Exception {
        String lhs = "<props combine.keys='key'>"
                + "<property key=\"LHS-ONLY\"><name>LHS-ONLY</name><value>LHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>LHS</value></property>" + "</props>";

        String rhs = "<props combine.keys='key'>"
                + "<property key=\"RHS-ONLY\"><name>RHS-ONLY</name><value>RHS</value></property>"
                + "<property combine.keys='name'><name>TOOVERWRITE</name><value>RHS</value></property>" + "</props>";

        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

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
    void testPreserveDominantBlankValue() throws XMLStreamException, IOException {
        String lhs = "<parameter xml:space=\"preserve\"> </parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals(" ", mergeResult.getValue());
    }

    @Test
    void testPreserveDominantEmptyNode() throws XMLStreamException, IOException {
        String lhs = "<parameter></parameter>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertEquals("", mergeResult.getValue());
    }

    @Test
    void testPreserveDominantEmptyNode2() throws XMLStreamException, IOException {
        String lhs = "<parameter/>";

        String rhs = "<parameter>recessive</parameter>";

        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode mergeResult = XmlNodeImpl.merge(leftDom, rightDom, true);
        assertNull(mergeResult.getValue());
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
        XmlNodeImpl leftDom = XmlNodeStaxBuilder.build(new StringReader(lhs), new FixedInputLocationBuilder("left"));
        XmlNodeImpl rightDom = XmlNodeStaxBuilder.build(new StringReader(rhs), new FixedInputLocationBuilder("right"));

        XmlNode result = XmlNode.merge(leftDom, rightDom);
        assertEquals(4, getChildren(result, "topsub1").size());
        assertEquals("t2s1Value", getChildren(result, "topsub1").get(0).getValue());
        assertEquals("t2s2Value", getChildren(result, "topsub1").get(1).getValue());
        assertEquals("t1s1Value", getChildren(result, "topsub1").get(2).getValue());
        assertEquals("t1s2Value", getChildren(result, "topsub1").get(3).getValue());

        assertEquals("left", result.getInputLocation());
        assertEquals("right", getChildren(result, "topsub1").get(0).getInputLocation());
        assertEquals("right", getChildren(result, "topsub1").get(1).getInputLocation());
        assertEquals("left", getChildren(result, "topsub1").get(2).getInputLocation());
        assertEquals("left", getChildren(result, "topsub1").get(3).getInputLocation());
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
        XmlNodeImpl dom = new XmlNodeImpl("top");

        assertEquals(dom, dom);
        assertNotEquals(dom, null);
        assertNotEquals(dom, new XmlNodeImpl(""));
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
        Xpp3Dom dom2 = new Xpp3Dom(new XmlNodeImpl(dom.getName(), null, attributes, childList, null));

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

        XmlNode result = XmlNode.merge(childConfig, parentConfig);
        XmlNode items = result.getChild("items");

        assertEquals(1, items.getChildren().size());

        XmlNode item = items.getChildren().get(0);
        assertEquals("three", item.getValue());
        assertEquals("child", item.getInputLocation());
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

        XmlNode result = XmlNode.merge(childConfig, parentConfig);
        XmlNode items = result.getChild("items");

        XmlNode[] item = items.getChildren().toArray(new XmlNode[0]);
        assertEquals(3, item.length);
        assertEquals("one", item[0].getValue());
        assertEquals("parent", item[0].getInputLocation());
        assertEquals("two", item[1].getValue());
        assertEquals("parent", item[1].getInputLocation());
        assertEquals("three", item[2].getValue());
        assertEquals("child", item[2].getInputLocation());
    }

    /**
     * <p>testShouldNotChangeUponMergeWithItselfWhenFirstOrLastSubItemIsEmpty.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @Test
    void testShouldNotChangeUponMergeWithItselfWhenFirstOrLastSubItemIsEmpty() throws Exception {
        String configStr = "<configuration><items><item/><item>test</item><item/></items></configuration>";
        Xpp3Dom dominantConfig = Xpp3DomBuilder.build(new StringReader(configStr));
        Xpp3Dom recessiveConfig = Xpp3DomBuilder.build(new StringReader(configStr));

        Xpp3Dom result = Xpp3Dom.mergeXpp3Dom(dominantConfig, recessiveConfig);
        Xpp3Dom items = result.getChild("items");

        assertEquals(3, items.getChildCount());

        assertNull(items.getChild(0).getValue());
        assertEquals("test", items.getChild(1).getValue());
        assertNull(items.getChild(2).getValue());
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
        Xpp3Dom dominantConfig = Xpp3DomBuilder.build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = Xpp3DomBuilder.build(new StringReader(recessiveStr));

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
        assertEquals("y", dom.getChild("foo").getValue());
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
        Xpp3Dom dominantConfig = Xpp3DomBuilder.build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = Xpp3DomBuilder.build(new StringReader(recessiveStr));

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
        Xpp3Dom dominantConfig = Xpp3DomBuilder.build(new StringReader(dominantStr));
        Xpp3Dom recessiveConfig = Xpp3DomBuilder.build(new StringReader(recessiveStr));

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
        XmlNode m = d.merge(r);
        assertEquals(expected, m.toString().replaceAll("\r\n", "\n"));
    }

    private static List<XmlNode> getChildren(XmlNode node, String name) {
        return node.getChildren().stream().filter(n -> n.getName().equals(name)).toList();
    }

    private static XmlNode getNthChild(XmlNode node, String name, int nth) {
        return node.getChildren().stream()
                .filter(n -> n.getName().equals(name))
                .skip(nth)
                .findFirst()
                .orElse(null);
    }

    private static XmlNode toXmlNode(String xml) throws XMLStreamException, IOException {
        return toXmlNode(xml, null);
    }

    private static XmlNode toXmlNode(String xml, XmlNodeStaxBuilder.InputLocationBuilderStax locationBuilder)
            throws XMLStreamException, IOException {
        return toXmlNode(new StringReader(xml), locationBuilder);
    }

    private static XmlNode toXmlNode(Reader reader) throws XMLStreamException, IOException {
        return toXmlNode(reader, null);
    }

    private static XmlNode toXmlNode(Reader reader, XmlNodeStaxBuilder.InputLocationBuilderStax locationBuilder)
            throws XMLStreamException, IOException {
        return XmlNodeStaxBuilder.build(reader, locationBuilder);
    }

    private static class FixedInputLocationBuilder implements XmlNodeStaxBuilder.InputLocationBuilderStax {
        private final Object location;

        public FixedInputLocationBuilder(Object location) {
            this.location = location;
        }

        public Object toInputLocation(XMLStreamReader parser) {
            return location;
        }
    }
}
