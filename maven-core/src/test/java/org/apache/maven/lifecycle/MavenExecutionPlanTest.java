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

import junit.framework.TestCase;
import org.apache.maven.lifecycle.internal.ExecutionPlanItem;
import org.apache.maven.lifecycle.internal.stub.DefaultLifecyclesStub;
import org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub;

import java.util.Iterator;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */
public class MavenExecutionPlanTest
    extends TestCase
{
    public void testFindFirstWithMatchingSchedule()
        throws Exception
    {
        MavenExecutionPlan plan = LifecycleExecutionPlanCalculatorStub.getProjectAExceutionPlan();
        final List<Scheduling> cycles = DefaultLifecyclesStub.getSchedulingList();
        final Schedule schedule = cycles.get( 0 ).getSchedules().get( 0 );

    }

    public void testForceAllComplete()
        throws Exception
    {
        MavenExecutionPlan plan = LifecycleExecutionPlanCalculatorStub.getProjectAExceutionPlan();
        plan.forceAllComplete();
        final Iterator<ExecutionPlanItem> planItemIterator = plan.iterator();
        assertFalse( planItemIterator.next().ensureComplete() );
        assertFalse( planItemIterator.next().ensureComplete() );
    }

}
