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
package org.apache.maven.eventspy.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionListener;
import org.eclipse.aether.RepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches callbacks to all registered EventSpies.
 * @since 3.0.2
 */
@Named
@Singleton
public class EventSpyDispatcher {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<EventSpy> eventSpies;

    @Inject
    public EventSpyDispatcher(List<EventSpy> eventSpies) {
        // make copy to get rid of needless overhead for dynamic lookups
        this.eventSpies = new ArrayList<>(eventSpies);
    }

    public ExecutionListener chainListener(ExecutionListener listener) {
        if (eventSpies.isEmpty()) {
            return listener;
        }
        return new EventSpyExecutionListener(this, listener);
    }

    public RepositoryListener chainListener(RepositoryListener listener) {
        if (eventSpies.isEmpty()) {
            return listener;
        }
        return new EventSpyRepositoryListener(this, listener);
    }

    public void init(EventSpy.Context context) {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.init(context);
            } catch (Exception | LinkageError e) {
                logError("initialize", e, eventSpy);
            }
        }
    }

    public void onEvent(Object event) {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.onEvent(event);
            } catch (Exception | LinkageError e) {
                logError("notify", e, eventSpy);
            }
        }
    }

    public void close() {
        if (eventSpies.isEmpty()) {
            return;
        }
        for (EventSpy eventSpy : eventSpies) {
            try {
                eventSpy.close();
            } catch (Exception | LinkageError e) {
                logError("close", e, eventSpy);
            }
        }
    }

    private void logError(String action, Throwable e, EventSpy spy) {
        String msg = "Failed to " + action + " spy " + spy.getClass().getName() + ": " + e.getMessage();

        if (logger.isDebugEnabled()) {
            logger.warn(msg, e);
        } else {
            logger.warn(msg);
        }
    }
}
