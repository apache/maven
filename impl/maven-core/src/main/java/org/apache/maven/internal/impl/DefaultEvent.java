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
package org.apache.maven.internal.impl;

import java.util.Optional;

import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;

public class DefaultEvent implements ExecutionEvent {
    private final InternalMavenSession session;
    private final org.apache.maven.execution.ExecutionEvent delegate;
    private final ExecutionEventType eventType;

    public DefaultEvent(
            InternalMavenSession session,
            org.apache.maven.execution.ExecutionEvent delegate,
            ExecutionEventType eventType) {
        this.session = session;
        this.delegate = delegate;
        this.eventType = eventType;
    }

    @Override
    public ExecutionEventType type() {
        return eventType;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Optional<Project> project() {
        return Optional.ofNullable(session.getProject(delegate.getProject()));
    }

    @Override
    public Optional<MojoExecution> mojoExecution() {
        return Optional.ofNullable(delegate.getMojoExecution()).map(me -> new DefaultMojoExecution(session, me));
    }

    @Override
    public Optional<Exception> exception() {
        return Optional.ofNullable(delegate.getException());
    }
}
