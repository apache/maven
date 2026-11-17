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
package org.apache.maven.lifecycle.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.internal.stub.LoggerStub;
import org.apache.maven.lifecycle.internal.stub.RankedEntriesMap;
import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultLifecyclePluginAnalyzerTest {

    @Test
    void highestRankedMappingWinsForDuplicatePackaging() throws Exception {
        // a build extension registers its own "jar" mapping, which ranks above the one in core
        RankedEntriesMap<LifecycleMapping> mappings = new RankedEntriesMap<LifecycleMapping>()
                .add("jar", mapping("org.example:extension-plugin:1.0:prepare"))
                .add("jar", mapping("org.example:core-plugin:1.0:prepare"));

        Set<Plugin> plugins = analyzer(mappings).getPluginsBoundByDefaultToAllLifecycles("jar");

        assertEquals(
                Collections.singletonList("org.example:extension-plugin"),
                plugins.stream().map(Plugin::getKey).collect(Collectors.toList()));
    }

    private static LifecycleMapping mapping(String goal) {
        org.apache.maven.lifecycle.mapping.Lifecycle lifecycle = new org.apache.maven.lifecycle.mapping.Lifecycle();
        lifecycle.setId("default");
        lifecycle.setLifecyclePhases(Collections.singletonMap("initialize", new LifecyclePhase(goal)));
        return new DefaultLifecycleMapping(Collections.singletonList(lifecycle));
    }

    private static DefaultLifecyclePluginAnalyzer analyzer(Map<String, LifecycleMapping> mappings) throws Exception {
        Map<String, Lifecycle> lifecycles = new LinkedHashMap<>();
        lifecycles.put("default", new Lifecycle("default", Arrays.asList("initialize"), null));
        lifecycles.put("clean", new Lifecycle("clean", Arrays.asList("clean"), null));
        lifecycles.put("site", new Lifecycle("site", Arrays.asList("site"), null));

        DefaultLifecyclePluginAnalyzer analyzer = new DefaultLifecyclePluginAnalyzer();
        set(analyzer, "lifecycleMappings", mappings);
        set(analyzer, "defaultLifeCycles", new DefaultLifecycles(lifecycles, new LoggerStub()));
        return analyzer;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = DefaultLifecyclePluginAnalyzer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
