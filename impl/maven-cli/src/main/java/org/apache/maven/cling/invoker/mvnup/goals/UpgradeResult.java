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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Result of an upgrade strategy application.
 * Uses sets of paths to track which POMs were processed, modified, or had errors,
 * avoiding double-counting when multiple strategies affect the same POMs.
 *
 * @param processedPoms the set of POMs that were processed
 * @param modifiedPoms the set of POMs that were modified
 * @param errorPoms the set of POMs that had errors
 */
public record UpgradeResult(Set<Path> processedPoms, Set<Path> modifiedPoms, Set<Path> errorPoms) {

    public UpgradeResult {
        // Defensive copying to ensure immutability
        processedPoms = Set.copyOf(processedPoms);
        modifiedPoms = Set.copyOf(modifiedPoms);
        errorPoms = Set.copyOf(errorPoms);
    }

    /**
     * Creates a successful result with the specified processed and modified POMs.
     */
    public static UpgradeResult success(Set<Path> processedPoms, Set<Path> modifiedPoms) {
        return new UpgradeResult(processedPoms, modifiedPoms, Collections.emptySet());
    }

    /**
     * Creates a failure result with the specified processed POMs and error POMs.
     */
    public static UpgradeResult failure(Set<Path> processedPoms, Set<Path> errorPoms) {
        return new UpgradeResult(processedPoms, Collections.emptySet(), errorPoms);
    }

    /**
     * Creates an empty result (no POMs processed).
     */
    public static UpgradeResult empty() {
        return new UpgradeResult(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Merges this result with another result, combining the sets of POMs.
     * This allows proper aggregation of results from multiple strategies without double-counting.
     */
    public UpgradeResult merge(UpgradeResult other) {
        Set<Path> mergedProcessed = new HashSet<>(this.processedPoms);
        mergedProcessed.addAll(other.processedPoms);

        Set<Path> mergedModified = new HashSet<>(this.modifiedPoms);
        mergedModified.addAll(other.modifiedPoms);

        Set<Path> mergedErrors = new HashSet<>(this.errorPoms);
        mergedErrors.addAll(other.errorPoms);

        return new UpgradeResult(mergedProcessed, mergedModified, mergedErrors);
    }

    /**
     * Returns true if no errors occurred.
     */
    public boolean success() {
        return errorPoms.isEmpty();
    }

    /**
     * Returns the number of POMs processed.
     */
    public int processedCount() {
        return processedPoms.size();
    }

    /**
     * Returns the number of POMs modified.
     */
    public int modifiedCount() {
        return modifiedPoms.size();
    }

    /**
     * Returns the number of POMs that had errors.
     */
    public int errorCount() {
        return errorPoms.size();
    }

    /**
     * Returns the number of POMs that were processed but not modified and had no errors.
     */
    public int unmodifiedCount() {
        Set<Path> unmodified = new HashSet<>(processedPoms);
        unmodified.removeAll(modifiedPoms);
        unmodified.removeAll(errorPoms);
        return unmodified.size();
    }
}
