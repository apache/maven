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
package org.apache.maven.internal.impl;

import java.util.Collections;
import java.util.List;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;

public class Lifecycles {

    static Lifecycle.Phase phase(String name) {
        return new DefaultPhase(name, Collections.emptyList(), Collections.emptyList());
    }

    static Lifecycle.Phase phase(String name, Plugin plugin) {
        return new DefaultPhase(name, Collections.singletonList(plugin), Collections.emptyList());
    }

    static Plugin plugin(String coord, String phase) {
        String[] c = coord.split(":");
        return Plugin.newBuilder()
                .groupId(c[0])
                .artifactId(c[1])
                .version(c[2])
                .executions(Collections.singletonList(PluginExecution.newBuilder()
                        .id("default-" + c[3])
                        .phase(phase)
                        .goals(Collections.singletonList(c[3]))
                        .build()))
                .build();
    }

    static class DefaultPhase implements Lifecycle.Phase {
        private final String name;
        private final List<Plugin> plugins;
        private final List<Lifecycle.Phase> phases;

        DefaultPhase(String name, List<Plugin> plugins, List<Lifecycle.Phase> phases) {
            this.name = name;
            this.plugins = plugins;
            this.phases = phases;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Plugin> plugins() {
            return plugins;
        }
    }
}
