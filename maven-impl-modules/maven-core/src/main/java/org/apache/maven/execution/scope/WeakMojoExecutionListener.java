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
package org.apache.maven.execution.scope;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Extension point that allows build extensions observe and possibly veto mojo executions.
 * <p>
 * Unlike {@link org.apache.maven.execution.MojoExecutionListener}, this extension point does not
 * trigger instantiation of the component, hence "weak" class name prefix. Only applies to mojo execution
 * scoped components.
 * </p>
 * <strong>Note:</strong> This interface is part of work in progress and can be changed or removed without notice.
 *
 * @see org.apache.maven.execution.MojoExecutionListener
 * @since 3.1.2
 */
public interface WeakMojoExecutionListener {
    void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException;

    void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException;

    void afterExecutionFailure(MojoExecutionEvent event);
}
