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
package org.apache.maven.plugin;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;

/**
 * Interface allows overriding default mojo execution strategy For example it is possible wrap some mojo execution to
 * decorate default functionality or skip some executions
 */
public interface MojosExecutionStrategy {

    /**
     * Entry point to the execution strategy
     *
     * @param mojos             list of mojos representing a project build
     * @param session           current session
     * @param mojoExecutionRunner mojo execution task which must be invoked by a strategy to actually run it
     * @throws LifecycleExecutionException
     */
    void execute(List<MojoExecution> mojos, MavenSession session, MojoExecutionRunner mojoExecutionRunner)
            throws LifecycleExecutionException;
}
