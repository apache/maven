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
package org.apache.maven.lifecycle.internal;

import junit.framework.TestCase;
import org.apache.maven.lifecycle.Schedule;
import org.apache.maven.lifecycle.internal.stub.MojoExecutorStub;
import org.apache.maven.plugin.MojoExecution;

/**
 * @author Kristian Rosenvold
 */
public class ExecutionPlanItemTest
    extends TestCase
{

    public void testSetComplete()
        throws Exception
    {
        ExecutionPlanItem item = createExecutionPlanItem( "testMojo", null );
        item.setComplete();  // This itself is a valid test
        assertTrue( item.isDone() );
    }

    public void testWaitUntilDone()
        throws Exception
    {

        final ExecutionPlanItem item =
            createExecutionPlanItem( "testMojo", createExecutionPlanItem( "testMojo2", null ) );
        new Thread( new Runnable()
        {
            public void run()
            {
                item.setComplete();
            }
        } ).start();
        item.waitUntilDone();
    }


    public static ExecutionPlanItem createExecutionPlanItem( String mojoDescription, ExecutionPlanItem downStream )
    {
        return createExecutionPlanItem( mojoDescription, downStream, null );
    }

    public static ExecutionPlanItem createExecutionPlanItem( String mojoDescription, ExecutionPlanItem downStream,
                                                             Schedule schedule )
    {
        return new ExecutionPlanItem( new MojoExecution( MojoExecutorStub.createMojoDescriptor( mojoDescription ) ),
                                      schedule );
    }


}
