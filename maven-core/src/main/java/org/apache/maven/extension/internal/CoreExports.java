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
package org.apache.maven.extension.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * Provides information about artifacts (identified by groupId:artifactId string key) and classpath elements exported by
 * Maven core itself and loaded Maven core extensions.
 *
 * @since 3.3.0
 */
public class CoreExports {
    private final Set<String> artifacts;

    private final Map<String, ClassLoader> packages;

    public CoreExports(CoreExtensionEntry entry) {
        this(entry.getClassRealm(), entry.getExportedArtifacts(), entry.getExportedPackages());
    }

    public CoreExports(ClassRealm realm, Set<String> exportedArtifacts, Set<String> exportedPackages) {
        this.artifacts = Collections.unmodifiableSet(new HashSet<>(exportedArtifacts));
        this.packages = exportedPackages.stream()
                .collect(collectingAndThen(toMap(identity(), v -> realm), Collections::unmodifiableMap));
    }

    /**
     * Returns artifacts exported by Maven core and core extensions. Artifacts are identified by their
     * groupId:artifactId string key.
     */
    public Set<String> getExportedArtifacts() {
        return artifacts;
    }

    /**
     * Returns packages exported by Maven core and core extensions.
     */
    public Map<String, ClassLoader> getExportedPackages() {
        return packages;
    }
}
