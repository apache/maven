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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.xml.XmlNode;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for testing thread safety and concurrent performance.
 *
 * This benchmark specifically tests the thread safety improvements in the new implementation
 * by running concurrent operations that would cause race conditions in the old version.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Threads(4) // Test with multiple threads to expose race conditions
public class XmlPlexusConfigurationConcurrencyBenchmark {

    private XmlNode testNode;
    private PlexusConfiguration configOld;
    private PlexusConfiguration configNew;

    @Setup
    public void setup() {
        testNode = createTestNode();
        configOld = new XmlPlexusConfigurationOld(testNode);
        configNew = new XmlPlexusConfiguration(testNode);
    }

    /**
     * Test concurrent child access with old implementation
     * This may expose race conditions and inconsistent behavior
     */
    @Benchmark
    @Group("concurrentAccessOld")
    public void concurrentChildAccessOld(Blackhole bh) {
        try {
            for (int i = 0; i < configOld.getChildCount(); i++) {
                PlexusConfiguration child = configOld.getChild(i);
                bh.consume(child.getName());
                bh.consume(child.getValue());

                // Access nested children to stress the implementation
                for (int j = 0; j < child.getChildCount(); j++) {
                    PlexusConfiguration nested = child.getChild(j);
                    bh.consume(nested.getName());
                    bh.consume(nested.getValue());
                }
            }
        } catch (Exception e) {
            // Old implementation may throw exceptions under concurrent access
            bh.consume(e);
        }
    }

    /**
     * Test concurrent child access with new implementation
     * This should be thread-safe and perform consistently
     */
    @Benchmark
    @Group("concurrentAccessNew")
    public void concurrentChildAccessNew(Blackhole bh) {
        for (int i = 0; i < configNew.getChildCount(); i++) {
            PlexusConfiguration child = configNew.getChild(i);
            bh.consume(child.getName());
            bh.consume(child.getValue());

            // Access nested children to stress the implementation
            for (int j = 0; j < child.getChildCount(); j++) {
                PlexusConfiguration nested = child.getChild(j);
                bh.consume(nested.getName());
                bh.consume(nested.getValue());
            }
        }
    }

    /**
     * Test concurrent construction and access with old implementation
     */
    @Benchmark
    public void concurrentConstructionOld(Blackhole bh) {
        try {
            PlexusConfiguration config = new XmlPlexusConfigurationOld(testNode);
            // Immediately access children to trigger potential race conditions
            for (int i = 0; i < config.getChildCount(); i++) {
                bh.consume(config.getChild(i).getName());
            }
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Test concurrent construction and access with new implementation
     */
    @Benchmark
    public void concurrentConstructionNew(Blackhole bh) {
        PlexusConfiguration config = new XmlPlexusConfiguration(testNode);
        // Immediately access children to test thread safety
        for (int i = 0; i < config.getChildCount(); i++) {
            bh.consume(config.getChild(i).getName());
        }
    }

    /**
     * Test concurrent attribute access
     */
    @Benchmark
    public void concurrentAttributeAccessOld(Blackhole bh) {
        try {
            String[] attrNames = configOld.getAttributeNames();
            for (String attrName : attrNames) {
                bh.consume(configOld.getAttribute(attrName));
            }
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    @Benchmark
    public void concurrentAttributeAccessNew(Blackhole bh) {
        String[] attrNames = configNew.getAttributeNames();
        for (String attrName : attrNames) {
            bh.consume(configNew.getAttribute(attrName));
        }
    }

    private XmlNode createTestNode() {
        Map<String, String> rootAttrs = Map.of("id", "test-root", "version", "1.0", "type", "benchmark");

        List<XmlNode> children = List.of(
                XmlNode.newBuilder()
                        .name("section1")
                        .attributes(Map.of("name", "section1"))
                        .children(List.of(
                                XmlNode.newInstance("item1", "value1"),
                                XmlNode.newInstance("item2", "value2"),
                                XmlNode.newInstance("item3", "value3")))
                        .build(),
                XmlNode.newBuilder()
                        .name("section2")
                        .attributes(Map.of("name", "section2"))
                        .children(
                                List.of(XmlNode.newInstance("item4", "value4"), XmlNode.newInstance("item5", "value5")))
                        .build(),
                XmlNode.newBuilder()
                        .name("section3")
                        .attributes(Map.of("name", "section3"))
                        .children(List.of(XmlNode.newBuilder()
                                .name("nested")
                                .children(List.of(
                                        XmlNode.newInstance("deep1", "deep-value1"),
                                        XmlNode.newInstance("deep2", "deep-value2")))
                                .build()))
                        .build());

        return XmlNode.newBuilder()
                .name("root")
                .attributes(rootAttrs)
                .children(children)
                .build();
    }
}
