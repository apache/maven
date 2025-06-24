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
package org.apache.maven.repository.internal;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;

/**
 * A utility class to assist in setting up a Maven-like repository system. <em>Note:</em> This component is meant to
 * assist those clients that employ the repository system outside of an IoC container, Maven plugins should instead
 * always use regular dependency injection to acquire the repository system.
 *
 * @deprecated See {@link MavenSessionBuilderSupplier}
 */
@Deprecated
public final class MavenRepositorySystemUtils {

    private MavenRepositorySystemUtils() {
        // hide constructor
    }

    /**
     * This method is deprecated, nobody should use it.
     *
     * @deprecated This method is here only for legacy uses (like UTs), nothing else should use it.
     */
    @Deprecated
    public static DefaultRepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false); // no close handle
        MavenSessionBuilderSupplier builder = new MavenSessionBuilderSupplier();
        session.setDependencyTraverser(builder.getDependencyTraverser());
        session.setDependencyManager(new ClassicDependencyManager()); // Maven 3 behavior
        session.setDependencySelector(builder.getDependencySelector());
        session.setDependencyGraphTransformer(builder.getDependencyGraphTransformer());
        session.setArtifactTypeRegistry(builder.getArtifactTypeRegistry());
        session.setArtifactDescriptorPolicy(builder.getArtifactDescriptorPolicy());
        return session;
    }
}
