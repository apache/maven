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
package org.apache.maven.lifecycle;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;

@Named
@Singleton
public class DelegatingProjectExecutionListener implements ProjectExecutionListener {
    private final List<ProjectExecutionListener> listeners = new CopyOnWriteArrayList<>();

    public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : listeners) {
            listener.beforeProjectExecution(event);
        }
    }

    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : listeners) {
            listener.beforeProjectLifecycleExecution(event);
        }
    }

    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : listeners) {
            listener.afterProjectExecutionSuccess(event);
        }
    }

    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
        for (ProjectExecutionListener listener : listeners) {
            listener.afterProjectExecutionFailure(event);
        }
    }

    public void addProjectExecutionListener(ProjectExecutionListener listener) {
        this.listeners.add(listener);
    }

    public void removeProjectExecutionListener(ProjectExecutionListener listener) {
        this.listeners.remove(listener);
    }
}
