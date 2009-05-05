package org.apache.maven.project;

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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A stub implementation that assumes an empty lifecycle to bypass interaction with the plugin manager and to avoid
 * plugin artifact resolution from repositories.
 * 
 * @author Benjamin Bentmann
 */
public class EmptyLifecycleExecutor
    implements LifecycleExecutor
{

    public List<MojoExecution> calculateLifecyclePlan( String lifecyclePhase, MavenSession session )
        throws LifecycleExecutionException
    {
        return Collections.emptyList();
    }

    public void execute( MavenSession session )
        throws LifecycleExecutionException, MojoFailureException, MojoExecutionException
    {
    }

    public Xpp3Dom getDefaultPluginConfiguration( String groupId, String artifactId, String version, String goal,
                                                  MavenProject project, ArtifactRepository localRepository )
        throws LifecycleExecutionException
    {
        return null;
    }

    public List<String> getLifecyclePhases()
    {
        return Collections.emptyList();
    }

    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        return Collections.emptySet();
    }

    public Set<Plugin> populateDefaultConfigurationForPlugins( Set<Plugin> plugins, MavenProject project,
                                                               ArtifactRepository localRepository )
        throws LifecycleExecutionException
    {
        return plugins;
    }

}
