package org.apache.maven.lifecycle;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
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
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class LifecycleExecutionException
    extends Exception
{
    public LifecycleExecutionException( String message )
    {
        super( message );
    }

    public LifecycleExecutionException( String message,
                                        PluginManagerException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        PluginNotFoundException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        PluginVersionResolutionException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        InvalidVersionSpecificationException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        InvalidPluginException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        ArtifactNotFoundException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        ArtifactResolutionException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        PluginLoaderException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        LifecycleException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        InvalidDependencyVersionException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        MojoExecutionException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        PluginConfigurationException cause )
    {
        super( message, cause );
    }

    public LifecycleExecutionException( String message,
                                        PluginVersionNotFoundException cause )
    {
        super( message, cause );
    }

}
