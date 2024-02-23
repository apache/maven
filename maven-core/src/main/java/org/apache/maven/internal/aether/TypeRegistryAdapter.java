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
package org.apache.maven.internal.aether;

import org.apache.maven.api.Type;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.repository.internal.type.DefaultType;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;

import static java.util.Objects.requireNonNull;

class TypeRegistryAdapter implements ArtifactTypeRegistry {
    private final TypeRegistry typeRegistry;

    TypeRegistryAdapter(TypeRegistry typeRegistry) {
        this.typeRegistry = requireNonNull(typeRegistry, "typeRegistry");
    }

    @Override
    public ArtifactType get(String typeId) {
        Type type = typeRegistry.require(typeId);
        if (type instanceof ArtifactType) {
            return (ArtifactType) type;
        }
        if (type != null) {
            return new DefaultType(
                    type.id(),
                    type.getLanguage(),
                    type.getExtension(),
                    type.getClassifier(),
                    type.isBuildPathConstituent(),
                    type.isIncludesDependencies());
        }
        return null;
    }
}
