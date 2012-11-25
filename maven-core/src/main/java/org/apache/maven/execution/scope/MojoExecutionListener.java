package org.apache.maven.execution.scope;

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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Helper interface that allows mojo execution scoped components to be notified before execution start and after
 * execution completion. The main purpose of this interface is to allow mojo execution scoped components perform setup
 * before and cleanup after mojo execution.
 * <p>
 * TODO decide if Mojo should be passed as parameter of callback methods
 * 
 * @author igor
 * @since 3.1.2
 * @provisional This interface is part of work in progress and can be changed or removed without notice.
 */
public interface MojoExecutionListener
{
    // TODO decide if this is needed
    // public void beforeExecution() throws MojoExecutionException;

    public void afterMojoExecutionSuccess()
        throws MojoExecutionException;

    // TODO decide if this is needed.
    // public void afterExecutionFailure(Throwable t) throws MojoExecutionException;

    public void afterMojoExecutionAlways()
        throws MojoExecutionException;
}
