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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.project.MavenProject;
import org.apache.maven.slf4j.MavenSimpleLogger;

/**
 * Forwards log messages to the client.
 */
public class ConcurrentLogOutput implements AutoCloseable {

    private static final ThreadLocal<ProjectExecutionContext> CONTEXT = new InheritableThreadLocal<>();

    public ConcurrentLogOutput() {
        MavenSimpleLogger.setLogSink(this::accept);
    }

    protected void accept(String message) {
        ProjectExecutionContext context = CONTEXT.get();
        if (context != null) {
            context.accept(message);
        } else {
            System.out.println(message);
        }
    }

    @Override
    public void close() {
        MavenSimpleLogger.setLogSink(null);
    }

    public AutoCloseable build(MavenProject project) {
        return new ProjectExecutionContext(project);
    }

    private static class ProjectExecutionContext implements AutoCloseable {
        final MavenProject project;
        final List<String> messages = new CopyOnWriteArrayList<>();
        boolean closed;

        ProjectExecutionContext(MavenProject project) {
            this.project = project;
            CONTEXT.set(this);
        }

        void accept(String message) {
            if (!closed) {
                this.messages.add(message);
            } else {
                System.out.println(message);
            }
        }

        @Override
        public void close() {
            closed = true;
            CONTEXT.set(null);
            this.messages.forEach(System.out::println);
        }
    }
}
