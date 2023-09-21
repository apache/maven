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
package org.apache.maven.lifecycle.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mojo (goals) bindings to a lifecycle phase.
 *
 * @see LifecycleMojo
 */
public class LifecyclePhase {

    private List<LifecycleMojo> mojos;

    public LifecyclePhase() {}

    public LifecyclePhase(String goals) {
        set(goals);
    }

    public List<LifecycleMojo> getMojos() {
        return mojos;
    }

    public void setMojos(List<LifecycleMojo> mojos) {
        this.mojos = mojos;
    }

    public void set(String goals) {
        mojos = new ArrayList<>();

        if (goals != null && !goals.isEmpty()) {
            String[] mojoGoals = goals.split(",");
            mojos = Arrays.stream(mojoGoals).map(fromGoalIntoLifecycleMojo).collect(Collectors.toList());
        }
    }

    private final Function<String, LifecycleMojo> fromGoalIntoLifecycleMojo = s -> {
        LifecycleMojo lifecycleMojo = new LifecycleMojo();
        lifecycleMojo.setGoal(s.trim());
        return lifecycleMojo;
    };

    @Override
    public String toString() {
        return Optional.ofNullable(getMojos()).orElse(Collections.emptyList()).stream()
                .map(LifecycleMojo::getGoal)
                .collect(Collectors.joining(","));
    }

    @Deprecated
    public static Map<String, String> toLegacyMap(Map<String, LifecyclePhase> lifecyclePhases) {
        if (lifecyclePhases == null) {
            return null;
        }

        if (lifecyclePhases.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> phases = new LinkedHashMap<>();
        for (Map.Entry<String, LifecyclePhase> e : lifecyclePhases.entrySet()) {
            phases.put(e.getKey(), e.getValue().toString());
        }
        return phases;
    }
}
