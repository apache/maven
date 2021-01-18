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

import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Just asserts that it's able to create those components. Handy when CDI container gets a nervous breakdown.
 *
 * @author Kristian Rosenvold
 */

public class LifecycleExecutorSubModulesTest
    extends AbstractCoreMavenComponentTestCase
{
    @Inject
    private DefaultLifecycles defaultLifeCycles;

    @Inject
    private MojoExecutor mojoExecutor;

    @Inject
    private LifecycleModuleBuilder lifeCycleBuilder;

    @Inject
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Inject
    private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

    @Inject
    private LifeCyclePluginAnalyzer lifeCyclePluginAnalyzer;

    @Inject
    private LifecycleTaskSegmentCalculator lifeCycleTaskSegmentCalculator;

    @Inject
    private ExceptionHandler exceptionHandler;

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }

    @Test
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
        assertNotNull( exceptionHandler );
    }

}