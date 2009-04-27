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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author Jason van  Zyl
 */
public interface LifecycleExecutor
{    
    List<String> getLifecyclePhases();
        
    /**
     * Calculate the list of {@link org.apache.maven.plugin.descriptor.MojoDescriptor} objects to run for the selected lifecycle phase.
     * 
     * @param phase
     * @param session
     * @return
     * @throws LifecycleExecutionException
     */
    List<MojoDescriptor> calculateLifecyclePlan( String lifecyclePhase, MavenSession session )
        throws LifecycleExecutionException;
        
    void execute( MavenSession session )
        throws LifecycleExecutionException, BuildFailureException;
}
