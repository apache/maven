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

import java.nio.file.Path;

import org.apache.maven.api.ProducedArtifact;

/** @deprecated Use {@link org.apache.maven.testing.plugin.stubs.ProjectStub} instead */
@Deprecated(since = "4.0.0-rc-6", forRemoval = true)
public class ProjectStub extends org.apache.maven.testing.plugin.stubs.ProjectStub {
    // The setters are overridden with a covariant return type so that chained calls
    // keep returning this deprecated type, as they did before the relocation.

    @Override
    public ProjectStub setBasedir(Path basedir) {
        super.setBasedir(basedir);
        return this;
    }

    @Override
    public ProjectStub setGroupId(String groupId) {
        super.setGroupId(groupId);
        return this;
    }

    @Override
    public ProjectStub setArtifactId(String artifactId) {
        super.setArtifactId(artifactId);
        return this;
    }

    @Override
    public ProjectStub setVersion(String version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public ProjectStub setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public ProjectStub setDescription(String desc) {
        super.setDescription(desc);
        return this;
    }

    @Override
    public ProjectStub setPackaging(String packaging) {
        super.setPackaging(packaging);
        return this;
    }

    @Override
    public ProjectStub setMainArtifact(ProducedArtifact mainArtifact) {
        super.setMainArtifact(mainArtifact);
        return this;
    }

    @Override
    public ProjectStub setPomPath(Path pomPath) {
        super.setPomPath(pomPath);
        return this;
    }

    @Override
    public ProjectStub setTopProject(boolean topProject) {
        super.setTopProject(topProject);
        return this;
    }

    @Override
    public ProjectStub setMavenModel(org.apache.maven.model.Model model) {
        super.setMavenModel(model);
        return this;
    }

    @Override
    public ProjectStub setRootDirectory(Path rootDirectory) {
        super.setRootDirectory(rootDirectory);
        return this;
    }

    @Override
    public ProjectStub addProperty(String key, String value) {
        super.addProperty(key, value);
        return this;
    }
}
