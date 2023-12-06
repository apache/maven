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

import java.util.Collection;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;

class CompoundProjectExecutionListener implements ProjectExecutionListener {
    private final Collection<ProjectExecutionListener> listeners;

    CompoundProjectExecutionListener(Collection<ProjectExecutionListener> listeners) {
        this.listeners = listeners; // NB this is live injected collection
    }

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
}
