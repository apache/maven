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
package org.apache.maven.lifecycle.internal;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * Holds data relevant for an execution event.
 *
 */
class DefaultExecutionEvent implements ExecutionEvent {

    private final Type type;

    private final MavenSession session;

    private final MojoExecution mojoExecution;

    private final Exception exception;

    DefaultExecutionEvent(Type type, MavenSession session, MojoExecution mojoExecution, Exception exception) {
        this.type = type;
        this.session = session;
        this.mojoExecution = mojoExecution;
        this.exception = exception;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public MavenSession getSession() {
        return session;
    }

    @Override
    public MavenProject getProject() {
        return session.getCurrentProject();
    }

    @Override
    public MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    @Override
    public Exception getException() {
        return exception;
    }
}
