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

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Jason van  Zyl
 */
public interface LifecycleExecutor
{    
    List<String> getLifecyclePhases();
        
    /**
     * Calculate the list of {@link org.apache.maven.plugin.descriptor.MojoDescriptor} objects to run for the selected lifecycle phase.
     * 
     * @param phase
     * @param session
     * @return
     * @throws LifecycleExecutionException
     */
    List<MojoDescriptor> calculateLifecyclePlan( String lifecyclePhase, MavenSession session )
        throws LifecycleExecutionException;
        
    // For a given project packaging find all the plugins that are bound to any registered
    // lifecycles. The project builder needs to now what default plugin information needs to be
    // merged into POM being built. Once the POM builder has this plugin information, versions can be assigned
    // by the POM builder because they will have to be defined in plugin management. Once this is done then it
    // can be passed back so that the default configuraiton information can be populated.
    //
    // We need to know the specific version so that we can lookup the right version of the plugin descriptor
    // which tells us what the default configuration is.
    //
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging );

    // Given a set of {@link org.apache.maven.Plugin} objects where the GAV is set we can lookup the plugin
    // descriptor and populate the default configuration.
    //
    Set<Plugin> populateDefaultConfigurationForPlugins( Set<Plugin> plugins, MavenProject project, ArtifactRepository localRepository )
        throws LifecycleExecutionException;
    
    void execute( MavenSession session )
        throws LifecycleExecutionException, MojoFailureException, MojoExecutionException;
    
    Xpp3Dom getDefaultPluginConfiguration( String groupId, String artifactId, String version, String goal, MavenProject project, ArtifactRepository localRepository ) 
        throws LifecycleExecutionException;    
}
