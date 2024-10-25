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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

public class BuildStep {
    public static final int CREATED = 0;
    public static final int PLANNING = 1;
    public static final int SCHEDULED = 2;
    public static final int EXECUTED = 3;
    public static final int FAILED = 4;

    public static final String PLAN = "$plan$";
    public static final String SETUP = "$setup$";
    public static final String TEARDOWN = "$teardown$";

    final MavenProject project;
    final String name;
    final Lifecycle.Phase phase;
    final Map<Integer, Map<String, MojoExecution>> mojos = new TreeMap<>();
    final Collection<BuildStep> predecessors = new HashSet<>();
    final Collection<BuildStep> successors = new HashSet<>();
    final AtomicInteger status = new AtomicInteger();
    final AtomicBoolean skip = new AtomicBoolean();

    public BuildStep(String name, MavenProject project, Lifecycle.Phase phase) {
        this.name = name;
        this.project = project;
        this.phase = phase;
    }

    public Stream<BuildStep> allPredecessors() {
        return preds(new HashSet<>()).stream();
    }

    private Set<BuildStep> preds(Set<BuildStep> preds) {
        if (preds.add(this)) {
            this.predecessors.forEach(n -> n.preds(preds));
        }
        return preds;
    }

    public boolean isSuccessorOf(BuildStep step) {
        return isSuccessorOf(new HashSet<>(), step);
    }

    private boolean isSuccessorOf(Set<BuildStep> visited, BuildStep step) {
        if (this == step) {
            return true;
        } else if (visited.add(this)) {
            return this.predecessors.stream().anyMatch(n -> n.isSuccessorOf(visited, step));
        } else {
            return false;
        }
    }

    public void skip() {
        skip.set(true);
        mojos.clear();
    }

    public void addMojo(MojoExecution mojo, int priority) {
        if (!skip.get()) {
            mojos.computeIfAbsent(priority, k -> new LinkedHashMap<>())
                    .put(mojo.getGoal() + ":" + mojo.getExecutionId(), mojo);
        }
    }

    public void executeAfter(BuildStep stepToExecuteBefore) {
        if (!isSuccessorOf(stepToExecuteBefore)) {
            predecessors.add(stepToExecuteBefore);
            stepToExecuteBefore.successors.add(this);
        }
    }

    public Stream<MojoExecution> executions() {
        return mojos.values().stream().flatMap(m -> m.values().stream());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildStep that = (BuildStep) o;
        return Objects.equals(project, that.project) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, name);
    }

    @Override
    public String toString() {
        return "BuildStep[" + "project="
                + project.getGroupId() + ":" + project.getArtifactId() + ", phase="
                + name + ']';
    }
}
