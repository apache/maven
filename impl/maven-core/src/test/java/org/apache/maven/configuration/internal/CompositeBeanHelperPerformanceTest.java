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
import java.util.Map;

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.sisu.plexus.CompositeBeanHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Performance comparison test between original CompositeBeanHelper and OptimizedCompositeBeanHelper.
 * This test is disabled by default as it's primarily for performance analysis.
 *
 * To run this test manually:
 * mvn test -Dtest=CompositeBeanHelperPerformanceTest -pl impl/maven-core
 */
@Disabled("Performance test - enable manually for benchmarking")
class CompositeBeanHelperPerformanceTest {

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
    void comparePerformance() throws Exception {
        int warmupIterations = 1000;
        int benchmarkIterations = 10000;

        System.out.println("Starting performance comparison...");
        System.out.println("Warmup iterations: " + warmupIterations);
        System.out.println("Benchmark iterations: " + benchmarkIterations);

        // Warm up JVM for both implementations
        System.out.println("Warming up JVM...");
        runOptimizedHelper(warmupIterations);
        runOriginalHelper(warmupIterations);

        // Run multiple rounds to get more stable results
        int rounds = 5;
        long totalOriginalTime = 0;
        long totalOptimizedTime = 0;

        for (int round = 0; round < rounds; round++) {
            System.gc(); // Suggest garbage collection between rounds
            Thread.sleep(100); // Brief pause

            // Clear caches for fair comparison
            OptimizedCompositeBeanHelper.clearCaches();
            long originalTime = runOriginalHelper(benchmarkIterations);

            System.gc();
            Thread.sleep(100);

            OptimizedCompositeBeanHelper.clearCaches();
            long optimizedTime = runOptimizedHelper(benchmarkIterations);

            totalOriginalTime += originalTime;
            totalOptimizedTime += optimizedTime;

            System.out.println("Round " + (round + 1) + ":");
            System.out.println("  Original: " + (originalTime / 1_000_000) + " ms");
            System.out.println("  Optimized: " + (optimizedTime / 1_000_000) + " ms");
            System.out.println("  Improvement: "
                    + String.format("%.2f", ((double) (originalTime - optimizedTime) / originalTime * 100)) + "%");
        }

        long avgOriginalTime = totalOriginalTime / rounds;
        long avgOptimizedTime = totalOptimizedTime / rounds;

        System.out.println("\n=== FINAL RESULTS ===");
        System.out.println("Average Original CompositeBeanHelper: " + (avgOriginalTime / 1_000_000) + " ms");
        System.out.println("Average Optimized CompositeBeanHelper: " + (avgOptimizedTime / 1_000_000) + " ms");
        System.out.println("Average Improvement: "
                + String.format("%.2f", ((double) (avgOriginalTime - avgOptimizedTime) / avgOriginalTime * 100)) + "%");
        System.out.println("Speedup Factor: " + String.format("%.2fx", ((double) avgOriginalTime / avgOptimizedTime)));

        // The optimized version should be faster, but we don't assert this
        // as performance can vary based on JVM, system load, etc.
    }

    private long runOriginalHelper(int iterations) throws Exception {
        CompositeBeanHelper helper =
                new CompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);

        when(evaluator.evaluate(anyString())).thenReturn("testValue");
        when(evaluator.evaluate("123")).thenReturn(123);
        when(evaluator.evaluate("true")).thenReturn(true);

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            RealisticTestBean bean = new RealisticTestBean();

            // Set multiple properties to simulate real mojo configuration
            setPropertyViaReflection(helper, bean, "name", String.class, "testValue");
            setPropertyViaReflection(helper, bean, "count", Integer.class, "123");
            setPropertyViaReflection(helper, bean, "enabled", Boolean.class, "true");
            setPropertyViaReflection(helper, bean, "description", String.class, "testValue");
            setPropertyViaReflection(helper, bean, "timeout", Long.class, "123");
        }

        return System.nanoTime() - startTime;
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

        when(evaluator.evaluate(anyString())).thenReturn("testValue");
        when(evaluator.evaluate("123")).thenReturn(123);
        when(evaluator.evaluate("true")).thenReturn(true);

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            RealisticTestBean bean = new RealisticTestBean();

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

            PlexusConfiguration descConfig = new XmlPlexusConfiguration("description");
            descConfig.setValue("testValue");
            helper.setProperty(bean, "description", String.class, descConfig);

            PlexusConfiguration timeoutConfig = new XmlPlexusConfiguration("timeout");
            timeoutConfig.setValue("123");
            helper.setProperty(bean, "timeout", Long.class, timeoutConfig);
        }

        return System.nanoTime() - startTime;
    }

    /**
     * Test bean class for performance testing.
     */
    public static class TestBean {
        private String name;
        private String description;
        private int count;
        private List<String> items = new ArrayList<>();
        private boolean enabled;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public List<String> getItems() {
            return items;
        }

        public void addItem(String item) {
            this.items.add(item);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * A more realistic test bean that simulates typical mojo parameters
     */
    public static class RealisticTestBean {
        private String name;
        private int count;
        private boolean enabled;
        private String description;
        private long timeout;
        private List<String> items;
        private Map<String, String> properties;

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

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        public List<String> getItems() {
            return items;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }
}
