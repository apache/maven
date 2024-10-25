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

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Represents a parsed phase identifier.
 */
public class PhaseId {
    /**
     * Interned {@link PhaseId} instances.
     */
    private static final Map<String, PhaseId> INSTANCES = new WeakHashMap<>();

    /**
     * The execution point of this {@link PhaseId}.
     */
    private final PhaseExecutionPoint executionPoint;

    /**
     * The static phase that this dynamic phase belongs to.
     */
    private final String phase;

    /**
     * The priority of this dynamic phase within the static phase.
     */
    private final int priority;

    /**
     * Parses the phase identifier.
     *
     * @param phase the phase identifier.
     * @return the {@link PhaseId}.
     */
    public static synchronized PhaseId of(String phase) {
        return INSTANCES.computeIfAbsent(phase, PhaseId::new);
    }

    /**
     * Constructor.
     *
     * @param phase the phase identifier string.
     */
    private PhaseId(String phase) {
        int phaseStart;
        if (phase.startsWith(PhaseExecutionPoint.BEFORE.prefix())) {
            executionPoint = PhaseExecutionPoint.BEFORE;
            phaseStart = PhaseExecutionPoint.BEFORE.prefix().length();
        } else if (phase.startsWith(PhaseExecutionPoint.AFTER.prefix())) {
            executionPoint = PhaseExecutionPoint.AFTER;
            phaseStart = PhaseExecutionPoint.AFTER.prefix().length();
        } else {
            executionPoint = PhaseExecutionPoint.AT;
            phaseStart = 0;
        }
        int phaseEnd = phase.indexOf('[');
        if (phaseEnd == -1) {
            priority = 0;
            this.phase = phase.substring(phaseStart);
        } else {
            int priorityEnd = phase.lastIndexOf(']');
            boolean hasPriority;
            int priority;
            if (priorityEnd < phaseEnd + 1) {
                priority = 0;
                hasPriority = false;
            } else {
                try {
                    priority = Integer.parseInt(phase.substring(phaseEnd + 1, priorityEnd));
                    hasPriority = true;
                } catch (NumberFormatException e) {
                    // priority must be an integer
                    priority = 0;
                    hasPriority = false;
                }
            }
            if (hasPriority) {
                this.phase = phase.substring(phaseStart, phaseEnd);
                this.priority = priority;
            } else {
                this.phase = phase.substring(phaseStart);
                this.priority = 0;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            PhaseId phaseId = (PhaseId) o;
            return Objects.equals(executionPoint(), phaseId.executionPoint())
                    && Objects.equals(phase(), phaseId.phase())
                    && Objects.equals(priority(), phaseId.priority());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionPoint(), phase(), priority());
    }

    @Override
    public String toString() {
        return executionPoint().prefix() + phase() + (priority() != 0 ? "[" + priority() + ']' : "");
    }

    public PhaseExecutionPoint executionPoint() {
        return executionPoint;
    }

    public String phase() {
        return phase;
    }

    public int priority() {
        return priority;
    }
}
