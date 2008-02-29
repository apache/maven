package org.apache.maven.lifecycle;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

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

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class LifecycleExecutionException
    extends Exception
{
    private final MavenProject project;

    public LifecycleExecutionException( String message, MavenProject project )
    {
        super( message );
        this.project = project;
    }

    public LifecycleExecutionException( String message, MavenProject project,
                                        PluginManagerException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message, MavenProject project,
                                        ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message, MavenProject project,
                                        ArtifactResolutionException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message,
                                        MavenProject project, PluginLoaderException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message,
                                        MavenProject project, LifecycleException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message, MavenProject project,
                                        InvalidDependencyVersionException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message, MavenProject project,
                                        PluginConfigurationException cause )
    {
        super( message, cause );
        this.project = project;
    }

    public LifecycleExecutionException( String message,
                                        Throwable cause )
    {
        super( message, cause );
        project = null;
    }

    public MavenProject getProject()
    {
        return project;
    }

}
