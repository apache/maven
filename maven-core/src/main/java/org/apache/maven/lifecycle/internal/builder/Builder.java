package org.apache.maven.lifecycle.internal.builder;

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
import java.util.concurrent.ExecutionException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;

/**
 * <p>
 * A {@link Builder} encapsulates a strategy for building a set of Maven projects. The default strategy in Maven builds
 * the the projects serially, but a {@link Builder} can employ any type of concurrency model to build the projects.
 * </p>
 * <strong>Note:</strong> This interface is part of work in progress and can be changed or removed without notice.
 * @author jvanzyl
 */
public interface Builder
{
    //
    // Be nice to whittle this down to Session, maybe add task segments to the session. The session really is the
    // the place to store reactor related information.
    //
    void build( MavenSession session, ReactorContext reactorContext, ProjectBuildList projectBuilds,
                List<TaskSegment> taskSegments, ReactorBuildStatus reactorBuildStatus )
        throws ExecutionException, InterruptedException;
}
