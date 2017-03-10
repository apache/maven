package org.apache.maven.execution;

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

import org.apache.maven.lifecycle.LifecycleExecutionException;

/**
 * <p>
 * Extension point that allows build extensions observe and possibly veto project build execution.
 * </p>
 * <strong>Note:</strong> This interface is part of work in progress and can be changed or removed without notice.
 *
 * @see ExecutionListener
 * @see MojoExecutionListener
 * @since 3.1.2
 */
public interface ProjectExecutionListener
{
    void beforeProjectExecution( ProjectExecutionEvent event )
        throws LifecycleExecutionException;

    void beforeProjectLifecycleExecution( ProjectExecutionEvent event )
        throws LifecycleExecutionException;

    void afterProjectExecutionSuccess( ProjectExecutionEvent event )
        throws LifecycleExecutionException;

    void afterProjectExecutionFailure( ProjectExecutionEvent event );
}
