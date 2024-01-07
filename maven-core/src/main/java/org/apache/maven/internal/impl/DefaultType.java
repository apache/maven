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
package org.apache.maven.internal.impl;

import java.util.*;
import java.util.Map;

import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultType implements Type, ArtifactType {
    private final String id;

    private final Language language;

    private final String extension;

    private final String classifier;
    private final boolean includesDependencies;
    private final Set<PathType> pathTypes;

    public DefaultType(
            String id,
            Language language,
            String extension,
            String classifier,
            boolean includesDependencies,
            PathType... pathTypes) {
        this.id = nonNull(id, "id");
        this.language = nonNull(language, "language");
        this.extension = nonNull(extension, "extension");
        this.classifier = classifier;
        this.includesDependencies = includesDependencies;
        this.pathTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(pathTypes)));
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
        Map<String, String> properties = new HashMap<>();
        properties.put(ArtifactProperties.TYPE, this.id);
        properties.put(ArtifactProperties.LANGUAGE, this.language.id());
        properties.put(ArtifactProperties.INCLUDES_DEPENDENCIES, String.valueOf(includesDependencies));
        properties.put(
                ArtifactProperties.CONSTITUTES_BUILD_PATH, String.valueOf(pathTypes.contains(JavaPathType.CLASSES)));
        return Collections.unmodifiableMap(properties);
    }
}
