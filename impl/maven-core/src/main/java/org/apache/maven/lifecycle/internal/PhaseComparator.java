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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares phases within the context of a specific lifecycle with secondary sorting based on the {@link PhaseId}.
 */
public class PhaseComparator implements Comparator<String> {
    /**
     * Map from phase name to its index in the lifecycle, enabling O(1) lookups
     * instead of O(n) List.indexOf() scans on every comparison.
     */
    private final Map<String, Integer> phaseIndexMap;

    /**
     * Constructor.
     *
     * @param lifecyclePhases the lifecycle phase ordering.
     */
    public PhaseComparator(List<String> lifecyclePhases) {
        this.phaseIndexMap = new HashMap<>(lifecyclePhases.size() * 2);
        for (int i = 0; i < lifecyclePhases.size(); i++) {
            phaseIndexMap.put(lifecyclePhases.get(i), i);
        }
    }

    @Override
    public int compare(String o1, String o2) {
        PhaseId p1 = PhaseId.of(o1);
        PhaseId p2 = PhaseId.of(o2);
        Integer i1 = phaseIndexMap.get(p1.executionPoint().prefix() + p1.phase());
        Integer i2 = phaseIndexMap.get(p2.executionPoint().prefix() + p2.phase());
        if (i1 == null && i2 == null) {
            // unknown phases, leave in existing order
            return 0;
        }
        if (i1 == null) {
            // second one is known, so it comes first
            return 1;
        }
        if (i2 == null) {
            // first one is known, so it comes first
            return -1;
        }
        int rv = Integer.compare(i1, i2);
        if (rv != 0) {
            return rv;
        }
        // same execution point, now compare priorities
        return Integer.compare(p1.priority(), p2.priority());
    }
}
