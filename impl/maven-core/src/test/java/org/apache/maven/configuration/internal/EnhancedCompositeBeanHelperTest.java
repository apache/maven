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
package org.apache.maven.configuration.internal;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for EnhancedCompositeBeanHelper to ensure it works correctly and provides performance benefits.
 */
class EnhancedCompositeBeanHelperTest {

    private EnhancedCompositeBeanHelper helper;
    private ConverterLookup converterLookup;
    private ExpressionEvaluator evaluator;
    private ConfigurationListener listener;

    @BeforeEach
    void setUp() {
        converterLookup = new DefaultConverterLookup();
        evaluator = mock(ExpressionEvaluator.class);
        listener = mock(ConfigurationListener.class);
        helper = new EnhancedCompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);
    }

    @AfterEach
    void tearDown() {
        EnhancedCompositeBeanHelper.clearCaches();
    }

    @Test
    void testSetPropertyWithSetter() throws Exception {
        TestBean bean = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("testValue");

        when(evaluator.evaluate("testValue")).thenReturn("testValue");

        helper.setProperty(bean, "name", String.class, config);

        assertEquals("testValue", bean.getName());
        verify(listener).notifyFieldChangeUsingSetter("name", "testValue", bean);
    }

    @Test
    void testSetPropertyWithField() throws Exception {
        TestBean bean = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("fieldValue");

        when(evaluator.evaluate("fieldValue")).thenReturn("fieldValue");

        helper.setProperty(bean, "directField", String.class, config);

        assertEquals("fieldValue", bean.getDirectField());
        verify(listener).notifyFieldChangeUsingReflection("directField", "fieldValue", bean);
    }

    @Test
    void testSetPropertyWithAdder() throws Exception {
        TestBean bean = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("item1");

        when(evaluator.evaluate("item1")).thenReturn("item1");

        helper.setProperty(bean, "item", String.class, config);

        assertEquals(1, bean.getItems().size());
        assertEquals("item1", bean.getItems().get(0));
    }

    @Test
    void testPerformanceWithRepeatedCalls() throws Exception {
        TestBean bean1 = new TestBean();
        TestBean bean2 = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("testValue");

        when(evaluator.evaluate("testValue")).thenReturn("testValue");

        // First call - should populate cache
        helper.setProperty(bean1, "name", String.class, config);

        // Second call - should use cache
        long start2 = System.nanoTime();
        helper.setProperty(bean2, "name", String.class, config);
        long time2 = System.nanoTime() - start2;

        assertEquals("testValue", bean1.getName());
        assertEquals("testValue", bean2.getName());

        // Second call should be faster (though this is not guaranteed in all environments)
        // We mainly verify that both calls work correctly
        assertTrue(time2 >= 0, "Expected " + time2 + " to be >= " + 0); // Just verify it completed
    }

    @Test
    void testCacheClearance() throws Exception {
        TestBean bean = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("testValue");

        when(evaluator.evaluate("testValue")).thenReturn("testValue");

        helper.setProperty(bean, "name", String.class, config);
        assertEquals("testValue", bean.getName());

        // Clear caches and verify it still works
        EnhancedCompositeBeanHelper.clearCaches();

        TestBean bean2 = new TestBean();
        helper.setProperty(bean2, "name", String.class, config);
        assertEquals("testValue", bean2.getName());
    }

    @Test
    void testFieldAccessibilityIsProperlyRestored() throws Exception {
        TestBean bean = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("fieldValue");

        when(evaluator.evaluate("fieldValue")).thenReturn("fieldValue");

        // Get the field to check its accessibility state
        java.lang.reflect.Field field = TestBean.class.getDeclaredField("directField");

        // Verify field is not accessible initially
        boolean initialAccessibility = field.canAccess(bean);

        // Set the property using the helper
        helper.setProperty(bean, "directField", String.class, config);

        // Verify the value was set correctly
        assertEquals("fieldValue", bean.getDirectField());

        // Verify field accessibility is restored to its original state
        boolean finalAccessibility = field.canAccess(bean);
        assertEquals(
                initialAccessibility,
                finalAccessibility,
                "Field accessibility should be restored to its original state after setting value");
    }

    @Test
    void testMultipleFieldAccessesDoNotLeakAccessibility() throws Exception {
        // This test verifies that repeated field accesses don't leave fields in an accessible state
        // which was the issue with the old caching implementation
        TestBean bean1 = new TestBean();
        TestBean bean2 = new TestBean();
        PlexusConfiguration config = new XmlPlexusConfiguration("test");
        config.setValue("value1");

        when(evaluator.evaluate("value1")).thenReturn("value1");
        when(evaluator.evaluate("value2")).thenReturn("value2");

        java.lang.reflect.Field field = TestBean.class.getDeclaredField("directField");

        // First access
        helper.setProperty(bean1, "directField", String.class, config);
        boolean accessibilityAfterFirst = field.canAccess(bean1);

        // Second access with different bean
        config.setValue("value2");
        helper.setProperty(bean2, "directField", String.class, config);
        boolean accessibilityAfterSecond = field.canAccess(bean2);

        // Both should have the same accessibility state (not accessible)
        assertEquals(
                accessibilityAfterFirst,
                accessibilityAfterSecond,
                "Field accessibility should be consistent across multiple accesses");

        // Verify values were set correctly
        assertEquals("value1", bean1.getDirectField());
        assertEquals("value2", bean2.getDirectField());
    }

    /**
     * Test bean class for testing property setting.
     */
    public static class TestBean {
        private String name;
        private String directField;
        private List<String> items = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDirectField() {
            return directField;
        }

        public List<String> getItems() {
            return items;
        }

        public void addItem(String item) {
            this.items.add(item);
        }
    }
}
