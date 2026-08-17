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
package org.apache.maven.impl.resolver;

import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.services.ModelBuilder;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultArtifactDescriptorReaderTest {

    @Test
    void testRemapCompileToApi() throws Exception {
        // Create an instance of DefaultArtifactDescriptorReader
        DefaultArtifactDescriptorReader reader = new DefaultArtifactDescriptorReader(
                Mockito.mock(VersionResolver.class),
                Mockito.mock(ArtifactResolver.class),
                Mockito.mock(ModelBuilder.class),
                Mockito.mock(RepositoryEventDispatcher.class),
                Collections.emptyMap());

        // Get the private convert() method via reflection
        Method convertMethod = DefaultArtifactDescriptorReader.class.getDeclaredMethod(
                "convert", Dependency.class, ArtifactTypeRegistry.class, boolean.class);
        convertMethod.setAccessible(true);

        // Mock ArtifactTypeRegistry
        ArtifactTypeRegistry stereotypes = new ArtifactTypeRegistry() {
            @Override
            public ArtifactType get(String typeId) {
                return new DefaultArtifactType(typeId, "jar", "", "java");
            }
        };

        // 1. Test remapCompileToApi = true, scope = compile -> api
        Dependency depCompile = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .type("jar")
                .scope("compile")
                .build();
        org.eclipse.aether.graph.Dependency aetherDep1 =
                (org.eclipse.aether.graph.Dependency) convertMethod.invoke(reader, depCompile, stereotypes, true);
        assertEquals("api", aetherDep1.getScope(), "Compile scope should be remapped to api when remap=true");

        // 2. Test remapCompileToApi = true, scope = "" -> api
        Dependency depEmpty = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .type("jar")
                .scope("")
                .build();
        org.eclipse.aether.graph.Dependency aetherDep2 =
                (org.eclipse.aether.graph.Dependency) convertMethod.invoke(reader, depEmpty, stereotypes, true);
        assertEquals("api", aetherDep2.getScope(), "Empty scope should be remapped to api when remap=true");

        // 3. Test remapCompileToApi = false, scope = compile -> compile
        org.eclipse.aether.graph.Dependency aetherDep3 =
                (org.eclipse.aether.graph.Dependency) convertMethod.invoke(reader, depCompile, stereotypes, false);
        assertEquals("compile", aetherDep3.getScope(), "Compile scope should remain compile when remap=false");

        // 4. Test remapCompileToApi = false, scope = "" -> ""
        org.eclipse.aether.graph.Dependency aetherDep4 =
                (org.eclipse.aether.graph.Dependency) convertMethod.invoke(reader, depEmpty, stereotypes, false);
        assertEquals("", aetherDep4.getScope(), "Empty scope should remain empty when remap=false");
    }
}
