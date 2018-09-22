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

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecyclePhase;
import org.apache.maven.lifecycle.Phase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub.*;

/**
 * @author Kristian Rosenvold
 */

public class DefaultLifecyclesStub
{
    public static DefaultLifecycles createDefaultLifecycles()
    {

        List<LifecyclePhase> stubDefaultCycle =
            Arrays.<LifecyclePhase>asList( new Phase( VALIDATE.getPhase() ),
                                           new Phase( INITIALIZE.getPhase() ),
                                           new Phase( PROCESS_RESOURCES.getPhase() ),
                                           new Phase( COMPILE.getPhase() ),
                                           new Phase( TEST.getPhase() ),
                                           new Phase( PROCESS_TEST_RESOURCES.getPhase() ),
                                           new Phase( PACKAGE.getPhase() ),
                                           new Phase( "BEER" ),
                                           new Phase( INSTALL.getPhase() ) );

        // The two phases below are really for future expansion, some would say they lack a drink
        // The point being that they do not really have to match the "real" stuff,
        List<LifecyclePhase> stubCleanCycle =
            Arrays.<LifecyclePhase>asList( new Phase( PRE_CLEAN.getPhase() ),
                                           new Phase( CLEAN.getPhase() ), 
                                           new Phase( POST_CLEAN.getPhase() ) );

        List<LifecyclePhase> stubSiteCycle =
            Arrays.<LifecyclePhase>asList( new Phase( PRE_SITE.getPhase() ),
                                           new Phase( SITE.getPhase() ),
                                           new Phase( POST_SITE.getPhase() ),
                                           new Phase( SITE_DEPLOY.getPhase() ) );

        Iterator<List<LifecyclePhase>> lcs = Arrays.asList( stubDefaultCycle, stubCleanCycle, stubSiteCycle ).iterator();

        Map<String, Lifecycle> lifeCycles = new HashMap<>();
        for ( String s : DefaultLifecycles.STANDARD_LIFECYCLES )
        {
            final Lifecycle lifecycle = new Lifecycle( s, lcs.next(), null );
            lifeCycles.put( s, lifecycle );

        }
        return new DefaultLifecycles( lifeCycles, new LoggerStub() );
    }

}