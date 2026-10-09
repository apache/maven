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
package org.apache.maven.lifecycle.internal.filter;

import org.apache.maven.api.MojoExecution;

/**
 * A {@link FilterPredicate} that matches all {@link MojoExecution}s bound to a specific lifecycle phase.
 *
 * <p>Syntax in {@code -Dmaven.lifecycle.filter}: {@code phase(<phaseName>)}
 *
 * <p>Example: {@code -Dmaven.lifecycle.filter=phase(test)} removes all mojos bound to the {@code test} phase.
 *
 * @since 4.1.0
 */
public class PhasePredicate implements FilterPredicate {

    private final String phaseName;

    public PhasePredicate(String phaseName) {
        this.phaseName = phaseName;
    }

    @Override
    public boolean matches(MojoExecution execution) {
        return phaseName.equals(execution.getLifecyclePhase());
    }

    @Override
    public String toString() {
        return "phase(" + phaseName + ")";
    }
}
