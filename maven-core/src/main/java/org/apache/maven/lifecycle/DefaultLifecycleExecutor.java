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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A facade that provides lifecycle services to components outside maven core.
 *
 * Note that this component is not normally used from within core itself.
 *
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 */
@Component( role = LifecycleExecutor.class )
public class DefaultLifecycleExecutor
    implements LifecycleExecutor
{

    @Requirement
    private LifecyclePluginAnalyzer lifecyclePluginAnalyzer;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator;

    @Requirement
    private LifecycleExecutionPlanCalculator lifecycleExecutionPlanCalculator;

    @Requirement
    private MojoExecutor mojoExecutor;

    @Requirement
    private LifecycleStarter lifecycleStarter;


    public void execute( MavenSession session )
    {
        lifecycleStarter.execute( session );
    }

    @Requirement
    private MojoDescriptorCreator mojoDescriptorCreator;

    /**
     * @deprecated As of Maven 3.4, please use
     * {@link LifecyclePluginAnalyzer#getLifecycleModel(org.apache.maven.model.Model)}.
     */
    @Deprecated
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
        throws LifecycleMappingNotFoundException
    {
        return lifecyclePluginAnalyzer.getPluginsBoundByDefaultToAllLifecycles( packaging );
    }

    // USED BY MAVEN HELP PLUGIN

    @Deprecated
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    {
        return defaultLifeCycles.getPhaseToLifecycleMap();
    }

    // NOTE: Backward-compat with maven-help-plugin:2.1

    @SuppressWarnings( { "UnusedDeclaration" } )
    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project, String invokedVia,
                                      boolean canUsePrefix, boolean isOptionalMojo )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        return mojoDescriptorCreator.getMojoDescriptor( task, session, project );
    }

    // Used by m2eclipse

    @SuppressWarnings( { "UnusedDeclaration" } )
    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, boolean setup, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
        PluginVersionResolutionException
    {
        List<TaskSegment> taskSegments =
            lifecycleTaskSegmentCalculator.calculateTaskSegments( session, Arrays.asList( tasks ) );

        TaskSegment mergedSegment = new TaskSegment( false );

        for ( TaskSegment taskSegment : taskSegments )
        {
            mergedSegment.getTasks().addAll( taskSegment.getTasks() );
        }

        return lifecycleExecutionPlanCalculator.calculateExecutionPlan( session, session.getCurrentProject(),
                                                                        mergedSegment.getTasks(), setup );
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
        PluginVersionResolutionException
    {
        return calculateExecutionPlan( session, true, tasks );
    }

    // Site 3.x
    public void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        lifecycleExecutionPlanCalculator.calculateForkedExecutions( mojoExecution, session );
    }

    // Site 3.x
    public List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws LifecycleExecutionException
    {
        return mojoExecutor.executeForkedExecutions( mojoExecution, session,
                                                     new ProjectIndex( session.getProjects() ) );
    }

}
