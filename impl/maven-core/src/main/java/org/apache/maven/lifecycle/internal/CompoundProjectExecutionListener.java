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
import java.util.Iterator;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates multiple {@link ProjectExecutionListener} instances and delegates to each in turn.
 *
 * <p>The backing collection is typically a Sisu live-injected list that dynamically includes
 * all {@code ProjectExecutionListener} implementations discovered across all classrealms.
 * When a plugin that is <b>not</b> configured as an extension ships JSR 330 components that
 * implement this interface, Sisu will discover and include them even though the plugin never
 * opted in via {@code <extensions>true</extensions>}. Lazy provisioning of such components
 * may fail because their plugin-internal dependencies are not bound in Maven's container.
 * This class therefore catches provisioning failures during iteration and logs them at debug
 * level instead of aborting the build.</p>
 *
 * @see <a href="https://github.com/apache/maven/issues/12522">GH-12522</a>
 */
public class CompoundProjectExecutionListener implements ProjectExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundProjectExecutionListener.class);

    private final Collection<ProjectExecutionListener> listeners;

    public CompoundProjectExecutionListener(Collection<ProjectExecutionListener> listeners) {
        this.listeners = listeners; // NB this is live injected collection
    }

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : safeListeners()) {
            listener.beforeProjectExecution(event);
        }
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : safeListeners()) {
            listener.beforeProjectLifecycleExecution(event);
        }
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {
        for (ProjectExecutionListener listener : safeListeners()) {
            listener.afterProjectExecutionSuccess(event);
        }
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
        for (ProjectExecutionListener listener : safeListeners()) {
            listener.afterProjectExecutionFailure(event);
        }
    }

    /**
     * Returns the listeners that can actually be provisioned, silently skipping any that
     * fail during lazy creation (typically components from non-extension plugins whose
     * internal dependencies are not available in Maven's container).
     */
    private Iterable<ProjectExecutionListener> safeListeners() {
        return () -> new Iterator<>() {
            private final Iterator<ProjectExecutionListener> delegate = listeners.iterator();
            private ProjectExecutionListener next;

            @Override
            public boolean hasNext() {
                while (next == null && delegate.hasNext()) {
                    try {
                        next = delegate.next();
                    } catch (RuntimeException e) {
                        // Sisu wraps provisioning failures in RuntimeException (ProvisionException).
                        // This happens when a non-extension plugin ships JSR 330 components whose
                        // dependencies cannot be resolved in Maven's container.
                        LOGGER.debug(
                                "Skipping ProjectExecutionListener that could not be provisioned"
                                        + " (likely from a non-extension plugin): {}",
                                e.getMessage());
                    }
                }
                return next != null;
            }

            @Override
            public ProjectExecutionListener next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                ProjectExecutionListener result = next;
                next = null;
                return result;
            }
        };
    }
}
