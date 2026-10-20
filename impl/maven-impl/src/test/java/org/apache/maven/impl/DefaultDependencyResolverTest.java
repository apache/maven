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
package org.apache.maven.impl;

import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class DefaultDependencyResolverTest {

    @Test
    void appliesProjectManagementToPluginDependency() {
        Dependency dependency =
                new Dependency(new DefaultArtifact("org.example", "processor", "", "jar", ""), "", false);
        Exclusion exclusion = new Exclusion("org.example", "excluded", "*", "*");
        Dependency managed = new Dependency(
                new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime", false, List.of(exclusion));

        Dependency result = DefaultDependencyResolver.applyDependencyManagement(List.of(dependency), List.of(managed))
                .get(0);

        assertEquals("1.2.3", result.getArtifact().getVersion());
        assertEquals("runtime", result.getScope());
        assertIterableEquals(List.of(exclusion), result.getExclusions());
    }

    @Test
    void keepsValuesDeclaredByPluginDependency() {
        Exclusion declaredExclusion = new Exclusion("org.example", "declared", "*", "*");
        Dependency dependency = new Dependency(
                new DefaultArtifact("org.example:processor:jar:2.0.0"), "test", false, List.of(declaredExclusion));
        Exclusion managedExclusion = new Exclusion("org.example", "managed", "*", "*");
        Dependency managed = new Dependency(
                new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime", false, List.of(managedExclusion));

        Dependency result = DefaultDependencyResolver.applyDependencyManagement(List.of(dependency), List.of(managed))
                .get(0);

        assertEquals("2.0.0", result.getArtifact().getVersion());
        assertEquals("test", result.getScope());
        assertIterableEquals(List.of(declaredExclusion), result.getExclusions());
    }
}
