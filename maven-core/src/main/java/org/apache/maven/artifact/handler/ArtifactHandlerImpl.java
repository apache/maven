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
package org.apache.maven.artifact.handler;

import static java.util.Objects.requireNonNull;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Jason van Zyl
 *
 * @since 3.10.0
 */
public final class ArtifactHandlerImpl implements ArtifactHandler {
    public static final String LANGUAGE_NONE = "none";
    public static final String LANGUAGE_JAVA = "java";

    private final String type;
    private final String extension;
    private final String classifier;
    private final String packaging;
    private final boolean includesDependencies;
    private final String language;
    private final boolean addedToClasspath;

    public ArtifactHandlerImpl(
            String type,
            String extension,
            String classifier,
            String packaging,
            boolean includesDependencies,
            String language,
            boolean addedToClasspath) {
        this.type = requireNonNull(type);
        this.extension = extension;
        this.classifier = classifier;
        this.packaging = packaging;
        this.includesDependencies = includesDependencies;
        this.language = language;
        this.addedToClasspath = addedToClasspath;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getExtension() {
        if (extension == null) {
            return getType();
        }
        return extension;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Deprecated
    @Override
    public String getDirectory() {
        return getPackaging() + "s";
    }

    @Override
    public String getPackaging() {
        if (packaging == null) {
            return getType();
        }
        return packaging;
    }

    @Override
    public boolean isIncludesDependencies() {
        return includesDependencies;
    }

    @Override
    public String getLanguage() {
        if (language == null) {
            return LANGUAGE_NONE;
        }
        return language;
    }

    @Override
    public boolean isAddedToClasspath() {
        return addedToClasspath;
    }
}
