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
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.sisu.plexus.CompositeBeanHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Performance comparison test between original CompositeBeanHelper and OptimizedCompositeBeanHelper.
 * This test uses JMH (Java Microbenchmark Harness) for accurate performance measurement.
 *
 * To run this benchmark:
 * mvn test -Dtest=CompositeBeanHelperPerformanceTest -pl impl/maven-core
 *
 * The main method will execute the JMH benchmarks with the configured parameters.
 *
 * IMPORTANT: Caches are only cleared between trials (10-second periods), not between individual
 * iterations, to properly test the cache benefits within each measurement period.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 10)
@Fork(1)
@State(Scope.Benchmark)
public class CompositeBeanHelperPerformanceTest {

    private ConverterLookup converterLookup;
    private ExpressionEvaluator evaluator;
    private ConfigurationListener listener;
    private CompositeBeanHelper originalHelper;
    private EnhancedCompositeBeanHelper optimizedHelper;

    @Setup(Level.Trial)
    @BeforeEach
    public void setUp() throws ExpressionEvaluationException {
        converterLookup = new DefaultConverterLookup();
        evaluator = mock(ExpressionEvaluator.class);
        listener = mock(ConfigurationListener.class);

        when(evaluator.evaluate(anyString())).thenReturn("testValue");
        for (int i = 0; i < 10; i++) {
            when(evaluator.evaluate(Integer.toString(i))).thenReturn(i);
        }
        when(evaluator.evaluate("123")).thenReturn(123);
        when(evaluator.evaluate("456")).thenReturn(456);
        when(evaluator.evaluate("true")).thenReturn(true);

        originalHelper = new CompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);
        optimizedHelper =
                new EnhancedCompositeBeanHelper(converterLookup, getClass().getClassLoader(), evaluator, listener);
    }

    @TearDown(Level.Trial)
    @AfterEach
    public void tearDown() {
        // Clear caches between trials (10-second periods) to allow cache benefits within each trial
        EnhancedCompositeBeanHelper.clearCaches();
    }

    @Benchmark
    public void benchmarkOriginalHelper() throws Exception {
        RealisticTestBean bean = new RealisticTestBean();

        // Set multiple properties to simulate real mojo configuration
        // Use direct method calls instead of reflection for fair comparison
        PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
        nameConfig.setValue("testValue");
        originalHelper.setProperty(bean, "name", String.class, nameConfig);

        PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
        countConfig.setValue("123");
        originalHelper.setProperty(bean, "count", Integer.class, countConfig);

        PlexusConfiguration enabledConfig = new XmlPlexusConfiguration("enabled");
        enabledConfig.setValue("true");
        originalHelper.setProperty(bean, "enabled", Boolean.class, enabledConfig);

        PlexusConfiguration descConfig = new XmlPlexusConfiguration("description");
        descConfig.setValue("testValue");
        originalHelper.setProperty(bean, "description", String.class, descConfig);

        PlexusConfiguration timeoutConfig = new XmlPlexusConfiguration("timeout");
        timeoutConfig.setValue("123");
        originalHelper.setProperty(bean, "timeout", Long.class, timeoutConfig);
    }

    @Benchmark
    public void benchmarkOptimizedHelper() throws Exception {
        RealisticTestBean bean = new RealisticTestBean();

        // Set multiple properties to simulate real mojo configuration
        PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
        nameConfig.setValue("testValue");
        optimizedHelper.setProperty(bean, "name", String.class, nameConfig);

        PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
        countConfig.setValue("123");
        optimizedHelper.setProperty(bean, "count", Integer.class, countConfig);

        PlexusConfiguration enabledConfig = new XmlPlexusConfiguration("enabled");
        enabledConfig.setValue("true");
        optimizedHelper.setProperty(bean, "enabled", Boolean.class, enabledConfig);

        PlexusConfiguration descConfig = new XmlPlexusConfiguration("description");
        descConfig.setValue("testValue");
        optimizedHelper.setProperty(bean, "description", String.class, descConfig);

        PlexusConfiguration timeoutConfig = new XmlPlexusConfiguration("timeout");
        timeoutConfig.setValue("123");
        optimizedHelper.setProperty(bean, "timeout", Long.class, timeoutConfig);
    }

    /**
     * Benchmark that tests multiple property configurations in a single operation.
     * This simulates a more realistic scenario where multiple properties are set on a bean.
     */
    @Benchmark
    public void benchmarkOriginalHelperMultipleProperties() throws Exception {
        RealisticTestBean bean = new RealisticTestBean();

        // Set multiple properties in one benchmark iteration
        PlexusConfiguration config6 = new XmlPlexusConfiguration("name");
        config6.setValue("testValue");
        originalHelper.setProperty(bean, "name", String.class, config6);
        PlexusConfiguration config5 = new XmlPlexusConfiguration("count");
        config5.setValue("123");
        originalHelper.setProperty(bean, "count", Integer.class, config5);
        PlexusConfiguration config4 = new XmlPlexusConfiguration("enabled");
        config4.setValue("true");
        originalHelper.setProperty(bean, "enabled", Boolean.class, config4);
        PlexusConfiguration config3 = new XmlPlexusConfiguration("description");
        config3.setValue("testValue");
        originalHelper.setProperty(bean, "description", String.class, config3);
        PlexusConfiguration config2 = new XmlPlexusConfiguration("timeout");
        config2.setValue("123");
        originalHelper.setProperty(bean, "timeout", Long.class, config2);
        // Repeat to test caching
        PlexusConfiguration config1 = new XmlPlexusConfiguration("name");
        config1.setValue("testValue2");
        originalHelper.setProperty(bean, "name", String.class, config1);
        PlexusConfiguration config = new XmlPlexusConfiguration("count");
        config.setValue("456");
        originalHelper.setProperty(bean, "count", Integer.class, config);
    }

    @Benchmark
    public void benchmarkOptimizedHelperMultipleProperties() throws Exception {
        RealisticTestBean bean = new RealisticTestBean();

        // Set multiple properties in one benchmark iteration
        PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
        nameConfig.setValue("testValue");
        optimizedHelper.setProperty(bean, "name", String.class, nameConfig);

        PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
        countConfig.setValue("123");
        optimizedHelper.setProperty(bean, "count", Integer.class, countConfig);

        PlexusConfiguration enabledConfig = new XmlPlexusConfiguration("enabled");
        enabledConfig.setValue("true");
        optimizedHelper.setProperty(bean, "enabled", Boolean.class, enabledConfig);

        PlexusConfiguration descConfig = new XmlPlexusConfiguration("description");
        descConfig.setValue("testValue");
        optimizedHelper.setProperty(bean, "description", String.class, descConfig);

        PlexusConfiguration timeoutConfig = new XmlPlexusConfiguration("timeout");
        timeoutConfig.setValue("123");
        optimizedHelper.setProperty(bean, "timeout", Long.class, timeoutConfig);

        // Repeat to test caching benefits
        nameConfig.setValue("testValue2");
        optimizedHelper.setProperty(bean, "name", String.class, nameConfig);
        countConfig.setValue("456");
        optimizedHelper.setProperty(bean, "count", Integer.class, countConfig);
    }

    /**
     * Benchmark that tests cache benefits by repeatedly setting properties on the same class.
     * This better demonstrates the caching improvements.
     */
    @Benchmark
    public void benchmarkOriginalHelperRepeatedOperations() throws Exception {
        // Test cache benefits by using same class multiple times
        for (int i = 0; i < 10; i++) {
            RealisticTestBean bean = new RealisticTestBean();

            PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
            nameConfig.setValue("testValue" + i);
            originalHelper.setProperty(bean, "name", String.class, nameConfig);

            PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
            countConfig.setValue(String.valueOf(i));
            originalHelper.setProperty(bean, "count", Integer.class, countConfig);
        }
    }

    @Benchmark
    @Test
    public void benchmarkOptimizedHelperRepeatedOperations() throws Exception {
        // Test cache benefits by using same class multiple times
        for (int i = 0; i < 10; i++) {
            RealisticTestBean bean = new RealisticTestBean();

            PlexusConfiguration nameConfig = new XmlPlexusConfiguration("name");
            nameConfig.setValue("testValue" + i);
            optimizedHelper.setProperty(bean, "name", String.class, nameConfig);

            PlexusConfiguration countConfig = new XmlPlexusConfiguration("count");
            countConfig.setValue(String.valueOf(i));
            optimizedHelper.setProperty(bean, "count", Integer.class, countConfig);
        }
    }

    /**
     * Benchmark with multiple different bean types to test method cache effectiveness.
     */
    @Benchmark
    public void benchmarkOriginalHelperMultipleTypes() throws Exception {
        // Test with different bean types
        RealisticTestBean bean1 = new RealisticTestBean();
        TestBean bean2 = new TestBean();

        PlexusConfiguration config1 = new XmlPlexusConfiguration("name");
        config1.setValue("testValue");
        originalHelper.setProperty(bean1, "name", String.class, config1);
        originalHelper.setProperty(bean2, "name", String.class, config1);

        PlexusConfiguration config2 = new XmlPlexusConfiguration("count");
        config2.setValue("123");
        originalHelper.setProperty(bean1, "count", Integer.class, config2);
        originalHelper.setProperty(bean2, "count", Integer.class, config2);
    }

    @Benchmark
    public void benchmarkOptimizedHelperMultipleTypes() throws Exception {
        // Test with different bean types
        RealisticTestBean bean1 = new RealisticTestBean();
        TestBean bean2 = new TestBean();

        PlexusConfiguration config1 = new XmlPlexusConfiguration("name");
        config1.setValue("testValue");
        optimizedHelper.setProperty(bean1, "name", String.class, config1);
        optimizedHelper.setProperty(bean2, "name", String.class, config1);

        PlexusConfiguration config2 = new XmlPlexusConfiguration("count");
        config2.setValue("123");
        optimizedHelper.setProperty(bean1, "count", Integer.class, config2);
        optimizedHelper.setProperty(bean2, "count", Integer.class, config2);
    }

    /**
     * Main method to run the JMH benchmark.
     *
     * @param args command line arguments
     * @throws RunnerException if the benchmark fails to run
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CompositeBeanHelperPerformanceTest.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        new Runner(opt).run();
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
