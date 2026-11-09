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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.lifecycle.internal.concurrent.BuildPlan;

final class DefaultLifecyclePlan implements LifecyclePlan {
    private final Project project;
    private final List<String> tasks;
    private final List<MojoExecution> executions;
    private final BuildPlan buildPlan;
    private final Map<org.apache.maven.plugin.MojoExecution, MojoExecution> internalToApiMap;

    DefaultLifecyclePlan(
            Project project,
            List<String> tasks,
            List<MojoExecution> executions,
            BuildPlan buildPlan,
            Map<org.apache.maven.plugin.MojoExecution, MojoExecution> internalToApiMap) {
        this.project = project;
        this.tasks = List.copyOf(tasks);
        this.executions = List.copyOf(executions);
        this.buildPlan = buildPlan;
        this.internalToApiMap = Map.copyOf(internalToApiMap);
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public List<String> tasks() {
        return tasks;
    }

    @Override
    public List<MojoExecution> executions() {
        return executions;
    }

    BuildPlan buildPlan() {
        return buildPlan;
    }

    Map<org.apache.maven.plugin.MojoExecution, MojoExecution> internalToApiMap() {
        return internalToApiMap;
    }

    static Map<org.apache.maven.plugin.MojoExecution, MojoExecution> buildMapping(
            List<org.apache.maven.plugin.MojoExecution> internalExecutions, List<MojoExecution> apiExecutions) {
        Map<org.apache.maven.plugin.MojoExecution, MojoExecution> map = new IdentityHashMap<>();
        for (int i = 0; i < internalExecutions.size(); i++) {
            map.put(internalExecutions.get(i), apiExecutions.get(i));
        }
        return map;
    }
}
