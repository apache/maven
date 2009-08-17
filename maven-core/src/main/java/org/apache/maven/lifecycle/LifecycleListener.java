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
 * Defines events that the lifecycle executor fires during a session.
 * 
 * @author Benjamin Bentmann
 */
public interface LifecycleListener
{

    void sessionStarted( LifecycleEvent event );

    void sessionEnded( LifecycleEvent event );

    void projectSkipped( LifecycleEvent event );

    void projectStarted( LifecycleEvent event );

    void projectSucceeded( LifecycleEvent event );

    void projectFailed( LifecycleEvent event );

    void mojoSkipped( LifecycleEvent event );

    void mojoStarted( LifecycleEvent event );

    void mojoSucceeded( LifecycleEvent event );

    void mojoFailed( LifecycleEvent event );

    void forkStarted( LifecycleEvent event );

    void forkSucceeded( LifecycleEvent event );

    void forkFailed( LifecycleEvent event );

}
