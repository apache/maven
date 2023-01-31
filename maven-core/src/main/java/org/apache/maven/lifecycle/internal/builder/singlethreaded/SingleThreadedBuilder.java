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
package org.apache.maven.lifecycle.internal.builder.singlethreaded;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * <p>
 * A {@link Builder} encapsulates a strategy for building a set of Maven projects. The default strategy in Maven builds
 * the the projects serially, but a {@link Builder} can employ any type of concurrency model to build the projects.
 */
@Component(role = Builder.class, hint = "singlethreaded")
public class SingleThreadedBuilder implements Builder {
    @Requirement
    private LifecycleModuleBuilder lifecycleModuleBuilder;

    public void build(
            MavenSession session,
            ReactorContext reactorContext,
            ProjectBuildList projectBuilds,
            List<TaskSegment> taskSegments,
            ReactorBuildStatus reactorBuildStatus) {
        for (TaskSegment taskSegment : taskSegments) {
            for (ProjectSegment projectBuild : projectBuilds.getByTaskSegment(taskSegment)) {
                try {
                    lifecycleModuleBuilder.buildProject(
                            session, reactorContext, projectBuild.getProject(), taskSegment);
                    if (reactorBuildStatus.isHalted()) {
                        break;
                    }
                } catch (Exception e) {
                    break; // Why are we just ignoring this exception? Are exceptions are being used for flow control
                }
            }
        }
    }
}
