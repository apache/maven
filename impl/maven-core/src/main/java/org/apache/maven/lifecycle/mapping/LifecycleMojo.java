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

import java.util.List;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.model.Dependency;

/**
 * Mojo (plugin goal) binding to a lifecycle phase.
 *
 * @see LifecyclePhase
 */
public class LifecycleMojo {

    private String goal;
    private XmlNode configuration;
    private List<Dependency> dependencies;

    public String getGoal() {
        return goal;
    }

    public XmlNode getConfiguration() {
        return configuration;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public void setConfiguration(XmlNode configuration) {
        this.configuration = configuration;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
