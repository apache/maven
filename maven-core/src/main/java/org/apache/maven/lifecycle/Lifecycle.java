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
package org.apache.maven.lifecycle;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * Lifecycle definition, with eventual plugin bindings (when they are not packaging-specific).
 */
public class Lifecycle {
    public Lifecycle() {}

    public Lifecycle(String id, List<String> phases, Map<String, LifecyclePhase> defaultPhases) {
        this.id = id;
        this.phases = phases;
        this.defaultPhases = defaultPhases;
    }

    public Lifecycle(
            org.apache.maven.api.services.LifecycleRegistry registry, org.apache.maven.api.Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.id = lifecycle.id();
        this.phases = registry.computePhases(lifecycle);
        this.defaultPhases = getDefaultPhases(lifecycle);
    }

    // <lifecycle>
    //   <id>clean</id>
    //   <phases>
    //     <phase>pre-clean</phase>
    //     <phase>clean</phase>
    //     <phase>post-clean</phase>
    //   </phases>
    //   <default-phases>
    //     <clean>org.apache.maven.plugins:maven-clean-plugin:clean</clean>
    //   </default-phases>
    // </lifecycle>

    private String id;

    private List<String> phases;

    private Map<String, LifecyclePhase> defaultPhases;

    private org.apache.maven.api.Lifecycle lifecycle;

    public String getId() {
        return id;
    }

    public List<String> getPhases() {
        return phases;
    }

    static Map<String, LifecyclePhase> getDefaultPhases(org.apache.maven.api.Lifecycle lifecycle) {
        Map<String, List<String>> goals = new HashMap<>();
        lifecycle.allPhases().forEach(phase -> phase.plugins()
                .forEach(plugin -> plugin.getExecutions().forEach(exec -> exec.getGoals()
                        .forEach(goal -> goals.computeIfAbsent(phase.name(), n -> new ArrayList<>())
                                .add(plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion()
                                        + ":" + goal)))));
        return goals.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new LifecyclePhase(String.join(",", e.getValue()))));
    }

    public Map<String, LifecyclePhase> getDefaultLifecyclePhases() {
        return defaultPhases;
    }

    @Deprecated
    public Map<String, String> getDefaultPhases() {
        return LifecyclePhase.toLegacyMap(getDefaultLifecyclePhases());
    }

    @Override
    public String toString() {
        return id + " -> " + phases;
    }
}
