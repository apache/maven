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
package org.apache.maven.project;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.api.model.Source;

/**
 * Static utility methods for analyzing {@code <source>} elements of a project.
 * <p>
 * <strong>Warning:</strong> This is an internal utility class, not part of the public API.
 * It can be changed or removed without prior notice.
 *
 * @since 4.0.0
 */
public final class SourceQueries {
    private SourceQueries() {}

    /**
     * Returns whether at least one source in the collection has a non-blank module name,
     * indicating a modular source hierarchy.
     *
     * @param sources the source elements to check
     * @return {@code true} if at least one source declares a module
     */
    public static boolean usesModuleSourceHierarchy(Collection<Source> sources) {
        return sources.stream().map(Source::getModule).filter(Objects::nonNull).anyMatch(s -> !s.isBlank());
    }

    /**
     * Returns whether at least one source in the collection is enabled.
     *
     * @param sources the source elements to check
     * @return {@code true} if at least one source is enabled
     */
    public static boolean hasEnabledSources(Collection<Source> sources) {
        for (Source source : sources) {
            if (source.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts unique, non-blank module names from the source elements, preserving declaration order.
     * The following relationship should always be true:
     *
     * <pre>getModuleNames(sources).isEmpty() == !usesModuleSourceHierarchy(sources)</pre>
     *
     * @param sources the source elements to extract module names from
     * @return set of non-blank module names in declaration order
     */
    public static Set<String> getModuleNames(Collection<Source> sources) {
        var modules = new LinkedHashSet<String>();
        sources.stream()
                .map(Source::getModule)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .forEach(modules::add);
        return modules;
    }
}
