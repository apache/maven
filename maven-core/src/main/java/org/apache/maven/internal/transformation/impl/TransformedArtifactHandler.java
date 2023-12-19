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
package org.apache.maven.internal.transformation.impl;

import org.apache.maven.artifact.handler.ArtifactHandler;

import static java.util.Objects.requireNonNull;

class TransformedArtifactHandler implements ArtifactHandler {

    private final String classifier;

    private final String extension;

    private final String packaging;

    TransformedArtifactHandler(String classifier, String extension, String packaging) {
        this.classifier = classifier;
        this.extension = requireNonNull(extension);
        this.packaging = requireNonNull(packaging);
    }

    public String getClassifier() {
        return classifier;
    }

    public String getDirectory() {
        return null;
    }

    public String getExtension() {
        return extension;
    }

    public String getLanguage() {
        return "none";
    }

    public String getPackaging() {
        return packaging;
    }

    public boolean isAddedToClasspath() {
        return false;
    }

    public boolean isIncludesDependencies() {
        return false;
    }
}
