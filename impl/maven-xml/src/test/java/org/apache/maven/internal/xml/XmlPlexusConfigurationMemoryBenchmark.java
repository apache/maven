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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for measuring memory allocation patterns and garbage collection impact.
 *
 * This benchmark measures the memory efficiency improvements in the new implementation
 * by creating many configuration objects and measuring allocation rates.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {"-XX:+UseG1GC", "-Xmx2g", "-Xms2g"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class XmlPlexusConfigurationMemoryBenchmark {

    private XmlNode smallNode;
    private XmlNode mediumNode;
    private XmlNode largeNode;

    @Setup
    public void setup() {
        smallNode = createSmallNode();
        mediumNode = createMediumNode();
        largeNode = createLargeNode();
    }

    /**
     * Benchmark memory allocation for small XML documents
     */
    @Benchmark
    public List<PlexusConfiguration> memoryAllocationOldSmall() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        // Create multiple configurations to measure allocation patterns
        for (int i = 0; i < 100; i++) {
            configs.add(new XmlPlexusConfigurationOld(smallNode));
        }
        return configs;
    }

    @Benchmark
    public List<PlexusConfiguration> memoryAllocationNewSmall() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        // Create multiple configurations to measure allocation patterns
        for (int i = 0; i < 100; i++) {
            configs.add(new XmlPlexusConfiguration(smallNode));
        }
        return configs;
    }

    /**
     * Benchmark memory allocation for medium XML documents
     */
    @Benchmark
    public List<PlexusConfiguration> memoryAllocationOldMedium() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            configs.add(new XmlPlexusConfigurationOld(mediumNode));
        }
        return configs;
    }

    @Benchmark
    public List<PlexusConfiguration> memoryAllocationNewMedium() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            configs.add(new XmlPlexusConfiguration(mediumNode));
        }
        return configs;
    }

    /**
     * Benchmark memory allocation for large XML documents
     */
    @Benchmark
    public List<PlexusConfiguration> memoryAllocationOldLarge() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            configs.add(new XmlPlexusConfigurationOld(largeNode));
        }
        return configs;
    }

    @Benchmark
    public List<PlexusConfiguration> memoryAllocationNewLarge() {
        List<PlexusConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            configs.add(new XmlPlexusConfiguration(largeNode));
        }
        return configs;
    }

    /**
     * Benchmark lazy vs eager child creation impact on memory
     */
    @Benchmark
    public void lazyVsEagerOld(Blackhole bh) {
        PlexusConfiguration config = new XmlPlexusConfigurationOld(largeNode);
        // All children are already created (eager), just access them
        for (int i = 0; i < config.getChildCount(); i++) {
            bh.consume(config.getChild(i));
        }
    }

    @Benchmark
    public void lazyVsEagerNew(Blackhole bh) {
        PlexusConfiguration config = new XmlPlexusConfiguration(largeNode);
        // Children are created on-demand (lazy), measure the impact
        for (int i = 0; i < config.getChildCount(); i++) {
            bh.consume(config.getChild(i));
        }
    }

    /**
     * Test memory sharing vs copying
     */
    @Benchmark
    public PlexusConfiguration memorySharingOld() {
        // This creates deep copies of all data
        return new XmlPlexusConfigurationOld(largeNode);
    }

    @Benchmark
    public PlexusConfiguration memorySharingNew() {
        // This shares the underlying XML structure
        return new XmlPlexusConfiguration(largeNode);
    }

    // Helper methods to create test nodes of different sizes
    private XmlNode createSmallNode() {
        Map<String, String> attrs = Map.of("id", "small-test");
        List<XmlNode> children =
                List.of(XmlNode.newInstance("child1", "value1"), XmlNode.newInstance("child2", "value2"));

        return XmlNode.newBuilder()
                .name("small")
                .attributes(attrs)
                .children(children)
                .build();
    }

    private XmlNode createMediumNode() {
        Map<String, String> attrs = Map.of("id", "medium-test", "version", "1.0");
        List<XmlNode> children = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Map<String, String> itemAttrs = Map.of("index", String.valueOf(i));
            List<XmlNode> itemChildren = List.of(XmlNode.newInstance("nested" + i, "nested-value-" + i));

            children.add(XmlNode.newBuilder()
                    .name("item" + i)
                    .value("value-" + i)
                    .attributes(itemAttrs)
                    .children(itemChildren)
                    .build());
        }

        return XmlNode.newBuilder()
                .name("medium")
                .attributes(attrs)
                .children(children)
                .build();
    }

    private XmlNode createLargeNode() {
        Map<String, String> attrs = Map.of("id", "large-test", "version", "2.0", "type", "benchmark");
        List<XmlNode> sections = new ArrayList<>();

        // Create a large, complex structure
        for (int section = 0; section < 10; section++) {
            Map<String, String> sectionAttrs = Map.of("name", "section-" + section);
            List<XmlNode> items = new ArrayList<>();

            for (int item = 0; item < 20; item++) {
                Map<String, String> itemAttrs = Map.of("id", "item-" + section + "-" + item);
                List<XmlNode> nestedElements = new ArrayList<>();

                // Add nested elements
                for (int nested = 0; nested < 5; nested++) {
                    Map<String, String> nestedAttrs = Map.of("level", String.valueOf(nested));
                    nestedElements.add(XmlNode.newBuilder()
                            .name("nested" + nested)
                            .value("nested-value-" + section + "-" + item + "-" + nested)
                            .attributes(nestedAttrs)
                            .build());
                }

                items.add(XmlNode.newBuilder()
                        .name("item" + item)
                        .value("section-" + section + "-item-" + item)
                        .attributes(itemAttrs)
                        .children(nestedElements)
                        .build());
            }

            sections.add(XmlNode.newBuilder()
                    .name("section" + section)
                    .attributes(sectionAttrs)
                    .children(items)
                    .build());
        }

        return XmlNode.newBuilder()
                .name("large")
                .attributes(attrs)
                .children(sections)
                .build();
    }
}
