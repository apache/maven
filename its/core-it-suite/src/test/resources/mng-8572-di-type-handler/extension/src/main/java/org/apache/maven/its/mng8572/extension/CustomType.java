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
package org.apache.maven.its.mng8572.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;

/**
 * Implementation of a custom artifact type.
 */
public class CustomType implements Type {

    private final String id;
    private final Language language;
    private final String extension;
    private final String classifier;
    private final boolean includesDependencies;
    private final Set<PathType> pathTypes;

    public CustomType(
            String id,
            Language language,
            String extension,
            String classifier,
            boolean includesDependencies,
            PathType... pathTypes) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.language = Objects.requireNonNull(language, "language cannot be null");
        this.extension = Objects.requireNonNull(extension, "extension cannot be null");
        this.classifier = classifier;
        this.includesDependencies = includesDependencies;
        this.pathTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(pathTypes)));
    }

    @Override
    public String id() {
        return id;
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
        return includesDependencies;
    }

    @Override
    public Set<PathType> getPathTypes() {
        return pathTypes;
    }
}
