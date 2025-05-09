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

import org.apache.maven.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests on <code>MavenProperties</code>.
 */
@Deprecated
class MavenPropertiesTest {

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
    void setUp() throws Exception {
        properties = new MavenProperties();
        properties.load(new StringReader(TEST_PROPERTIES));
    }

    @Test
    void spaces() throws Exception {
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

        java.util.Properties props1 = new java.util.Properties();
        props1.load(new StringReader(config));

        MavenProperties props2 = new MavenProperties();
        props2.load(new StringReader(config));

        String s325 = props1.getProperty(" \r");
        assertThat(s325).as("1").isEqualTo("\n \t \f");
        String s324 = props1.getProperty("a");
        assertThat(s324).as("2").isEqualTo("a");
        String s323 = props1.getProperty("b");
        assertThat(s323).as("3").isEqualTo("bb as,dn   ");
        String s322 = props1.getProperty("c\r \t\nu");
        assertThat(s322).as("4").isEqualTo(":: cu");
        String s321 = props1.getProperty("bu");
        assertThat(s321).as("5").isEqualTo("bu");
        String s320 = props1.getProperty("d");
        assertThat(s320).as("6").isEqualTo("d\r\ne=e");
        String s319 = props1.getProperty("f");
        assertThat(s319).as("7").isEqualTo("fff");
        String s318 = props1.getProperty("g");
        assertThat(s318).as("8").isEqualTo("g");
        String s317 = props1.getProperty("h h");
        assertThat(s317).as("9").isEqualTo("");
        String s316 = props1.getProperty(" ");
        assertThat(s316).as("10").isEqualTo("i=i");
        String s315 = props1.getProperty("j");
        assertThat(s315).as("11").isEqualTo("   j");
        String s314 = props1.getProperty("space");
        assertThat(s314).as("12").isEqualTo("   c");
        String s313 = props1.getProperty("dblbackslash");
        assertThat(s313).as("13").isEqualTo("\\");

        String s312 = props2.getProperty(" \r");
        assertThat(s312).as("1").isEqualTo("\n \t \f");
        String s311 = props2.getProperty("a");
        assertThat(s311).as("2").isEqualTo("a");
        String s310 = props2.getProperty("b");
        assertThat(s310).as("3").isEqualTo("bb as,dn   ");
        String s39 = props2.getProperty("c\r \t\nu");
        assertThat(s39).as("4").isEqualTo(":: cu");
        String s38 = props2.getProperty("bu");
        assertThat(s38).as("5").isEqualTo("bu");
        String s37 = props2.getProperty("d");
        assertThat(s37).as("6").isEqualTo("d\r\ne=e");
        String s36 = props2.getProperty("f");
        assertThat(s36).as("7").isEqualTo("fff");
        String s35 = props2.getProperty("g");
        assertThat(s35).as("8").isEqualTo("g");
        String s34 = props2.getProperty("h h");
        assertThat(s34).as("9").isEqualTo("");
        String s33 = props2.getProperty(" ");
        assertThat(s33).as("10").isEqualTo("i=i");
        String s32 = props2.getProperty("j");
        assertThat(s32).as("11").isEqualTo("   j");
        String s31 = props2.getProperty("space");
        assertThat(s31).as("12").isEqualTo("   c");
        String s3 = props2.getProperty("dblbackslash");
        assertThat(s3).as("13").isEqualTo("\\");
        assertThat(props2).isEqualTo(props1);
    }

    @Test
    void configInterpolation() throws IOException {
        String config = "a=$\\\\\\\\{var}\n" + "ab=${a}b\n" + "abc=${ab}c";
        Map<String, String> expected = Map.of("a", "$\\{var}", "ab", "$\\{var}b", "abc", "$\\{var}bc");

        java.util.Properties props1 = new java.util.Properties();
        props1.load(new StringReader(config));
        new DefaultInterpolator().performSubstitution((Map) props1, null, true);
        assertThat(props1).isEqualTo(expected);

        MavenProperties props2 = new MavenProperties();
        props2.load(new StringReader(config));
        assertThat(props2).isEqualTo(expected);
    }

    /**
     * <p>
     * Test getting property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    void gettingProperty() throws Exception {
        Object o2 = properties.get("test");
        assertThat(o2).isEqualTo("test");
    }

    @Test
    void loadSave() throws IOException {
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
    void javaUtilPropertiesCompatibility() throws Exception {
        MavenProperties properties = new MavenProperties();
        properties.load(new StringReader(TEST_PROPERTIES));

        String test = properties.getProperty("test");
        assertThat(test).isEqualTo("test");

        String defaultValue = properties.getProperty("notfound", "default");
        assertThat(defaultValue).isEqualTo("default");

        properties.setProperty("another", "another");
        Object o1 = properties.getProperty("another");
        assertThat(o1).isEqualTo("another");

        properties.store(System.err, null);
        System.err.println("====");
    }

    private static final String RESULT1 = COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    void saveComment1() throws Exception {
        properties.put(KEY1, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertThat(sw.toString().endsWith(RESULT1)).as(msg).isTrue();
    }

    private static final String RESULT1A = COMMENT + LINE_SEPARATOR + KEY2A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    void saveComment1a() throws Exception {
        properties.put(KEY2, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertThat(sw.toString().endsWith(RESULT1A)).as(msg).isTrue();
    }

    private static final String RESULT2 =
            COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    @Test
    void saveComment2() throws Exception {
        properties.put(KEY1, List.of(new String[] {COMMENT, COMMENT}), VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertThat(sw.toString().endsWith(RESULT2)).as(msg).isTrue();
    }

    private static final String RESULT3 = COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1
            + "\\" + LINE_SEPARATOR + VALUE1 + LINE_SEPARATOR;

    @Test
    void saveComment3() throws Exception {
        properties.put(KEY1, List.of(new String[] {COMMENT, COMMENT}), List.of(new String[] {VALUE1, VALUE1}));
        StringWriter sw = new StringWriter();
        properties.save(sw);
        String msg = sw.toString();
        assertThat(sw.toString().endsWith(RESULT3)).as(msg).isTrue();
        List<String> rawValue = properties.getRaw(KEY1);
        assertThat((Object) rawValue.size()).isEqualTo(2);
        assertThat(rawValue.get(0)).isEqualTo(KEY1A + " = " + VALUE1);
        assertThat(rawValue.get(1)).isEqualTo(VALUE1);
    }

    @Test
    void entrySetValue() throws Exception {
        properties.put(KEY1, VALUE1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.save(baos);

        properties = new MavenProperties();
        properties.load(new ByteArrayInputStream(baos.toByteArray()));
        Object o22 = properties.get(KEY1);
        assertThat(o22).isEqualTo(VALUE1);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            entry.setValue(entry.getValue() + "x");
        }
        Object o21 = properties.get(KEY1);
        assertThat(o21).isEqualTo(VALUE1 + "x");

        baos = new ByteArrayOutputStream();
        properties.save(baos);

        properties = new MavenProperties();
        properties.load(new ByteArrayInputStream(baos.toByteArray()));
        Object o2 = properties.get(KEY1);
        assertThat(o2).isEqualTo(VALUE1 + "x");
    }

    @Test
    void multiValueEscaping() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("fruits                           apple, banana, pear, \\");
        pw.println("                                 cantaloupe, watermelon, \\");
        pw.println("                                 kiwi, mango");

        java.util.Properties p = new java.util.Properties();
        p.load(new StringReader(sw.toString()));
        Object o24 = p.getProperty("fruits");
        assertThat(o24).isEqualTo("apple, banana, pear, cantaloupe, watermelon, kiwi, mango");

        MavenProperties props = new MavenProperties();
        props.load(new StringReader(sw.toString()));
        Object o23 = props.getProperty("fruits");
        assertThat(o23).isEqualTo("apple, banana, pear, cantaloupe, watermelon, kiwi, mango");
        List<String> raw = props.getRaw("fruits");
        assertThat(raw).isNotNull();
        assertThat((Object) raw.size()).isEqualTo(3);
        assertThat(raw.get(0)).isEqualTo("fruits                           apple, banana, pear, ");

        props = new MavenProperties();
        props.put(
                "fruits",
                props.getComments("fruits"),
                List.of(
                        "fruits                           apple, banana, pear, ",
                        "                                 cantaloupe, watermelon, ",
                        "                                 kiwi, mango"));
        Object o22 = props.getProperty("fruits");
        assertThat(o22).isEqualTo("apple, banana, pear, cantaloupe, watermelon, kiwi, mango");
        raw = props.getRaw("fruits");
        assertThat(raw).isNotNull();
        assertThat((Object) raw.size()).isEqualTo(3);
        assertThat(raw.get(0)).isEqualTo("fruits                           apple, banana, pear, ");

        sw = new StringWriter();
        props.save(sw);
        props = new MavenProperties();
        props.load(new StringReader(sw.toString()));
        Object o21 = props.getProperty("fruits");
        assertThat(o21).isEqualTo("apple, banana, pear, cantaloupe, watermelon, kiwi, mango");
        raw = props.getRaw("fruits");
        assertThat(raw).isNotNull();
        assertThat((Object) raw.size()).isEqualTo(3);
        assertThat(raw.get(0)).isEqualTo("fruits                           apple, banana, pear, ");

        props = new MavenProperties();
        props.put(
                "fruits",
                props.getComments("fruits"),
                List.of(
                        "                           apple, banana, pear, ",
                        "                                 cantaloupe, watermelon, ",
                        "                                 kiwi, mango"));
        Object o2 = props.getProperty("fruits");
        assertThat(o2).isEqualTo("apple, banana, pear, cantaloupe, watermelon, kiwi, mango");
        raw = props.getRaw("fruits");
        assertThat(raw).isNotNull();
        assertThat((Object) raw.size()).isEqualTo(3);
        assertThat(raw.get(0)).isEqualTo("fruits =                            apple, banana, pear, ");
    }

    @Test
    void update() throws Exception {
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

        assertThat((Object) p1.size()).isEqualTo(2);
        Object o23 = p1.getComments("trees");
        assertThat(o23).isEqualTo(List.of("#", "# List of trees", "#"));
        Object o22 = p1.getProperty("trees");
        assertThat(o22).isEqualTo("fir, oak, maple");
        Object o21 = p1.getComments("fruits");
        assertThat(o21).isEqualTo(List.of("#", "# List of good fruits", "#"));
        Object o2 = p1.getProperty("fruits");
        assertThat(o2).isEqualTo("apple, banana, pear");
    }

    @Test
    void substitution() throws IOException {
        String str = "port = 4141" + LINE_SEPARATOR + "host = localhost"
                + LINE_SEPARATOR + "url = https://${host}:${port}/service"
                + LINE_SEPARATOR;
        MavenProperties properties = new MavenProperties();
        properties.load(new StringReader(str));
        properties.put("url", "https://localhost:4141/service");
        StringWriter sw = new StringWriter();
        properties.save(sw);
        Object o2 = sw.toString();
        assertThat(o2).isEqualTo(str);
    }
}
