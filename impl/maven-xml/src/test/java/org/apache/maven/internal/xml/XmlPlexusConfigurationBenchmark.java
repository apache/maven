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
 * JMH benchmarks comparing the performance of the old vs new XmlPlexusConfiguration implementations.
 *
 * To run these benchmarks:
 * mvn test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main"
 *     -Dexec.classpathScope=test
 *     -Dexec.args="org.apache.maven.internal.xml.XmlPlexusConfigurationBenchmark"
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class XmlPlexusConfigurationBenchmark {

    private XmlNode simpleNode;
    private XmlNode complexNode;
    private XmlNode deepNode;

    @Setup
    public void setup() {
        // Create test XML nodes of varying complexity
        simpleNode = createSimpleNode();
        complexNode = createComplexNode();
        deepNode = createDeepNode();
    }

    /**
     * Benchmark constructor performance - Simple XML
     */
    @Benchmark
    public PlexusConfiguration constructorOldSimple() {
        return new XmlPlexusConfigurationOld(simpleNode);
    }

    @Benchmark
    public PlexusConfiguration constructorNewSimple() {
        return new XmlPlexusConfiguration(simpleNode);
    }

    /**
     * Benchmark constructor performance - Complex XML
     */
    @Benchmark
    public PlexusConfiguration constructorOldComplex() {
        return new XmlPlexusConfigurationOld(complexNode);
    }

    @Benchmark
    public PlexusConfiguration constructorNewComplex() {
        return new XmlPlexusConfiguration(complexNode);
    }

    /**
     * Benchmark constructor performance - Deep XML
     */
    @Benchmark
    public PlexusConfiguration constructorOldDeep() {
        return new XmlPlexusConfigurationOld(deepNode);
    }

    @Benchmark
    public PlexusConfiguration constructorNewDeep() {
        return new XmlPlexusConfiguration(deepNode);
    }

    /**
     * Benchmark child access performance - Lazy vs Eager
     */
    @Benchmark
    public void childAccessOldComplex(Blackhole bh) {
        PlexusConfiguration config = new XmlPlexusConfigurationOld(complexNode);
        // Access all children to measure eager loading performance
        for (int i = 0; i < config.getChildCount(); i++) {
            bh.consume(config.getChild(i));
        }
    }

    @Benchmark
    public void childAccessNewComplex(Blackhole bh) {
        PlexusConfiguration config = new XmlPlexusConfiguration(complexNode);
        // Access all children to measure lazy loading performance
        for (int i = 0; i < config.getChildCount(); i++) {
            bh.consume(config.getChild(i));
        }
    }

    /**
     * Benchmark memory allocation patterns
     */
    @Benchmark
    public PlexusConfiguration memoryAllocationOld() {
        // This will trigger deep copying and high memory allocation
        return new XmlPlexusConfigurationOld(deepNode);
    }

    @Benchmark
    public PlexusConfiguration memoryAllocationNew() {
        // This should have much lower memory allocation due to sharing
        return new XmlPlexusConfiguration(deepNode);
    }

    // Helper methods to create test XML nodes
    private XmlNode createSimpleNode() {
        Map<String, String> attrs = Map.of("attr1", "value1");
        return XmlNode.newBuilder()
                .name("simple")
                .value("test-value")
                .attributes(attrs)
                .build();
    }

    private XmlNode createComplexNode() {
        Map<String, String> attrs = Map.of("id", "test", "version", "1.0");
        List<XmlNode> children = List.of(
                XmlNode.newInstance("child1", "value1"),
                XmlNode.newInstance("child2", "value2"),
                XmlNode.newBuilder()
                        .name("child3")
                        .children(List.of(
                                XmlNode.newInstance("nested1", "nested-value1"),
                                XmlNode.newInstance("nested2", "nested-value2")))
                        .build(),
                XmlNode.newInstance("child4", "value4"),
                XmlNode.newInstance("child5", "value5"));

        return XmlNode.newBuilder()
                .name("complex")
                .attributes(attrs)
                .children(children)
                .build();
    }

    private XmlNode createDeepNode() {
        List<XmlNode> levels = new ArrayList<>();

        // Create a deep hierarchy to stress test performance
        for (int i = 0; i < 10; i++) {
            List<XmlNode> items = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Map<String, String> itemAttrs = Map.of("index", String.valueOf(j));
                items.add(XmlNode.newBuilder()
                        .name("item" + j)
                        .value("value-" + i + "-" + j)
                        .attributes(itemAttrs)
                        .build());
            }
            levels.add(XmlNode.newBuilder().name("level" + i).children(items).build());
        }

        return XmlNode.newBuilder().name("root").children(levels).build();
    }
}
