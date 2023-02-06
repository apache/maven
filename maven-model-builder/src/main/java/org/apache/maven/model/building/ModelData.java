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
package org.apache.maven.model.building;

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

/**
 * Holds a model along with some auxiliary information. This internal utility class assists the model builder during POM
 * processing by providing a means to transport information that cannot be (easily) extracted from the model itself.
 *
 * @author Benjamin Bentmann
 */
class ModelData {
    private final ModelSource source;

    private Model model;

    private Model rawModel;

    private List<Profile> activeProfiles;

    private String groupId;

    private String artifactId;

    private String version;

    /**
     * Creates a new container for the specified model.
     *
     * @param model The model to wrap, may be {@code null}.
     */
    ModelData(ModelSource source, Model model) {
        this.source = source;
        this.model = model;
    }

    /**
     * Creates a new container for the specified model.
     *
     * @param model The model to wrap, may be {@code null}.
     * @param groupId The effective group identifier of the model, may be {@code null}.
     * @param artifactId The effective artifact identifier of the model, may be {@code null}.
     * @param version The effective version of the model, may be {@code null}.
     */
    ModelData(ModelSource source, Model model, String groupId, String artifactId, String version) {
        this.source = source;
        this.model = model;
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
    }

    public ModelSource getSource() {
        return source;
    }

    /**
     * Gets the model being wrapped.
     *
     * @return The model or {@code null} if not set.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Sets the model being wrapped.
     *
     * @param model The model, may be {@code null}.
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * Gets the raw model being wrapped.
     *
     * @return The raw model or {@code null} if not set.
     */
    public Model getRawModel() {
        return rawModel;
    }

    /**
     * Sets the raw model being wrapped.
     *
     * @param rawModel The raw model, may be {@code null}.
     */
    public void setRawModel(Model rawModel) {
        this.rawModel = rawModel;
    }

    /**
     * Gets the active profiles from the model.
     *
     * @return The active profiles or {@code null} if not set.
     */
    public List<Profile> getActiveProfiles() {
        return activeProfiles;
    }

    /**
     * Sets the active profiles from the model.
     *
     * @param activeProfiles The active profiles, may be {@code null}.
     */
    public void setActiveProfiles(List<Profile> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    /**
     * Gets the effective group identifier of the model.
     *
     * @return The effective group identifier of the model or an empty string if unknown, never {@code null}.
     */
    public String getGroupId() {
        return (groupId != null) ? groupId : "";
    }

    /**
     * Sets the effective group identifier of the model.
     *
     * @param groupId The effective group identifier of the model, may be {@code null}.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the effective artifact identifier of the model.
     *
     * @return The effective artifact identifier of the model or an empty string if unknown, never {@code null}.
     */
    public String getArtifactId() {
        return (artifactId != null) ? artifactId : "";
    }

    /**
     * Sets the effective artifact identifier of the model.
     *
     * @param artifactId The effective artifact identifier of the model, may be {@code null}.
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Gets the effective version of the model.
     *
     * @return The effective version of the model or an empty string if unknown, never {@code null}.
     */
    public String getVersion() {
        return (version != null) ? version : "";
    }

    /**
     * Sets the effective version of the model.
     *
     * @param version The effective version of the model, may be {@code null}.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the effective identifier of the model in the form {@code <groupId>:<artifactId>:<version>}.
     *
     * @return The effective identifier of the model, never {@code null}.
     */
    public String getId() {
        StringBuilder buffer = new StringBuilder(128);

        buffer.append(getGroupId())
                .append(':')
                .append(getArtifactId())
                .append(':')
                .append(getVersion());

        return buffer.toString();
    }

    @Override
    public String toString() {
        return String.valueOf(model);
    }
}
