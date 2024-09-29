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
package org.apache.maven.repository.internal.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.apache.maven.repository.internal.artifact.MavenArtifactProperties;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation of {@link Type} and Resolver {@link ArtifactType}.
 *
 * @since 4.0.0
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 */
@Deprecated(since = "4.0.0")
public class DefaultType implements Type, ArtifactType {
    private final String id;
    private final Language language;
    private final String extension;
    private final String classifier;
    private final boolean includesDependencies;
    private final Set<PathType> pathTypes;
    private final Map<String, String> properties;

    public DefaultType(
            String id,
            Language language,
            String extension,
            String classifier,
            boolean includesDependencies,
            PathType... pathTypes) {
        this.id = requireNonNull(id, "id");
        this.language = requireNonNull(language, "language");
        this.extension = requireNonNull(extension, "extension");
        this.classifier = classifier;
        this.includesDependencies = includesDependencies;
        this.pathTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(pathTypes)));

        Map<String, String> properties = new HashMap<>();
        properties.put(ArtifactProperties.TYPE, id);
        properties.put(ArtifactProperties.LANGUAGE, language.id());
        properties.put(MavenArtifactProperties.INCLUDES_DEPENDENCIES, Boolean.toString(includesDependencies));
        properties.put(
                MavenArtifactProperties.CONSTITUTES_BUILD_PATH,
                String.valueOf(this.pathTypes.contains(JavaPathType.CLASSES)));
        this.properties = Collections.unmodifiableMap(properties);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String getId() {
        return id();
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public boolean isIncludesDependencies() {
        return this.includesDependencies;
    }

    public Set<PathType> getPathTypes() {
        return this.pathTypes;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}
