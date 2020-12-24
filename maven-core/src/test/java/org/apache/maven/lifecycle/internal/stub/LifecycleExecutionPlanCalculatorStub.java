/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.maven.lifecycle.internal.stub;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.DefaultLifecyclePluginAnalyzer;
import org.apache.maven.lifecycle.internal.ExecutionPlanItem;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kristian Rosenvold
 */
public class LifecycleExecutionPlanCalculatorStub
    implements LifecycleExecutionPlanCalculator
{
    // clean

    public final static MojoDescriptor PRE_CLEAN = createMojoDescriptor( "pre-clean" );

    public final static MojoDescriptor CLEAN = createMojoDescriptor( "clean" );

    public final static MojoDescriptor POST_CLEAN = createMojoDescriptor( "post-clean" );

    // default (or at least some of them)

    public final static MojoDescriptor VALIDATE = createMojoDescriptor( "validate" );

    public final static MojoDescriptor INITIALIZE = createMojoDescriptor( "initialize" );

    public final static MojoDescriptor TEST_COMPILE = createMojoDescriptor( "test-compile" );

    public final static MojoDescriptor PROCESS_TEST_RESOURCES = createMojoDescriptor( "process-test-resources" );

    public final static MojoDescriptor PROCESS_RESOURCES = createMojoDescriptor( "process-resources" );

    public final static MojoDescriptor COMPILE = createMojoDescriptor( "compile", true );

    public final static MojoDescriptor TEST = createMojoDescriptor( "test" );

    public final static MojoDescriptor PACKAGE = createMojoDescriptor( "package" );

    public final static MojoDescriptor INSTALL = createMojoDescriptor( "install" );

    // site

    public final static MojoDescriptor PRE_SITE = createMojoDescriptor( "pre-site" );

    public final static MojoDescriptor SITE = createMojoDescriptor( "site" );

    public final static MojoDescriptor POST_SITE = createMojoDescriptor( "post-site" );

    public final static MojoDescriptor SITE_DEPLOY = createMojoDescriptor( "site-deploy" );

    // wrapper

    public final static MojoDescriptor WRAPPER = createMojoDescriptor( "wrapper" );

    /**
     * @deprecated instead use {@link #getNumberOfExecutions(ProjectBuildList)}
     */
    @Deprecated
    public int getNumberOfExceutions( ProjectBuildList projectBuildList )
        throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
        NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException
    {
        int result = 0;
        for ( ProjectSegment projectBuild : projectBuildList )
        {
            MavenExecutionPlan plan = calculateExecutionPlan( projectBuild.getSession(), projectBuild.getProject(),
                                                              projectBuild.getTaskSegment().getTasks() );
            result += plan.size();
        }
        return result;
    }

    public int getNumberOfExecutions( ProjectBuildList projectBuildList )
        throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
        NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException
    {
        return getNumberOfExceutions( projectBuildList );
    }

    public void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        // Maybe do something ?
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, MavenProject project, List<Object> tasks,
                                                      boolean setup )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        if ( project.equals( ProjectDependencyGraphStub.A ) )
        {
            return getProjectAExceutionPlan();
        }
        if ( project.equals( ProjectDependencyGraphStub.B ) )
        {
            return getProjectBExecutionPlan();
        }
        // The remaining are basically "for future expansion"
        List<MojoExecution> me = new ArrayList<>();
        me.add( createMojoExecution( "resources", "default-resources", PROCESS_RESOURCES ) );
        me.add( createMojoExecution( "compile", "default-compile", COMPILE ) );
        return createExecutionPlan( project, me );
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, MavenProject project, List<Object> tasks )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        return calculateExecutionPlan( session, project, tasks, true );
    }

    public void setupMojoExecution( MavenSession session, MavenProject project, MojoExecution mojoExecution, Set<MojoDescriptor> alreadyForkedExecutions )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
    }

    public static MavenExecutionPlan getProjectAExceutionPlan()
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        List<MojoExecution> me = new ArrayList<>();
        me.add( createMojoExecution( "initialize", "default-initialize", INITIALIZE ) );
        me.add( createMojoExecution( "resources", "default-resources", PROCESS_RESOURCES ) );
        me.add( createMojoExecution( "compile", "default-compile", COMPILE ) );
        me.add( createMojoExecution( "testResources", "default-testResources", PROCESS_TEST_RESOURCES ) );
        me.add( createMojoExecution( "testCompile", "default-testCompile", TEST_COMPILE ) );
        me.add( createMojoExecution( "test", "default-test", TEST ) );
        me.add( createMojoExecution( "war", "default-war", PACKAGE ) );
        me.add( createMojoExecution( "install", "default-install", INSTALL ) );
        return createExecutionPlan( ProjectDependencyGraphStub.A.getExecutionProject(), me );
    }

    public static MavenExecutionPlan getProjectBExecutionPlan()
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        List<MojoExecution> me = new ArrayList<>();
        me.add( createMojoExecution( "enforce", "enforce-versions", VALIDATE ) );
        me.add( createMojoExecution( "resources", "default-resources", PROCESS_RESOURCES ) );
        me.add( createMojoExecution( "compile", "default-compile", COMPILE ) );
        me.add( createMojoExecution( "testResources", "default-testResources", PROCESS_TEST_RESOURCES ) );
        me.add( createMojoExecution( "testCompile", "default-testCompile", TEST_COMPILE ) );
        me.add( createMojoExecution( "test", "default-test", TEST ) );
        return createExecutionPlan( ProjectDependencyGraphStub.B.getExecutionProject(), me );
    }


    private static MavenExecutionPlan createExecutionPlan( MavenProject project, List<MojoExecution> mojoExecutions )
        throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
        NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException
    {
        final List<ExecutionPlanItem> planItemList =
            ExecutionPlanItem.createExecutionPlanItems( project, mojoExecutions );
        return new MavenExecutionPlan( planItemList, DefaultLifecyclesStub.createDefaultLifecycles() );
    }

    private static MojoExecution createMojoExecution( String goal, String executionId, MojoDescriptor mojoDescriptor )
    {
        InputSource defaultBindings = new InputSource();
        defaultBindings.setModelId( DefaultLifecyclePluginAnalyzer.DEFAULTLIFECYCLEBINDINGS_MODELID );

        final Plugin plugin = mojoDescriptor.getPluginDescriptor().getPlugin();
        plugin.setLocation( "version", new InputLocation( 12, 34, defaultBindings ) );
        MojoExecution result = new MojoExecution( plugin, goal, executionId );
        result.setConfiguration( new Xpp3Dom( executionId + "-" + goal ) );
        result.setMojoDescriptor( mojoDescriptor );
        result.setLifecyclePhase( mojoDescriptor.getPhase() );

        return result;

    }

    public static MojoDescriptor createMojoDescriptor( String phaseName )
    {
        return createMojoDescriptor( phaseName, false );
    }

    public static MojoDescriptor createMojoDescriptor( String phaseName, boolean threadSafe )
    {
        final MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPhase( phaseName );
        final PluginDescriptor descriptor = new PluginDescriptor();
        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.test.MavenExecutionPlan" );
        plugin.setArtifactId( "stub-plugin-" + phaseName );
        descriptor.setPlugin( plugin );
        descriptor.setArtifactId( "artifact." + phaseName );
        mojoDescriptor.setPluginDescriptor( descriptor );
        mojoDescriptor.setThreadSafe( threadSafe );
        return mojoDescriptor;
    }


    public static Set<String> getScopes()
    {
        return new HashSet<>( Arrays.asList( "compile" ) );
    }

}
