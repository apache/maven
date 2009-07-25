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

import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;

//TODO: lifecycles being executed
//TODO: what runs in each phase
//TODO: plugins that need downloading
//TODO: project dependencies that need downloading
//TODO: unfortunately the plugins need to be downloaded in order to get the plugin.xml file. need to externalize this from the plugin archive.
//TODO: this will be the class that people get in IDEs to modify
public class MavenExecutionPlan
{
    /** Individual executions that must be performed. */
    private List<MojoExecution> executions;
    
    /** For project dependency resolution, the scopes of resolution required if any. */
    private Set<String> requiredDependencyResolutionScopes;

    public MavenExecutionPlan( List<MojoExecution> executions, Set<String> requiredDependencyResolutionScopes )
    {
        this.executions = executions;
        this.requiredDependencyResolutionScopes = requiredDependencyResolutionScopes;
    }

    public List<MojoExecution> getExecutions()
    {
        return executions;
    }

    public Set<String> getRequiredResolutionScopes()
    {
        return requiredDependencyResolutionScopes;
    }        
}
