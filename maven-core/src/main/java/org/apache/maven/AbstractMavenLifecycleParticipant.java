package org.apache.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.execution.MavenSession;

/**
 * Allows core extensions to participate in build lifecycle.
 *
 * All callback methods (will) follow beforeXXX/afterXXX naming pattern to
 * indicate at what lifecycle point it is being called.
 *
 */
public abstract class AbstractMavenLifecycleParticipant
{

    /**
     * Invoked after all MavenProject instances have been created.
     *
     * This callback is intended to allow extensions to manipulate MavenProjects
     * before they are sorted and actual build execution starts.
     */
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        // do nothing
    }

    /**
     * Invoked after MavenSession instance has been created.
     *
     * This callback is intended to allow extensions to inject execution properties,
     * activate profiles and perform similar tasks that affect MavenProject
     * instance construction.
     */
    // TODO: This is too early for build extensions, so maybe just remove it?
    public void afterSessionStart( MavenSession session )
        throws MavenExecutionException
    {
        // do nothing
    }

}
