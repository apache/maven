package org.apache.maven.lifecycle;

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

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.project.MavenProject;

/**
 * Responsible for orchestrating the process of building the ordered list of
 * steps required to achieve the specified set of tasks passed into Maven, then
 * executing these mojos in order. This class also manages the various error messages
 * that may occur during this process, and directing the behavior of the build
 * according to what's specified in {@link MavenExecutionRequest#getReactorFailureBehavior()}.
 *
 * @author Jason van Zyl
 * @author jdcasey
 * @version $Id$
 */
public interface LifecycleExecutor
{
    String ROLE = LifecycleExecutor.class.getName();

    /**
     * Provides a fail-fast way to check that all goals specified in {@link MavenExecutionRequest#getGoals()}
     * or {@link MavenSession#getGoals()} is valid.
     */
    TaskValidationResult isTaskValid( String task, MavenSession session, MavenProject rootProject );

    /**
     * Order and execute mojos associated with the current set of projects in the
     * reactor. Specific lifecycle phases and mojo invocations that determine what
     * phases and mojos this method will attempt to execute are provided in {@link MavenSession#getGoals()},
     * which is populated from {@link MavenExecutionRequest#getGoals()}.
     */
    void execute( MavenSession session, ReactorManager rm, EventDispatcher dispatcher )
        throws LifecycleExecutionException, BuildFailureException;

}
