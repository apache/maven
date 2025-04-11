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
package org.apache.maven.its.mng8648;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;

public class ProjectEventSpy implements EventSpy {
    private final ConcurrentMap<String, MavenProject> projects = new ConcurrentHashMap<>();

    @Override
    public void init(Context context) {}

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            MavenProject project = executionEvent.getProject();
            switch (executionEvent.getType()) {
                case ProjectStarted:
                    System.out.println(project.getId() + " " + executionEvent.getType());
                    MavenProject existing = projects.put(project.getId(), project);
                    if (existing != null) {
                        throw new IllegalStateException("Project " + project.getId() + " was already started");
                    }
                    break;
                case ProjectSucceeded:
                case ProjectFailed:
                case ProjectSkipped:
                    System.out.println(project.getId() + " " + executionEvent.getType());
                    MavenProject mavenProject = projects.get(project.getId());
                    if (mavenProject == null) {
                        throw new IllegalStateException("Project " + project.getId() + " was never started");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void close() {}
}
