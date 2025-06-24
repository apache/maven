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

import org.apache.maven.api.Event;
import org.apache.maven.api.EventType;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.execution.ExecutionEvent;

public class DefaultEvent implements Event {
    private final InternalMavenSession session;
    private final ExecutionEvent delegate;
    private final EventType eventType;

    public DefaultEvent(InternalMavenSession session, ExecutionEvent delegate, EventType eventType) {
        this.session = session;
        this.delegate = delegate;
        this.eventType = eventType;
    }

    @Override
    public EventType getType() {
        return eventType;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Optional<Project> getProject() {
        return Optional.ofNullable(session.getProject(delegate.getProject()));
    }

    @Override
    public Optional<MojoExecution> getMojoExecution() {
        return Optional.ofNullable(delegate.getMojoExecution()).map(me -> new DefaultMojoExecution(session, me));
    }

    @Override
    public Optional<Exception> getException() {
        return Optional.ofNullable(delegate.getException());
    }
}
