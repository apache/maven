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
import org.apache.maven.lifecycle.Schedule;
import org.apache.maven.lifecycle.Scheduling;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */

public class DefaultLifecyclesStub
{
    public static DefaultLifecycles createDefaultLifeCycles()
    {
        final Lifecycle lifecycle1 = new Lifecycle( "abc", Arrays.asList( "compile" ), null );
        final Lifecycle lifecycle2 = new Lifecycle( "abc", Arrays.asList( "test" ), null );
        final List<Lifecycle> lifeCycles = Arrays.asList( lifecycle1, lifecycle2 );
        final List<Scheduling> schedulingList = getSchedulingList();
        final DefaultLifecycles defaultLifecycles = new DefaultLifecycles( lifeCycles, schedulingList );
        try
        {
            defaultLifecycles.initialize();
        }
        catch ( InitializationException e )
        {
            throw new RuntimeException( e );
        }
        return defaultLifecycles;
    }

    public static List<Scheduling> getSchedulingList()
    {
        return Arrays.asList( new Scheduling( "default", Arrays.asList( new Schedule( "compile", false, false ),
                                                                        new Schedule( "test", false, true ) ) ) );
    }
}