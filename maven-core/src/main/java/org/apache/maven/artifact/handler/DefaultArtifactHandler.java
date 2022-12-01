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

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Jason van Zyl
 */
@Component(role = ArtifactHandler.class)
public class DefaultArtifactHandler implements ArtifactHandler {
    private String extension;

    private String type;

    private String classifier;

    private String directory;

    private String packaging;

    private boolean includesDependencies;

    private String language;

    private boolean addedToClasspath;

    public DefaultArtifactHandler() {}

    public DefaultArtifactHandler(String type) {
        this.type = type;
    }

    public String getExtension() {
        if (extension == null) {
            extension = type;
        }
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getDirectory() {
        if (directory == null) {
            directory = getPackaging() + "s";
        }
        return directory;
    }

    public String getPackaging() {
        if (packaging == null) {
            packaging = type;
        }
        return packaging;
    }

    public boolean isIncludesDependencies() {
        return includesDependencies;
    }

    public void setIncludesDependencies(boolean includesDependencies) {
        this.includesDependencies = includesDependencies;
    }

    public String getLanguage() {
        if (language == null) {
            language = "none";
        }

        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isAddedToClasspath() {
        return addedToClasspath;
    }

    public void setAddedToClasspath(boolean addedToClasspath) {
        this.addedToClasspath = addedToClasspath;
    }
}
