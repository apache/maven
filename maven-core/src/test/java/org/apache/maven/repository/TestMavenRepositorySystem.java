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
package org.apache.maven.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.eclipse.sisu.Priority;

@Named
@Priority(10)
@Singleton
public class TestMavenRepositorySystem extends MavenRepositorySystem {

    @Inject
    public TestMavenRepositorySystem(
            ArtifactHandlerManager artifactHandlerManager, Map<String, ArtifactRepositoryLayout> layouts) {
        super(artifactHandlerManager, layouts);
    }

    @Override
    public ArtifactRepository createDefaultRemoteRepository() throws Exception {
        return new MavenArtifactRepository(
                DEFAULT_REMOTE_REPO_ID,
                "file://"
                        + new File(System.getProperty("basedir", "."), "src/test/remote-repo")
                                .getAbsoluteFile()
                                .toURI()
                                .getPath(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }
}
