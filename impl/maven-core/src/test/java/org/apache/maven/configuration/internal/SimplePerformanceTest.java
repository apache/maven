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

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.sisu.plexus.CompositeBeanHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simple performance test to measure actual performance differences.
 */
class SimplePerformanceTest {

    private ConverterLookup converterLookup;
    private ExpressionEvaluator evaluator;
    private ConfigurationListener listener;

    @BeforeEach
    void setUp() {
        converterLookup = new DefaultConverterLookup();
        evaluator = mock(ExpressionEvaluator.class);
        listener = mock(ConfigurationListener.class);
    }

    @Test
    void measureActualPerformance() throws Exception {
        int iterations = 1000;

        System.out.println("=== ACTUAL PERFORMANCE MEASUREMENT ===");
        System.out.println("Iterations: " + iterations);

        // Setup mocks
        when(evaluator.evaluate(anyString())).thenReturn("testValue");
        when(evaluator.evaluate("123")).thenReturn(123);
        when(evaluator.evaluate("true")).thenReturn(true);

        // Warmup
        runOptimizedHelper(100);
        runOriginalHelper(100);

        // Clear caches for fair comparison
        OptimizedCompositeBeanHelper.clearCaches();

        // Measure original implementation
        long originalStart = System.nanoTime();
        runOriginalHelper(iterations);
        long originalTime = System.nanoTime() - originalStart;

        // Clear caches and measure optimized implementation
        OptimizedCompositeBeanHelper.clearCaches();
        long optimizedStart = System.nanoTime();
        runOptimizedHelper(iterations);
        long optimizedTime = System.nanoTime() - optimizedStart;

        // Report results
        System.out.println("\nResults:");
        System.out.println("Original CompositeBeanHelper: " + (originalTime / 1_000_000) + " ms");
        System.out.println("Optimized CompositeBeanHelper: " + (optimizedTime / 1_000_000) + " ms");

        if (originalTime > optimizedTime) {
            double improvement = ((double) (originalTime - optimizedTime) / originalTime * 100);
            double speedup = ((double) originalTime / optimizedTime);
            System.out.println("Improvement: " + String.format("%.2f", improvement) + "%");
            System.out.println("Speedup: " + String.format("%.2fx", speedup));
        } else {
            System.out.println("No improvement detected (may be due to JVM warmup or test environment)");
        }
    }

    private long runOriginalHelper(int iterations) throws Exception {
        CompositeBeanHelper helper =
                new CompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);

        for (int i = 0; i < iterations; i++) {
            TestBean bean = new TestBean();

            // Set multiple properties to simulate real mojo configuration
            setPropertyViaReflection(helper, bean, "name", String.class, "testValue");
            setPropertyViaReflection(helper, bean, "count", Integer.class, "123");
            setPropertyViaReflection(helper, bean, "enabled", Boolean.class, "true");
        }

        return 0; // We're measuring externally
    }

    private void setPropertyViaReflection(
            CompositeBeanHelper helper, Object bean, String propertyName, Class<?> type, String value) {
        try {
            PlexusConfiguration config = new XmlPlexusConfiguration(propertyName);
            config.setValue(value);

            java.lang.reflect.Method setPropertyMethod = CompositeBeanHelper.class.getDeclaredMethod(
                    "setProperty", Object.class, String.class, Class.class, PlexusConfiguration.class);
            setPropertyMethod.setAccessible(true);
            setPropertyMethod.invoke(helper, bean, propertyName, type, config);
        } catch (Exception e) {
            // If reflection fails, skip this property
        }
    }

    private long runOptimizedHelper(int iterations) throws Exception {
        OptimizedCompositeBeanHelper helper =
                new OptimizedCompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);

        for (int i = 0; i < iterations; i++) {
            TestBean bean = new TestBean();

            // Set multiple properties to simulate real mojo configuration
            PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
            nameConfig.setValue("testValue");
            helper.setProperty(bean, "name", String.class, nameConfig);

            PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
            countConfig.setValue("123");
            helper.setProperty(bean, "count", Integer.class, countConfig);

            PlexusConfiguration enabledConfig = new XmlPlexusConfiguration("enabled");
            enabledConfig.setValue("true");
            helper.setProperty(bean, "enabled", Boolean.class, enabledConfig);
        }

        return 0; // We're measuring externally
    }

    /**
     * Simple test bean for performance testing.
     */
    public static class TestBean {
        private String name;
        private int count;
        private boolean enabled;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
