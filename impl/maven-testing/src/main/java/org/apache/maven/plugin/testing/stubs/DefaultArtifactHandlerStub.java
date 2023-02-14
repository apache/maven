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
package org.apache.maven.plugin.testing.stubs;

import org.apache.maven.artifact.handler.ArtifactHandler;

/**
 * Minimal artifact handler used by the stub factory to create unpackable archives.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class DefaultArtifactHandlerStub implements ArtifactHandler {
    private String extension;

    private String type;

    private String classifier;

    private String directory;

    private String packaging;

    private boolean includesDependencies;

    private String language;

    private boolean addedToClasspath;

    /**
     * @param t the artifact handler type
     * @param c the artifact handler classifier
     */
    public DefaultArtifactHandlerStub(String t, String c) {
        type = t;
        classifier = c;
        if (t.equals("test-jar")) {
            extension = "jar";
        }
    }

    /**
     * @param type the artifact handler type
     */
    public DefaultArtifactHandlerStub(String type) {
        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    public String getExtension() {
        if (extension == null) {
            extension = type;
        }
        return extension;
    }

    /**
     * @return the artifact handler type
     */
    public String getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public String getClassifier() {
        return classifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getDirectory() {
        if (directory == null) {
            directory = getPackaging() + "s";
        }
        return directory;
    }

    /** {@inheritDoc} */
    @Override
    public String getPackaging() {
        if (packaging == null) {
            packaging = getType();
        }
        return packaging;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludesDependencies() {
        return includesDependencies;
    }

    /** {@inheritDoc} */
    @Override
    public String getLanguage() {
        if (language == null) {
            language = "none";
        }

        return language;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAddedToClasspath() {
        return addedToClasspath;
    }

    /**
     * @param theAddedToClasspath The addedToClasspath to set.
     */
    public void setAddedToClasspath(boolean theAddedToClasspath) {
        this.addedToClasspath = theAddedToClasspath;
    }

    /**
     * @param theClassifier The classifier to set.
     */
    public void setClassifier(String theClassifier) {
        this.classifier = theClassifier;
    }

    /**
     * @param theDirectory The directory to set.
     */
    public void setDirectory(String theDirectory) {
        this.directory = theDirectory;
    }

    /**
     * @param theExtension The extension to set.
     */
    public void setExtension(String theExtension) {
        this.extension = theExtension;
    }

    /**
     * @param theIncludesDependencies The includesDependencies to set.
     */
    public void setIncludesDependencies(boolean theIncludesDependencies) {
        this.includesDependencies = theIncludesDependencies;
    }

    /**
     * @param theLanguage The language to set.
     */
    public void setLanguage(String theLanguage) {
        this.language = theLanguage;
    }

    /**
     * @param thePackaging The packaging to set.
     */
    public void setPackaging(String thePackaging) {
        this.packaging = thePackaging;
    }

    /**
     * @param theType The type to set.
     */
    public void setType(String theType) {
        this.type = theType;
    }
}
