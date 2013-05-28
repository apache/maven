package org.apache.maven;

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

import org.apache.maven.plugin.MojoFailureException;

/**
 * Exception which occurs when a normal (i.e. non-aggregator) mojo fails to
 * execute. In this case, the mojo failed while executing against a particular
 * project instance, so we can wrap the {@link MojoFailureException} with context
 * information including projectId that caused the failure.
 *
 * @author jdcasey
 *
 */
public class ProjectBuildFailureException
    extends BuildFailureException
{

    private final String projectId;

    public ProjectBuildFailureException( String projectId, MojoFailureException cause )
    {
        super( "Build for project: " + projectId + " failed during execution of mojo.", cause );

        this.projectId = projectId;
    }

    public MojoFailureException getMojoFailureException()
    {
        return (MojoFailureException) getCause();
    }

    public String getProjectId()
    {
        return projectId;
    }
}
