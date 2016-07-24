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


package org.apache.maven.lifecycle;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Just asserts that it's able to create those components. Handy when plexus gets a nervous breakdown.
 *
 * @author Kristian Rosenvold
 */

public class LifecycleExecutorSubModulesTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private MojoExecutor mojoExecutor;

    @Requirement
    private LifecycleModuleBuilder lifeCycleBuilder;

    @Requirement
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Requirement
    private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

    @Requirement
    private LifecyclePluginAnalyzer lifeCyclePluginAnalyzer;

    @Requirement
    private LifecycleTaskSegmentCalculator lifeCycleTaskSegmentCalculator;


    protected void setUp()
        throws Exception
    {
        super.setUp();
        defaultLifeCycles = lookup( DefaultLifecycles.class );
        mojoExecutor = lookup( MojoExecutor.class );
        lifeCycleBuilder = lookup( LifecycleModuleBuilder.class );
        lifeCycleDependencyResolver = lookup( LifecycleDependencyResolver.class );
        lifeCycleExecutionPlanCalculator = lookup( LifecycleExecutionPlanCalculator.class );
        lifeCyclePluginAnalyzer = lookup( LifecyclePluginAnalyzer.class );
        lifeCycleTaskSegmentCalculator = lookup( LifecycleTaskSegmentCalculator.class );
        lookup( ExceptionHandler.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        defaultLifeCycles = null;
        super.tearDown();
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }

    public void testCreation()
        throws Exception
    {
        assertNotNull( defaultLifeCycles );
        assertNotNull( mojoExecutor );
        assertNotNull( lifeCycleBuilder );
        assertNotNull( lifeCycleDependencyResolver );
        assertNotNull( lifeCycleExecutionPlanCalculator );
        assertNotNull( lifeCyclePluginAnalyzer );
        assertNotNull( lifeCycleTaskSegmentCalculator );
    }

}