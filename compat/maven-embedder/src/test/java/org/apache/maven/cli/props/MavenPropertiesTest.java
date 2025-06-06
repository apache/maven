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
package org.apache.maven.cli.props;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests on <code>MavenProperties</code>.
 */
@Deprecated
public class MavenPropertiesTest {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String COMMENT = "# comment";
    private static final String KEY1 = "mvn:foo/bar";
    private static final String KEY1A = "mvn\\:foo/bar";
    private static final String KEY2 = "foo:bar:version:type:classifier";
    private static final String KEY2A = "foo\\:bar\\:version\\:type\\:classifier";
    private static final String VALUE1 = "value";

    private MavenProperties properties;

    static final String TEST_PROPERTIES =
            """
                #
                # test.properties
                # Used in the PropertiesTest
                #
                test=test
                """;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @BeforeEach
    public void setUp() throws Exception {
        properties = new MavenProperties();
        properties.load(new StringReader(TEST_PROPERTIES));
    }

    @Test
    public void testSpaces() throws Exception {
        String config = "\n" + "\n"
                + "    \n"
                + "                \n"
                + "   \\ \\r \\n \\t \\f\n"
                + "   \n"
                + "                                                \n"
                + "! dshfjklahfjkldashgjl;as\n"
                + "     #jdfagdfjagkdjfghksdajfd\n"
                + "     \n"
                + "!!properties\n"
                + "\n"
                + "a=a\n"
                + "b bb as,dn   \n"
                + "c\\r\\ \\t\\nu =:: cu\n"
                + "bu= b\\\n"
                + "                u\n"
                + "d=d\\r\\ne=e\n"
                + "f   :f\\\n"
                + "f\\\n"
                + "                        f\n"
                + "g               g\n"
                + "h\\u0020h\n"
                + "\\   i=i\n"
                + "j=\\   j\n"
                + "space=\\   c\n"
                + "\n"
                + "dblbackslash=\\\\\n"
                + "                        \n";

        Properties props1 = new Properties();
        props1.load(new StringReader(config));

        MavenProperties props2 = new MavenProperties();
        props2.load(new StringReader(config));

        String s325 = props1.getProperty(" \r");
        assertEquals("\n \t \f", s325, "1");
        String s324 = props1.getProperty("a");
        assertEquals("a", s324, "2");
        String s323 = props1.getProperty("b");
        assertEquals("bb as,dn   ", s323, "3");
        String s322 = props1.getProperty("c\r \t\nu");
        assertEquals(":: cu", s322, "4");
        String s321 = props1.getProperty("bu");
        assertEquals("bu", s321, "5");
        String s320 = props1.getProperty("d");
        assertEquals("d\r\ne=e", s320, "6");
        String s319 = props1.getProperty("f");
        assertEquals("fff", s319, "7");
        String s318 = props1.getProperty("g");
        assertEquals("g", s318, "8");
        String s317 = props1.getProperty("h h");
        assertEquals("", s317, "9");
        String s316 = props1.getProperty(" ");
        assertEquals("i=i", s316, "10");
        String s315 = props1.getProperty("j");
        assertEquals("   j", s315, "11");
        String s314 = props1.getProperty("space");
        assertEquals("   c", s314, "12");
        String s313 = props1.getProperty("dblbackslash");
        assertEquals("\\", s313, "13");

        String s312 = props2.getProperty(" \r");
        assertEquals("\n \t \f", s312, "1");
        String s311 = props2.getProperty("a");
        assertEquals("a", s311, "2");
        String s310 = props2.getProperty("b");
        assertEquals("bb as,dn   ", s310, "3");
        String s39 = props2.getProperty("c\r \t\nu");
        assertEquals(":: cu", s39, "4");
        String s38 = props2.getProperty("bu");
        assertEquals("bu", s38, "5");
        String s37 = props2.getProperty("d");
        assertEquals("d\r\ne=e", s37, "6");
        String s36 = props2.getProperty("f");
        assertEquals("fff", s36, "7");
        String s35 = props2.getProperty("g");
        assertEquals("g", s35, "8");
        String s34 = props2.getProperty("h h");
        assertEquals("", s34, "9");
        String s33 = props2.getProperty(" ");
        assertEquals("i=i", s33, "10");
        String s32 = props2.getProperty("j");
        assertEquals("   j", s32, "11");
        String s31 = props2.getProperty("space");
        assertEquals("   c", s31, "12");
        String s3 = props2.getProperty("dblbackslash");
        assertEquals("\\", s3, "13");
        assertEquals(props1, props2);
    }

    @Test
    public void testConfigInterpolation() throws IOException {
        String config = "a=$\\\\\\\\{var}\n" + "ab=${a}b\n" + "abc=${ab}c";
        Map<String, String> expected = Map.of("a", "$\\{var}", "ab", "$\\{var}b", "abc", "$\\{var}bc");

        Properties props1 = new Properties();
        props1.load(new StringReader(config));
        new DefaultInterpolator().performSubstitution((Map) props1, null, true);
        assertEquals(expected, props1);

        MavenProperties props2 = new MavenProperties();
        props2.load(new StringReader(config));
        assertEquals(expected, props2);
    }

    /**
     * <p>
     * Test getting property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testGettingProperty() throws Exception {
        Object o2 = properties.get("test");
        assertEquals("test", o2);
    }

    @Test
    public void testLoadSave() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("# ");
        pw.println("# The Main  ");
        pw.println("# ");
        pw.println("# Comment ");
        pw.println("# ");
        pw.println("");
        pw.println("# Another comment");
        pw.println("");
        pw.println("# A value comment");
        pw.println("key1 = val1");
        pw.println("");
        pw.println("# Another value comment");
        pw.println("key2 = ${key1}/foo");
        pw.println("");
        pw.println("# A third comment");
        pw.println("key3 = val3");
        pw.println("");

        MavenProperties props = new MavenProperties();
        props.load(new StringReader(sw.toString()));
        props.save(System.err);
        System.err.println("=====");

        props.put("key2", props.get("key2"));
        props.put("key3", "foo");
        props.save(System.err);
        System.err.println("=====");
    }

    @Test
    public void testJavaUtilPropertiesCompatibility() throws Exception {
        MavenProperties properties = new MavenProperties();
        properties.load(new StringReader(TEST_PROPERTIES));

        String test = properties.getProperty("test");
        assertEquals("test", test);

        String defaultValue = properties.getProperty("notfound", "default");
        assertEquals("default", defaultValue);

        properties.setProperty("another", "another");
        Object o1 = properties.getProperty("another");
        assertEquals("another", o1);

        properties.store(System.err, null);
        System.err.println("====");
    }

    private static final String RESULT1 = COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    public void testSaveComment1() throws Exception {
        properties.put(KEY1, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertTrue(sw.toString().endsWith(RESULT1), msg);
    }

    private static final String RESULT1A = COMMENT + LINE_SEPARATOR + KEY2A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    public void testSaveComment1a() throws Exception {
        properties.put(KEY2, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertTrue(sw.toString().endsWith(RESULT1A), msg);
    }

    private static final String RESULT2 =
            COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    public void testSaveComment2() throws Exception {
        properties.put(KEY1, List.of(new String[] {COMMENT, COMMENT}), VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertTrue(sw.toString().endsWith(RESULT2), msg);
    }

    private static final String RESULT3 = COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1
            + "\\" + LINE_SEPARATOR + VALUE1 + LINE_SEPARATOR;

    @Test
    public void testSaveComment3() throws Exception {
        properties.put(KEY1, List.of(new String[] {COMMENT, COMMENT}), List.of(new String[] {VALUE1, VALUE1}));
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertTrue(sw.toString().endsWith(RESULT3), msg);
        List<String> rawValue = properties.getRaw(KEY1);
        assertEquals(2, (Object) rawValue.size());
        assertEquals(KEY1A + " = " + VALUE1, rawValue.get(0));
        assertEquals(VALUE1, rawValue.get(1));
    }

    @Test
    public void testEntrySetValue() throws Exception {
        properties.put(KEY1, VALUE1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.save(baos);

        properties = new MavenProperties();
        properties.load(new ByteArrayInputStream(baos.toByteArray()));
        Object o22 = properties.get(KEY1);
        assertEquals(VALUE1, o22);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            entry.setValue(entry.getValue() + "x");
        }
        Object o21 = properties.get(KEY1);
        assertEquals(VALUE1 + "x", o21);

        baos = new ByteArrayOutputStream();
        properties.save(baos);

        properties = new MavenProperties();
        properties.load(new ByteArrayInputStream(baos.toByteArray()));
        Object o2 = properties.get(KEY1);
        assertEquals(VALUE1 + "x", o2);
    }

    @Test
    public void testMultiValueEscaping() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("fruits                           apple, banana, pear, \\");
        pw.println("                                 cantaloupe, watermelon, \\");
        pw.println("                                 kiwi, mango");

        Properties p = new Properties();
        p.load(new StringReader(sw.toString()));
        Object o24 = p.getProperty("fruits");
        assertEquals("apple, banana, pear, cantaloupe, watermelon, kiwi, mango", o24);

        MavenProperties props = new MavenProperties();
        props.load(new StringReader(sw.toString()));
        Object o23 = props.getProperty("fruits");
        assertEquals("apple, banana, pear, cantaloupe, watermelon, kiwi, mango", o23);
        List<String> raw = props.getRaw("fruits");
        assertNotNull(raw);
        assertEquals(3, (Object) raw.size());
        assertEquals("fruits                           apple, banana, pear, ", raw.get(0));

        props = new MavenProperties();
        props.put(
                "fruits",
                props.getComments("fruits"),
                List.of(
                        "fruits                           apple, banana, pear, ",
                        "                                 cantaloupe, watermelon, ",
                        "                                 kiwi, mango"));
        Object o22 = props.getProperty("fruits");
        assertEquals("apple, banana, pear, cantaloupe, watermelon, kiwi, mango", o22);
        raw = props.getRaw("fruits");
        assertNotNull(raw);
        assertEquals(3, (Object) raw.size());
        assertEquals("fruits                           apple, banana, pear, ", raw.get(0));

        sw = new StringWriter();
        props.save(sw);
        props = new MavenProperties();
        props.load(new StringReader(sw.toString()));
        Object o21 = props.getProperty("fruits");
        assertEquals("apple, banana, pear, cantaloupe, watermelon, kiwi, mango", o21);
        raw = props.getRaw("fruits");
        assertNotNull(raw);
        assertEquals(3, (Object) raw.size());
        assertEquals("fruits                           apple, banana, pear, ", raw.get(0));

        props = new MavenProperties();
        props.put(
                "fruits",
                props.getComments("fruits"),
                List.of(
                        "                           apple, banana, pear, ",
                        "                                 cantaloupe, watermelon, ",
                        "                                 kiwi, mango"));
        Object o2 = props.getProperty("fruits");
        assertEquals("apple, banana, pear, cantaloupe, watermelon, kiwi, mango", o2);
        raw = props.getRaw("fruits");
        assertNotNull(raw);
        assertEquals(3, (Object) raw.size());
        assertEquals("fruits =                            apple, banana, pear, ", raw.get(0));
    }

    @Test
    public void testUpdate() throws Exception {
        MavenProperties p1 = new MavenProperties();
        p1.put(
                "fruits",
                List.of("#", "# List of fruits", "#"),
                List.of(
                        "                           apple, banana, pear, ",
                        "                                 cantaloupe, watermelon, ",
                        "                                 kiwi, mango"));
        p1.put("trees", List.of("#", "# List of trees", "#"), List.of("                           fir, oak, maple"));
        p1.put("vegetables", List.of("#", "# List of vegetables", "#"), List.of("                           potatoes"));

        MavenProperties p2 = new MavenProperties();
        p2.put(
                "fruits",
                List.of("#", "# List of good fruits", "#"),
                List.of("                           apple, banana, pear"));
        p2.put("trees", "fir, oak, maple");
        p1.update(p2);

        assertEquals(2, (Object) p1.size());
        Object o23 = p1.getComments("trees");
        assertEquals(List.of("#", "# List of trees", "#"), o23);
        Object o22 = p1.getProperty("trees");
        assertEquals("fir, oak, maple", o22);
        Object o21 = p1.getComments("fruits");
        assertEquals(List.of("#", "# List of good fruits", "#"), o21);
        Object o2 = p1.getProperty("fruits");
        assertEquals("apple, banana, pear", o2);
    }

    @Test
    public void testSubstitution() throws IOException {
        String str = "port = 4141" + LINE_SEPARATOR + "host = localhost"
                + LINE_SEPARATOR + "url = https://${host}:${port}/service"
                + LINE_SEPARATOR;
        MavenProperties properties = new MavenProperties();
        properties.load(new StringReader(str));
        properties.put("url", "https://localhost:4141/service");
        StringWriter sw = new StringWriter();
        properties.save(sw);
        Object o2 = sw.toString();
        assertEquals(str, o2);
    }
}
