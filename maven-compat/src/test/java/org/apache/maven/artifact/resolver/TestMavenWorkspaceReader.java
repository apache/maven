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
package org.apache.maven.artifact.resolver;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceRepository;

public class TestMavenWorkspaceReader implements MavenWorkspaceReader {

    static final String REPO_LAYOUT = "test";

    static final String REPO_URL = "https://test/me";

    static final String REPO_ID = "custom";

    static final String GROUP_ID = "org.apache.maven";

    static final String ARTIFACT_ID = "this.is.a.test";

    static final String VERSION = "99.99";

    private static final WorkspaceRepository WORKSPACE_REPOSITORY = new WorkspaceRepository(REPO_LAYOUT);

    @Override
    public WorkspaceRepository getRepository() {
        return WORKSPACE_REPOSITORY;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return Collections.emptyList();
    }

    @Override
    public Model findModel(Artifact artifact) {
        if (GROUP_ID.equals(artifact.getGroupId())
                && ARTIFACT_ID.equals(artifact.getArtifactId())
                && VERSION.equals(artifact.getVersion())) {
            Model m = new Model();
            m.setArtifactId(ARTIFACT_ID);
            m.setGroupId(GROUP_ID);
            m.setVersion(VERSION);
            Repository repository = new Repository();
            repository.setId(REPO_ID);
            repository.setUrl(REPO_URL);
            repository.setLayout(REPO_LAYOUT);
            m.getRepositories().add(repository);
            return m;
        }
        return null;
    }
}
