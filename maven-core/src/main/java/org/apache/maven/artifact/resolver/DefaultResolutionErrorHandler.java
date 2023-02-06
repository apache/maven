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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Benjamin Bentmann
 */
@Component(role = ResolutionErrorHandler.class)
public class DefaultResolutionErrorHandler implements ResolutionErrorHandler {

    public void throwErrors(ArtifactResolutionRequest request, ArtifactResolutionResult result)
            throws ArtifactResolutionException {
        // Metadata cannot be found

        if (result.hasMetadataResolutionExceptions()) {
            throw result.getMetadataResolutionException(0);
        }

        // Metadata cannot be retrieved

        // Cyclic Dependency Error

        if (result.hasCircularDependencyExceptions()) {
            throw result.getCircularDependencyException(0);
        }

        // Version Range Violation

        if (result.hasVersionRangeViolations()) {
            throw result.getVersionRangeViolation(0);
        }

        // Transfer Error

        if (result.hasErrorArtifactExceptions()) {
            throw result.getErrorArtifactExceptions().get(0);
        }

        if (result.hasMissingArtifacts()) {
            throw new MultipleArtifactsNotFoundException(
                    request.getArtifact(),
                    toList(result.getArtifacts()),
                    result.getMissingArtifacts(),
                    request.getRemoteRepositories());
        }

        // this should never happen since we checked all possible error sources before but better be sure
        if (result.hasExceptions()) {
            throw new ArtifactResolutionException(
                    "Unknown error during artifact resolution, " + request + ", " + result.getExceptions(),
                    request.getArtifact(),
                    request.getRemoteRepositories());
        }
    }

    private static <T> List<T> toList(Collection<T> items) {
        return (items != null) ? new ArrayList<>(items) : null;
    }
}
