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
package org.apache.maven.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * MojoExecution
 */
public class MojoExecution {

    private Plugin plugin;

    private String goal;

    private String executionId;

    private MojoDescriptor mojoDescriptor;

    private Xpp3Dom configuration;

    /**
     * Describes the source of an execution.
     */
    public enum Source {

        /**
         * An execution that originates from the direct invocation of a goal from the CLI.
         */
        CLI,

        /**
         * An execution that originates from a goal bound to a lifecycle phase.
         */
        LIFECYCLE,
    }

    private Source source = Source.LIFECYCLE;

    /**
     * The phase may or may not have been bound to a phase but once the plan has been calculated we know what phase
     * this mojo execution is going to run in.
     */
    private String lifecyclePhase;

    /**
     * The executions to fork before this execution, indexed by the groupId:artifactId:version of the project on which
     * the forked execution are to be run and in reactor build order.
     */
    private Map<String, List<MojoExecution>> forkedExecutions = new LinkedHashMap<>();

    public MojoExecution(Plugin plugin, String goal, String executionId) {
        this.plugin = plugin;
        this.goal = goal;
        this.executionId = executionId;
    }

    public MojoExecution(MojoDescriptor mojoDescriptor) {
        this.mojoDescriptor = mojoDescriptor;
        this.executionId = null;
        this.configuration = null;
    }

    public MojoExecution(MojoDescriptor mojoDescriptor, String executionId, Source source) {
        this.mojoDescriptor = mojoDescriptor;
        this.executionId = executionId;
        this.configuration = null;
        this.source = source;
    }

    public MojoExecution(MojoDescriptor mojoDescriptor, String executionId) {
        this.mojoDescriptor = mojoDescriptor;
        this.executionId = executionId;
        this.configuration = null;
    }

    public MojoExecution(MojoDescriptor mojoDescriptor, Xpp3Dom configuration) {
        this.mojoDescriptor = mojoDescriptor;
        this.configuration = configuration;
        this.executionId = null;
    }

    /**
     * Gets the source of this execution.
     *
     * @return The source of this execution or {@code null} if unknown.
     */
    public Source getSource() {
        return source;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Plugin getPlugin() {
        if (mojoDescriptor != null) {
            return mojoDescriptor.getPluginDescriptor().getPlugin();
        }

        return plugin;
    }

    public MojoDescriptor getMojoDescriptor() {
        return mojoDescriptor;
    }

    public Xpp3Dom getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Xpp3Dom configuration) {
        this.configuration = configuration;
    }

    public String identify() {
        StringBuilder sb = new StringBuilder(256);

        sb.append(executionId);
        sb.append(configuration.toString());

        return sb.toString();
    }

    public String getLifecyclePhase() {
        return lifecyclePhase;
    }

    public void setLifecyclePhase(String lifecyclePhase) {
        this.lifecyclePhase = lifecyclePhase;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        if (mojoDescriptor != null) {
            buffer.append(mojoDescriptor.getId());
        }
        buffer.append(" {execution: ").append(executionId).append('}');
        return buffer.toString();
    }

    public String getGroupId() {
        if (mojoDescriptor != null) {
            return mojoDescriptor.getPluginDescriptor().getGroupId();
        }

        return plugin.getGroupId();
    }

    public String getArtifactId() {
        if (mojoDescriptor != null) {
            return mojoDescriptor.getPluginDescriptor().getArtifactId();
        }

        return plugin.getArtifactId();
    }

    public String getVersion() {
        if (mojoDescriptor != null) {
            return mojoDescriptor.getPluginDescriptor().getVersion();
        }

        return plugin.getVersion();
    }

    public String getGoal() {
        if (mojoDescriptor != null) {
            return mojoDescriptor.getGoal();
        }

        return goal;
    }

    public void setMojoDescriptor(MojoDescriptor mojoDescriptor) {
        this.mojoDescriptor = mojoDescriptor;
    }

    public Map<String, List<MojoExecution>> getForkedExecutions() {
        return forkedExecutions;
    }

    public void setForkedExecutions(String projectKey, List<MojoExecution> forkedExecutions) {
        this.forkedExecutions.put(projectKey, forkedExecutions);
    }
}
