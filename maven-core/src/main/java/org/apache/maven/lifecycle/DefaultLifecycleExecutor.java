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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * A facade that provides lifecycle services to components outside maven core.
 *
 * Note that this component is not normally used from within core itself.
 *
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 */
@Named
@Singleton
public class DefaultLifecycleExecutor
    implements LifecycleExecutor
{

    @Inject
    private LifeCyclePluginAnalyzer lifeCyclePluginAnalyzer;

    @Inject
    private DefaultLifecycles defaultLifeCycles;

    @Inject
    private LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator;

    @Inject
    private LifecycleExecutionPlanCalculator lifecycleExecutionPlanCalculator;

    @Inject
    private MojoExecutor mojoExecutor;

    @Inject
    private LifecycleStarter lifecycleStarter;


    public void execute( MavenSession session )
    {
        lifecycleStarter.execute( session );
    }

    @Inject
    private MojoDescriptorCreator mojoDescriptorCreator;

    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.

    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //
    // TODO This whole method could probably removed by injecting lifeCyclePluginAnalyzer straight into client site.
    // TODO But for some reason the whole plexus appcontext refuses to start when I try this.

    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        return lifeCyclePluginAnalyzer.getPluginsBoundByDefaultToAllLifecycles( packaging );
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
