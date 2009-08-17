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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Provides a skeleton implementation for lifecycle listeners. The methods of this class are empty.
 * 
 * @author Benjamin Bentmann
 */
public class AbstractLifecycleListener
    implements LifecycleListener
{

    public void sessionStarted( LifecycleEvent event )
    {
        // default does nothing
    }

    public void sessionEnded( LifecycleEvent event )
    {
        // default does nothing
    }

    public void projectSkipped( LifecycleEvent event )
    {
        // default does nothing
    }

    public void projectStarted( LifecycleEvent event )
    {
        // default does nothing
    }

    public void projectSucceeded( LifecycleEvent event )
    {
        // default does nothing
    }

    public void projectFailed( LifecycleEvent event )
    {
        // default does nothing
    }

    public void forkStarted( LifecycleEvent event )
    {
        // default does nothing
    }

    public void forkSucceeded( LifecycleEvent event )
    {
        // default does nothing
    }

    public void forkFailed( LifecycleEvent event )
    {
        // default does nothing
    }

    public void mojoSkipped( LifecycleEvent event )
    {
        // default does nothing
    }

    public void mojoStarted( LifecycleEvent event )
    {
        // default does nothing
    }

    public void mojoSucceeded( LifecycleEvent event )
    {
        // default does nothing
    }

    public void mojoFailed( LifecycleEvent event )
    {
        // default does nothing
    }

}
