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
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Assists in firing execution events. <strong>Warning:</strong> This is an internal utility class that is only public
 * for technical reasons, it is not part of the public API. In particular, this class can be changed or deleted without
 * prior notice.
 *
 * @author Benjamin Bentmann
 */
@Component(role = ExecutionEventCatapult.class)
public class DefaultExecutionEventCatapult implements ExecutionEventCatapult {

    public void fire(ExecutionEvent.Type eventType, MavenSession session, MojoExecution mojoExecution) {
        fire(eventType, session, mojoExecution, null);
    }

    public void fire(
            ExecutionEvent.Type eventType, MavenSession session, MojoExecution mojoExecution, Exception exception) {
        ExecutionListener listener = session.getRequest().getExecutionListener();

        if (listener != null) {
            ExecutionEvent event = new DefaultExecutionEvent(eventType, session, mojoExecution, exception);

            switch (eventType) {
                case ProjectDiscoveryStarted:
                    listener.projectDiscoveryStarted(event);
                    break;

                case SessionStarted:
                    listener.sessionStarted(event);
                    break;
                case SessionEnded:
                    listener.sessionEnded(event);
                    break;

                case ProjectSkipped:
                    listener.projectSkipped(event);
                    break;
                case ProjectStarted:
                    listener.projectStarted(event);
                    break;
                case ProjectSucceeded:
                    listener.projectSucceeded(event);
                    break;
                case ProjectFailed:
                    listener.projectFailed(event);
                    break;

                case MojoSkipped:
                    listener.mojoSkipped(event);
                    break;
                case MojoStarted:
                    listener.mojoStarted(event);
                    break;
                case MojoSucceeded:
                    listener.mojoSucceeded(event);
                    break;
                case MojoFailed:
                    listener.mojoFailed(event);
                    break;

                case ForkStarted:
                    listener.forkStarted(event);
                    break;
                case ForkSucceeded:
                    listener.forkSucceeded(event);
                    break;
                case ForkFailed:
                    listener.forkFailed(event);
                    break;

                case ForkedProjectStarted:
                    listener.forkedProjectStarted(event);
                    break;
                case ForkedProjectSucceeded:
                    listener.forkedProjectSucceeded(event);
                    break;
                case ForkedProjectFailed:
                    listener.forkedProjectFailed(event);
                    break;

                default:
                    throw new IllegalStateException("Unknown execution event type " + eventType);
            }
        }
    }
}
