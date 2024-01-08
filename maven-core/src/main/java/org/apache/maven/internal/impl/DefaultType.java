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

import java.util.Map;

import org.apache.maven.api.DependencyProperties;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.eclipse.aether.artifact.ArtifactType;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultType implements Type, ArtifactType {
    private final String extension;

    private final String classifier;

    private final DependencyProperties dependencyProperties;

    public DefaultType(
            String id,
            String language,
            String extension,
            String classifier,
            DependencyProperties dependencyProperties) {
        nonNull(id, "id");
        nonNull(language, "language");
        this.extension = nonNull(extension, "extension");
        this.classifier = classifier;
        nonNull(dependencyProperties, "dependencyProperties");
        DefaultDependencyProperties.Builder props = new DefaultDependencyProperties.Builder()
                .setAll(dependencyProperties)
                .set(DependencyProperties.TYPE, id)
                .set(DependencyProperties.LANGUAGE, language);
        this.dependencyProperties = props.build();
    }

    public DefaultType(String id, String language, String extension, String classifier, PathType... pathTypes) {
        this.extension = nonNull(extension, "extension");
        this.classifier = classifier;
        dependencyProperties = new DefaultDependencyProperties.Builder()
                .set(DependencyProperties.TYPE, id)
                .set(DependencyProperties.LANGUAGE, language)
                .set(DependencyProperties.PATH_TYPES, pathTypes)
                .build();
    }

    @Override
    public String getId() {
        return dependencyProperties.get(DependencyProperties.TYPE);
    }

    @Override
    public String getLanguage() {
        return dependencyProperties.get(DependencyProperties.LANGUAGE);
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
    public DependencyProperties getDependencyProperties() {
        return dependencyProperties;
    }

    @Override
    public Map<String, String> getProperties() {
        return getDependencyProperties().asMap();
    }
}
