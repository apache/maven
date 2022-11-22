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
 */
public class DefaultArtifactHandler implements ArtifactHandler {
    private final String type;

    private String extension;

    private String classifier;

    private String directory;

    private String packaging;

    private boolean includesDependencies;

    private String language;

    private boolean addedToClasspath;

    /**
     * Default ctor for Plexus compatibility, as many plugins have artifact handlers declared in legacy Plexus XML.
     * Do not use directly!
     *
     * @deprecated This ctor is present only for Plexus XML defined component compatibility, do not use it.
     */
    @Deprecated
    public DefaultArtifactHandler() {
        this.type = null;
    }

    public DefaultArtifactHandler(final String type) {
        this(type, null, null, null, null, false, null, false);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultArtifactHandler(
            final String type,
            final String extension,
            final String classifier,
            final String directory,
            final String packaging,
            final boolean includesDependencies,
            final String language,
            final boolean addedToClasspath) {
        this.type = requireNonNull(type);
        this.extension = extension;
        this.classifier = classifier;
        this.directory = directory;
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
            return type;
        }
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(final String classifier) {
        this.classifier = classifier;
    }

    @Override
    public String getDirectory() {
        if (directory == null) {
            return getPackaging() + "s";
        }
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    @Override
    public String getPackaging() {
        if (packaging == null) {
            return type;
        }
        return packaging;
    }

    public void setPackaging(final String packaging) {
        this.packaging = packaging;
    }

    @Override
    public boolean isIncludesDependencies() {
        return includesDependencies;
    }

    public void setIncludesDependencies(final boolean includesDependencies) {
        this.includesDependencies = includesDependencies;
    }

    @Override
    public String getLanguage() {
        if (language == null) {
            return "none";
        }

        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    @Override
    public boolean isAddedToClasspath() {
        return addedToClasspath;
    }

    public void setAddedToClasspath(final boolean addedToClasspath) {
        this.addedToClasspath = addedToClasspath;
    }
}
