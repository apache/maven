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
package org.apache.maven.api.plugin.testing.stubs;

import java.util.Optional;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.xml.XmlNode;

/**
 * Stub for {@link MojoExecution}.
 */
public class MojoExecutionStub implements MojoExecution {
    private String executionId;
    private String goal;
    private XmlNode dom;
    private Plugin plugin = new PluginStub();
    private PluginExecution model;
    private MojoDescriptor descriptor;
    private String lifecyclePhase;

    public MojoExecutionStub(String executionId, String goal) {
        this(executionId, goal, null);
    }

    public MojoExecutionStub(String executionId, String goal, XmlNode dom) {
        this.executionId = executionId;
        this.goal = goal;
        this.dom = dom;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public PluginExecution getModel() {
        return model;
    }

    @Override
    public MojoDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getLifecyclePhase() {
        return lifecyclePhase;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getGoal() {
        return goal;
    }

    @Override
    public Optional<XmlNode> getConfiguration() {
        return Optional.ofNullable(dom);
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public void setDom(XmlNode dom) {
        this.dom = dom;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setModel(PluginExecution model) {
        this.model = model;
    }

    public void setDescriptor(MojoDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void setLifecyclePhase(String lifecyclePhase) {
        this.lifecyclePhase = lifecyclePhase;
    }
}
