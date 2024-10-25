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
package org.apache.maven.lifecycle.internal;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.artifact.ProjectArtifactsCache;

public class SetWithResolutionResult extends AbstractSet<Artifact>
        implements ProjectArtifactsCache.ArtifactsSetWithResult {
    final DependencyResolutionResult result;
    final Set<Artifact> artifacts;

    public SetWithResolutionResult(DependencyResolutionResult result, Set<Artifact> artifacts) {
        this.result = result;
        this.artifacts = Collections.unmodifiableSet(artifacts);
    }

    @Override
    public Iterator<Artifact> iterator() {
        return artifacts.iterator();
    }

    @Override
    public int size() {
        return artifacts.size();
    }

    @Override
    public DependencyResolutionResult getResult() {
        return result;
    }
}
