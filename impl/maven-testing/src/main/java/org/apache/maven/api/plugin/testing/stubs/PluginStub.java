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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;

public class PluginStub implements Plugin {

    org.apache.maven.api.model.Plugin model;
    PluginDescriptor descriptor;
    List<Lifecycle> lifecycles = Collections.emptyList();
    ClassLoader classLoader;
    Artifact artifact;
    List<Dependency> dependencies = Collections.emptyList();
    Map<String, Dependency> dependenciesMap = Collections.emptyMap();

    @Override
    public org.apache.maven.api.model.Plugin getModel() {
        return model;
    }

    public void setModel(org.apache.maven.api.model.Plugin model) {
        this.model = model;
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public List<Lifecycle> getLifecycles() {
        return lifecycles;
    }

    public void setLifecycles(List<Lifecycle> lifecycles) {
        this.lifecycles = lifecycles;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, Dependency> getDependenciesMap() {
        return dependenciesMap;
    }

    public void setDependenciesMap(Map<String, Dependency> dependenciesMap) {
        this.dependenciesMap = dependenciesMap;
    }
}
