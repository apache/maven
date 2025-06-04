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
package org.apache.maven.its.extensions;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.apache.maven.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("ide")
@Singleton
public class IdeWorkspaceReader implements WorkspaceReader {

    private static final Logger log = LoggerFactory.getLogger(IdeWorkspaceReader.class);

    private final WorkspaceRepository repository = new WorkspaceRepository();

    public IdeWorkspaceReader() {
        log.info("created");
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    @Nullable
    public File findArtifact(Artifact artifact) {
        log.info("findArtifact({})", artifact);
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        log.info("findVersions({})", artifact);
        return Collections.emptyList();
    }
}
