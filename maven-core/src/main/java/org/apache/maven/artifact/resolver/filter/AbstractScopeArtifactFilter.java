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
package org.apache.maven.artifact.resolver.filter;

import org.apache.maven.artifact.Artifact;

/**
 * Filter to only retain objects in the given artifactScope or better.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
abstract class AbstractScopeArtifactFilter implements ArtifactFilter {

    private boolean compileScope;

    private boolean runtimeScope;

    private boolean testScope;

    private boolean providedScope;

    private boolean systemScope;

    void addScopeInternal(String scope) {
        if (Artifact.SCOPE_COMPILE.equals(scope)) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
        } else if (Artifact.SCOPE_RUNTIME.equals(scope)) {
            compileScope = true;
            runtimeScope = true;
        } else if (Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals(scope)) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
        } else if (Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals(scope)) {
            systemScope = true;
            compileScope = true;
            runtimeScope = true;
        } else if (Artifact.SCOPE_TEST.equals(scope)) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
        }
    }

    public boolean include(Artifact artifact) {
        if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())) {
            return compileScope;
        } else if (Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
            return runtimeScope;
        } else if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
            return testScope;
        } else if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
            return providedScope;
        } else if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
            return systemScope;
        } else {
            return true;
        }
    }
}
