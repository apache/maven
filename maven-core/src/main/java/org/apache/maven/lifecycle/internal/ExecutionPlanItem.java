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

import org.apache.maven.lifecycle.Schedule;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps individual MojoExecutions, containing information about completion status and scheduling.
 * <p/>
 * NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @author Kristian Rosenvold
 */
public class ExecutionPlanItem
{
    private final MojoExecution mojoExecution;

    private final Schedule schedule;
    // Completeness just indicates that it has been run or failed

    private final AtomicBoolean complete = new AtomicBoolean( false );

    private final Object monitor = new Object();

    public ExecutionPlanItem( MojoExecution mojoExecution, Schedule schedule )
    {
        this.mojoExecution = mojoExecution;
        this.schedule = schedule;
    }

    public MojoExecution getMojoExecution()
    {
        return mojoExecution;
    }

    public void setComplete()
    {
        boolean transitionSuccessful = ensureComplete();
        if ( !transitionSuccessful )
        {
            throw new IllegalStateException( "Expected to be able to setComplete node, but was complete already" );
        }
    }

    public boolean ensureComplete()
    {
        boolean f = complete.compareAndSet( false, true );
        notifyListeners();
        return f;
    }

    private void notifyListeners()
    {
        synchronized ( monitor )
        {
            monitor.notifyAll();
        }
    }

    public void forceComplete()
    {
        final boolean b = complete.getAndSet( true );
        if ( !b )
        {
            notifyListeners();
        } // Release anyone waiting for us
    }

    public void waitUntilDone()
        throws InterruptedException
    {
        synchronized ( monitor )
        {
            while ( !complete.get() )
            {
                monitor.wait( 100 );
            }
        }
    }

    public Schedule getSchedule()
    {
        return schedule;
    }

    public boolean hasSchedule( Schedule other )
    {
        if ( getSchedule() != null && !getSchedule().isMissingPhase() )
        {
            if ( other.getPhase().equals( getSchedule().getPhase() ) )
            {
                return true;
            }
        }
        return false;
    }

    public Plugin getPlugin()
    {
        final MojoDescriptor mojoDescriptor = getMojoExecution().getMojoDescriptor();
        return mojoDescriptor.getPluginDescriptor().getPlugin();
    }

    @Override
    public String toString()
    {
        return "ExecutionPlanItem{" + ", mojoExecution=" + mojoExecution + ", schedule=" + schedule + '}' +
            super.toString();
    }

}
