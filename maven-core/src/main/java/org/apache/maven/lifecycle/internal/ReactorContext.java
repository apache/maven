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

import org.apache.maven.execution.MavenExecutionResult;

/**
 * Context that is fixed for the entire reactor build.
 *
 * @since 3.0
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
public class ReactorContext {
    private final MavenExecutionResult result;

    private final ProjectIndex projectIndex;

    private final ClassLoader originalContextClassLoader;

    private final ReactorBuildStatus reactorBuildStatus;

    public ReactorContext(
            MavenExecutionResult result,
            ProjectIndex projectIndex,
            ClassLoader originalContextClassLoader,
            ReactorBuildStatus reactorBuildStatus) {
        this.result = result;
        this.projectIndex = projectIndex;
        this.originalContextClassLoader = originalContextClassLoader;
        this.reactorBuildStatus = reactorBuildStatus;
    }

    public ReactorBuildStatus getReactorBuildStatus() {
        return reactorBuildStatus;
    }

    public MavenExecutionResult getResult() {
        return result;
    }

    public ProjectIndex getProjectIndex() {
        return projectIndex;
    }

    public ClassLoader getOriginalContextClassLoader() {
        return originalContextClassLoader;
    }
}
