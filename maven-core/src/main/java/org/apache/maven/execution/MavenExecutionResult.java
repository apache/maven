package org.apache.maven.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.project.MavenProject;

/**
 * @author Jason van Zyl
 */
public interface MavenExecutionResult
{
    MavenExecutionResult setProject( MavenProject project );
    MavenProject getProject();

    MavenExecutionResult setTopologicallySortedProjects( List projects );
    List<MavenProject> getTopologicallySortedProjects();

    MavenExecutionResult setArtifactResolutionResult( ArtifactResolutionResult result );
    ArtifactResolutionResult getArtifactResolutionResult();

    MavenExecutionResult setReactorManager( ReactorManager reactorManager );
    ReactorManager getReactorManager();

    // for each exception
    // - knowing what artifacts are missing
    // - project building exception
    // - invalid project model exception: list of markers
    // - xmlpull parser exception
    List<Exception> getExceptions();

    MavenExecutionResult addException( Throwable e );

    boolean hasExceptions();
        
    MavenExecutionResult setExceptionSummary( ExceptionSummary exceptionSummary );
    ExceptionSummary getExceptionSummary();
}
