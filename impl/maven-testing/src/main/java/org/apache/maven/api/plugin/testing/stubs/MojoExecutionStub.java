package org.apache.maven.api.plugin.testing.stubs;

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

import java.util.Optional;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.xml.XmlNode;

/**
 * Stub for {@link MojoExecution}.
 */
public class MojoExecutionStub implements MojoExecution
{
    private final String artifactId;
    private final String executionId;
    private final String goal;
    private final XmlNode dom;

    public MojoExecutionStub( String artifactId, String executionId, String goal )
    {
        this( artifactId, executionId, goal, null );
    }

    public MojoExecutionStub( String artifactId, String executionId, String goal, XmlNode dom )
    {
        this.artifactId = artifactId;
        this.executionId = executionId;
        this.goal = goal;
        this.dom = dom;
    }

    @Override
    public Plugin getPlugin()
    {
        return Plugin.newBuilder()
                .artifactId( artifactId )
                .build();
    }

    @Override
    public String getExecutionId()
    {
        return executionId;
    }

    @Override
    public String getGoal()
    {
        return goal;
    }

    @Override
    public Optional<XmlNode> getConfiguration()
    {
        return Optional.ofNullable( dom );
    }
}
