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
package org.apache.maven.plugin.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V4MojoConfiguratorTest {

    private TypeAwareExpressionEvaluator evaluator;
    private PluginDescriptor pluginDescriptor;
    private V4MojoConfigurator configurator;

    @BeforeEach
    void setUp() throws Exception {
        evaluator = mock(TypeAwareExpressionEvaluator.class);
        pluginDescriptor = new PluginDescriptor();
        configurator = new V4MojoConfigurator(evaluator, getClass().getClassLoader(), pluginDescriptor);

        when(evaluator.evaluate(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evaluator.evaluate(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evaluator.alignToBaseDirectory(any(File.class))).thenAnswer(invocation -> {
            File f = invocation.getArgument(0);
            return f.isAbsolute() ? f : new File(new File("base"), f.getPath()).getAbsoluteFile();
        });
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public static class ScalarMojo {
        String str;
        int primitiveInt;
        Integer boxedInt;
        long primitiveLong;
        Long boxedLong;
        double primitiveDouble;
        Double boxedDouble;
        float primitiveFloat;
        Float boxedFloat;
        short primitiveShort;
        Short boxedShort;
        byte primitiveByte;
        Byte boxedByte;
        boolean primitiveBool;
        Boolean boxedBool;
        char primitiveChar;
        Character boxedChar;
        Status status;
        File file;
        Path path;
    }

    @Test
    void testScalarTypesConfiguration() throws Exception {
        ScalarMojo mojo = new ScalarMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(
                        new XmlNodeImpl("str", "hello"),
                        new XmlNodeImpl("primitiveInt", "42"),
                        new XmlNodeImpl("boxedInt", "43"),
                        new XmlNodeImpl("primitiveLong", "100"),
                        new XmlNodeImpl("boxedLong", "101"),
                        new XmlNodeImpl("primitiveDouble", "3.14"),
                        new XmlNodeImpl("boxedDouble", "2.71"),
                        new XmlNodeImpl("primitiveFloat", "1.5"),
                        new XmlNodeImpl("boxedFloat", "2.5"),
                        new XmlNodeImpl("primitiveShort", "10"),
                        new XmlNodeImpl("boxedShort", "11"),
                        new XmlNodeImpl("primitiveByte", "1"),
                        new XmlNodeImpl("boxedByte", "2"),
                        new XmlNodeImpl("primitiveBool", "true"),
                        new XmlNodeImpl("boxedBool", "false"),
                        new XmlNodeImpl("primitiveChar", "X"),
                        new XmlNodeImpl("boxedChar", "Y"),
                        new XmlNodeImpl("status", "ACTIVE"),
                        new XmlNodeImpl("file", "relative/file.txt"),
                        new XmlNodeImpl("path", "relative/path.txt")),
                null);

        configurator.configure(mojo, config);

        assertEquals("hello", mojo.str);
        assertEquals(42, mojo.primitiveInt);
        assertEquals(43, mojo.boxedInt);
        assertEquals(100L, mojo.primitiveLong);
        assertEquals(101L, mojo.boxedLong);
        assertEquals(3.14, mojo.primitiveDouble, 0.001);
        assertEquals(2.71, mojo.boxedDouble, 0.001);
        assertEquals(1.5f, mojo.primitiveFloat, 0.001f);
        assertEquals(2.5f, mojo.boxedFloat, 0.001f);
        assertEquals((short) 10, mojo.primitiveShort);
        assertEquals((short) 11, mojo.boxedShort);
        assertEquals((byte) 1, mojo.primitiveByte);
        assertEquals((byte) 2, mojo.boxedByte);
        assertTrue(mojo.primitiveBool);
        assertFalse(mojo.boxedBool);
        assertEquals('X', mojo.primitiveChar);
        assertEquals('Y', mojo.boxedChar);
        assertEquals(Status.ACTIVE, mojo.status);
        assertEquals(new File(new File("base"), "relative/file.txt").getAbsoluteFile(), mojo.file);
        assertEquals(Paths.get(new File(new File("base"), "relative/path.txt").getAbsolutePath()), mojo.path);
    }

    @Test
    void testUnresolvedExpressionsDoNotThrowAndUseDefaults() throws Exception {
        ExpressionEvaluator nullEvaluator = mock(ExpressionEvaluator.class);
        when(nullEvaluator.evaluate(anyString())).thenReturn(null);
        V4MojoConfigurator nullConfigurator =
                new V4MojoConfigurator(nullEvaluator, getClass().getClassLoader(), pluginDescriptor);

        ScalarMojo mojo = new ScalarMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(
                        new XmlNodeImpl("str", "${unresolved.str}"),
                        new XmlNodeImpl("boxedInt", "${unresolved.int}"),
                        new XmlNodeImpl("primitiveInt", "${unresolved.int}"),
                        new XmlNodeImpl("boxedChar", "${unresolved.char}"),
                        new XmlNodeImpl("primitiveChar", "${unresolved.char}"),
                        new XmlNodeImpl("status", "${unresolved.status}"),
                        new XmlNodeImpl("file", "${unresolved.file}")),
                null);

        nullConfigurator.configure(mojo, config);

        assertNull(mojo.str);
        assertNull(mojo.boxedInt);
        assertEquals(0, mojo.primitiveInt);
        assertNull(mojo.boxedChar);
        assertEquals('\0', mojo.primitiveChar);
        assertNull(mojo.status);
        assertNull(mojo.file);
    }

    @Test
    void testDefaultValueAttributeFallback() throws Exception {
        ExpressionEvaluator nullEvaluator = mock(ExpressionEvaluator.class);
        when(nullEvaluator.evaluate(anyString())).thenAnswer(inv -> {
            String val = inv.getArgument(0);
            return val.startsWith("${unresolved") ? null : val;
        });
        V4MojoConfigurator fallbackConfigurator =
                new V4MojoConfigurator(nullEvaluator, getClass().getClassLoader(), pluginDescriptor);

        ScalarMojo mojo = new ScalarMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(
                        new XmlNodeImpl("str", null, Map.of("default-value", "fallback-str"), List.of(), null),
                        new XmlNodeImpl(
                                "primitiveInt", "${unresolved}", Map.of("default-value", "99"), List.of(), null),
                        new XmlNodeImpl("primitiveChar", null, Map.of("default-value", "Z"), List.of(), null),
                        new XmlNodeImpl("primitiveBool", null, Map.of("default-value", "true"), List.of(), null)),
                null);

        fallbackConfigurator.configure(mojo, config);

        assertEquals("fallback-str", mojo.str);
        assertEquals(99, mojo.primitiveInt);
        assertEquals('Z', mojo.primitiveChar);
        assertTrue(mojo.primitiveBool);
    }

    public static class CollectionsMojo {
        String[] array;
        List<String> list;
        Set<String> set;
        Map<String, String> map;
        Properties properties;
    }

    @Test
    void testCollectionsAndMaps() throws Exception {
        CollectionsMojo mojo = new CollectionsMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(
                        new XmlNodeImpl(
                                "array",
                                null,
                                Map.of(),
                                List.of(new XmlNodeImpl("item", "a"), new XmlNodeImpl("item", "b")),
                                null),
                        new XmlNodeImpl(
                                "list",
                                null,
                                Map.of(),
                                List.of(new XmlNodeImpl("item", "c"), new XmlNodeImpl("item", "d")),
                                null),
                        new XmlNodeImpl(
                                "set",
                                null,
                                Map.of(),
                                List.of(new XmlNodeImpl("item", "e"), new XmlNodeImpl("item", "e")),
                                null),
                        new XmlNodeImpl(
                                "map",
                                null,
                                Map.of(),
                                List.of(new XmlNodeImpl("k1", "v1"), new XmlNodeImpl("k2", "v2")),
                                null),
                        new XmlNodeImpl("properties", null, Map.of(), List.of(new XmlNodeImpl("p1", "pv1")), null)),
                null);

        configurator.configure(mojo, config);

        assertArrayEquals(new String[] {"a", "b"}, mojo.array);
        assertEquals(List.of("c", "d"), mojo.list);
        assertEquals(Set.of("e"), mojo.set);
        assertEquals(Map.of("k1", "v1", "k2", "v2"), mojo.map);
        assertEquals("pv1", mojo.properties.getProperty("p1"));
    }

    // Package-private class with non-public constructor (issue #13459 / #13470)
    static class PackagePrivateBean {
        String name;
        int count;

        PackagePrivateBean() {}
    }

    public static class BeanMojo {
        PackagePrivateBean bean;
    }

    @Test
    void testPackagePrivateBeanInstantiation() throws Exception {
        BeanMojo mojo = new BeanMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(new XmlNodeImpl(
                        "bean",
                        null,
                        Map.of(),
                        List.of(new XmlNodeImpl("name", "pkg-private"), new XmlNodeImpl("count", "7")),
                        null)),
                null);

        configurator.configure(mojo, config);

        assertNotNull(mojo.bean);
        assertEquals("pkg-private", mojo.bean.name);
        assertEquals(7, mojo.bean.count);
    }

    // Record configuration
    public record ConfigRecord(String key, int number) {}

    public static class RecordMojo {
        ConfigRecord record;
    }

    @Test
    void testRecordConfiguration() throws Exception {
        RecordMojo mojo = new RecordMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(new XmlNodeImpl(
                        "record",
                        null,
                        Map.of(),
                        List.of(new XmlNodeImpl("key", "secret"), new XmlNodeImpl("number", "123")),
                        null)),
                null);

        configurator.configure(mojo, config);

        assertNotNull(mojo.record);
        assertEquals("secret", mojo.record.key());
        assertEquals(123, mojo.record.number());
    }

    // Sealed interface and implementations
    public sealed interface Artifact permits JarArtifact, WarArtifact {}

    public static final class JarArtifact implements Artifact {
        String classifier;
    }

    public static final class WarArtifact implements Artifact {
        String webXml;
    }

    public static class SealedMojo {
        Artifact artifact;
    }

    @Test
    void testSealedTypeImplementationHint() throws Exception {
        SealedMojo mojo = new SealedMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration",
                null,
                Map.of(),
                List.of(new XmlNodeImpl(
                        "artifact",
                        null,
                        Map.of("implementation", "JarArtifact"),
                        List.of(new XmlNodeImpl("classifier", "sources")),
                        null)),
                null);

        configurator.configure(mojo, config);

        JarArtifact jar = assertInstanceOf(JarArtifact.class, mojo.artifact);
        assertEquals("sources", jar.classifier);
    }

    // Bean with default setter
    public static class DefaultSetterBean {
        private String internal;

        public void set(String value) {
            this.internal = value;
        }

        public String getInternal() {
            return internal;
        }
    }

    public static class DefaultSetterMojo {
        DefaultSetterBean item;
    }

    @Test
    void testDefaultSetterInvocation() throws Exception {
        DefaultSetterMojo mojo = new DefaultSetterMojo();
        XmlNode config = new XmlNodeImpl(
                "configuration", null, Map.of(), List.of(new XmlNodeImpl("item", "single-value")), null);

        configurator.configure(mojo, config);

        assertNotNull(mojo.item);
        assertEquals("single-value", mojo.item.getInternal());
    }
}
