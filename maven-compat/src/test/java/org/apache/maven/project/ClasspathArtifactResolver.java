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
package org.apache.maven.project;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * @author Benjamin Bentmann
 */
@Component(role = ArtifactResolver.class, hint = "classpath")
public class ClasspathArtifactResolver implements ArtifactResolver {

    public List<ArtifactResult> resolveArtifacts(
            RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {
        List<ArtifactResult> results = new ArrayList<>();

        for (ArtifactRequest request : requests) {
            ArtifactResult result = new ArtifactResult(request);
            results.add(result);

            Artifact artifact = request.getArtifact();
            if ("maven-test".equals(artifact.getGroupId())) {
                String scope = artifact.getArtifactId().substring("scope-".length());

                try {
                    artifact = artifact.setFile(ProjectClasspathTest.getFileForClasspathResource(
                            ProjectClasspathTest.dir + "transitive-" + scope + "-dep.xml"));
                    result.setArtifact(artifact);
                } catch (FileNotFoundException | URISyntaxException e) {
                    throw new IllegalStateException("Missing test POM for " + artifact, e);
                }
            } else {
                result.addException(new ArtifactNotFoundException(artifact, null));
                throw new ArtifactResolutionException(results);
            }
        }

        return results;
    }

    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request)
            throws ArtifactResolutionException {
        return resolveArtifacts(session, Collections.singleton(request)).get(0);
    }
}
