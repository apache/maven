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
package org.apache.maven.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * ReactorManager - unused
 */
@Deprecated
public class ReactorManager {
    public static final String FAIL_FAST = "fail-fast";

    public static final String FAIL_AT_END = "fail-at-end";

    public static final String FAIL_NEVER = "fail-never";

    public static final String MAKE_MODE = "make";

    public static final String MAKE_DEPENDENTS_MODE = "make-dependents";

    // make projects that depend on me, and projects that I depend on
    public static final String MAKE_BOTH_MODE = "make-both";

    private List<String> blackList = new ArrayList<>();

    private Map<String, BuildFailure> buildFailuresByProject = new HashMap<>();

    private Map<String, Map<String, Map>> pluginContextsByProjectAndPluginKey = new HashMap<>();

    private String failureBehavior = FAIL_FAST;

    private final ProjectSorter sorter;

    private Map<String, BuildSuccess> buildSuccessesByProject = new HashMap<>();

    public ReactorManager(List<MavenProject> projects) throws CycleDetectedException, DuplicateProjectException {
        this.sorter = new ProjectSorter(projects);
    }

    public Map getPluginContext(PluginDescriptor plugin, MavenProject project) {
        Map<String, Map> pluginContextsByKey = pluginContextsByProjectAndPluginKey.get(project.getId());

        if (pluginContextsByKey == null) {
            pluginContextsByKey = new HashMap<>();
            pluginContextsByProjectAndPluginKey.put(project.getId(), pluginContextsByKey);
        }

        Map pluginContext = pluginContextsByKey.get(plugin.getPluginLookupKey());

        if (pluginContext == null) {
            pluginContext = new HashMap<>();
            pluginContextsByKey.put(plugin.getPluginLookupKey(), pluginContext);
        }

        return pluginContext;
    }

    public void setFailureBehavior(String failureBehavior) {
        if (failureBehavior == null) {
            this.failureBehavior = FAIL_FAST; // default
            return;
        }
        if (FAIL_FAST.equals(failureBehavior)
                || FAIL_AT_END.equals(failureBehavior)
                || FAIL_NEVER.equals(failureBehavior)) {
            this.failureBehavior = failureBehavior;
        } else {
            throw new IllegalArgumentException("Invalid failure behavior (must be one of: \'" + FAIL_FAST + "\', \'"
                    + FAIL_AT_END + "\', \'" + FAIL_NEVER + "\').");
        }
    }

    public String getFailureBehavior() {
        return failureBehavior;
    }

    public void blackList(MavenProject project) {
        blackList(getProjectKey(project));
    }

    private void blackList(String id) {
        if (!blackList.contains(id)) {
            blackList.add(id);

            List<String> dependents = sorter.getDependents(id);

            if (dependents != null && !dependents.isEmpty()) {
                for (String dependentId : dependents) {
                    if (!buildSuccessesByProject.containsKey(dependentId)
                            && !buildFailuresByProject.containsKey(dependentId)) {
                        blackList(dependentId);
                    }
                }
            }
        }
    }

    public boolean isBlackListed(MavenProject project) {
        return blackList.contains(getProjectKey(project));
    }

    private static String getProjectKey(MavenProject project) {
        return ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());
    }

    public void registerBuildFailure(MavenProject project, Exception error, String task, long time) {
        buildFailuresByProject.put(getProjectKey(project), new BuildFailure(project, time, error));
    }

    public boolean hasBuildFailures() {
        return !buildFailuresByProject.isEmpty();
    }

    public boolean hasBuildFailure(MavenProject project) {
        return buildFailuresByProject.containsKey(getProjectKey(project));
    }

    public boolean hasMultipleProjects() {
        return sorter.hasMultipleProjects();
    }

    public List<MavenProject> getSortedProjects() {
        return sorter.getSortedProjects();
    }

    public boolean hasBuildSuccess(MavenProject project) {
        return buildSuccessesByProject.containsKey(getProjectKey(project));
    }

    public void registerBuildSuccess(MavenProject project, long time) {
        buildSuccessesByProject.put(getProjectKey(project), new BuildSuccess(project, time));
    }

    public BuildFailure getBuildFailure(MavenProject project) {
        return buildFailuresByProject.get(getProjectKey(project));
    }

    public BuildSuccess getBuildSuccess(MavenProject project) {
        return buildSuccessesByProject.get(getProjectKey(project));
    }

    public boolean executedMultipleProjects() {
        return buildFailuresByProject.size() + buildSuccessesByProject.size() > 1;
    }
}
