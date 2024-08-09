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

/**
 * Represents where a dynamic phase should be executed within a static phase.
 */
public enum PhaseExecutionPoint {
    /**
     * Execution must occur before any executions of the phase proper. Failure of any {@code #BEFORE} dynamic phase
     * execution will prevent the {@link #AT} phase but will not prevent any {@link #AFTER} dynamic phases.
     */
    BEFORE("before:"),
    /**
     * Execution is the execution of the phase proper. Failure of any {@code #AT} dynamic phase execution will fail
     * the phase. Any {@link #AFTER} phases will still be executed.
     */
    AT(""),
    /**
     * Guaranteed execution dynamic phases on completion of the static phase. All {@code #AFTER} dynamic phases will
     * be executed provided at least one {@link #BEFORE} or {@link #AT} dynamic phase has started execution.
     */
    AFTER("after:");

    private final String prefix;

    PhaseExecutionPoint(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
