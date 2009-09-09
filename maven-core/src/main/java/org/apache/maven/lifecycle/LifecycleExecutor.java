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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * @author Jason van  Zyl
 */
public interface LifecycleExecutor
{

    // USED BY MAVEN HELP PLUGIN
    @Deprecated
    String ROLE = LifecycleExecutor.class.getName();

    /**
     * Calculate the list of {@link org.apache.maven.plugin.descriptor.MojoDescriptor} objects to run for the selected lifecycle phase.
     * 
     * @param phase
     * @param session
     * @return
     * @throws InvalidPluginDescriptorException 
     * @throws LifecycleExecutionException
     */
    MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException,
        InvalidPluginDescriptorException, PluginManagerException, LifecyclePhaseNotFoundException,
        LifecycleNotFoundException, PluginVersionResolutionException;
        
    // For a given project packaging find all the plugins that are bound to any registered
    // lifecycles. The project builder needs to now what default plugin information needs to be
    // merged into POM being built. Once the POM builder has this plugin information, versions can be assigned
    // by the POM builder because they will have to be defined in plugin management. Once this is done then it
    // can be passed back so that the default configuraiton information can be populated.
    //
    // We need to know the specific version so that we can lookup the right version of the plugin descriptor
    // which tells us what the default configuration is.
    //
    /**
     * @return The plugins bound to the lifecycles of the specified packaging or {@code null} if the packaging is
     *         unknown.
     */
    Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging );

    void execute( MavenSession session );

    /**
     * Calculates the forked mojo executions requested by the mojo associated with the specified mojo execution.
     * 
     * @param mojoExecution The mojo execution for which to calculate the forked mojo executions, must not be {@code
     *            null}.
     * @param session The current build session that holds the projects and further settings, must not be {@code null}.
     */
    void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException;

    /**
     * Executes the previously calculated forked mojo executions of the given mojo execution. If the specified mojo
     * execution requires no forking, this method does nothing. The return value denotes a subset of the projects from
     * the session that have been forked. The method {@link MavenProject#getExecutionProject()} of those projects
     * returns the project clone on which the forked execution were performed. It is the responsibility of the caller to
     * reset those execution projects to {@code null} once they are no longer needed to free memory and to avoid
     * accidental usage by unrelated mojos.
     * 
     * @param mojoExecution The mojo execution whose forked mojo executions should be processed, must not be {@code
     *            null}.
     * @param session The current build session that holds the projects and further settings, must not be {@code null}.
     * @return The (unmodifiable) list of projects that have been forked, can be empty if no forking was required but
     *         will never be {@code null}.
     */
    List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException;

}
