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
package org.apache.maven.repository.internal.artifact;

/**
 * The keys for Maven specific properties of artifacts. These properties "extend" (or supplement) the Resolver
 * core properties defined in {@link org.eclipse.aether.artifact.ArtifactProperties}.
 *
 * @see org.eclipse.aether.artifact.ArtifactProperties
 * @since 4.0.0
 */
public final class MavenArtifactProperties {
    /**
     * A boolean flag indicating whether the artifact presents some kind of bundle that physically includes its
     * dependencies, e.g. a fat WAR.
     */
    public static final String INCLUDES_DEPENDENCIES = "includesDependencies";

    /**
     * A boolean flag indicating whether the artifact is meant to be used for the compile/runtime/test build path of a
     * consumer project.
     * <p>
     * Note: This property is about "build path", whatever it means in the scope of the consumer project. It is NOT
     * about Java classpath or anything alike. How artifact is being consumed depends heavily on the consumer project.
     * Resolver is and will remain agnostic of consumer project use cases.
     */
    public static final String CONSTITUTES_BUILD_PATH = "constitutesBuildPath";

    private MavenArtifactProperties() {
        // hide constructor
    }
}
