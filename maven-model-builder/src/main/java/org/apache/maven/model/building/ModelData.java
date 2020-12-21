package org.apache.maven.model.building;

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

import java.util.Objects;

import org.apache.maven.building.Source;
import org.apache.maven.model.Model;

/**
 * Holds a model along with some auxiliary information. This internal utility class assists the model builder during POM
 * processing by providing a means to transport information that cannot be (easily) extracted from the model itself.
 *
 * @author Benjamin Bentmann
 */
class ModelData
{
    private final Source source;

    private final Model model;

    private String groupId;

    private String artifactId;

    private String version;

    /**
     * Creates a new container for the specified model.
     *
     * @param model The model to wrap, may be {@code null}.
     */
    ModelData( Source source, Model model )
    {
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
    ModelData( Source source, Model model, String groupId, String artifactId, String version )
    {
        this.source = source;
        this.model = model;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public Source getSource()
    {
        return source;
    }

    /**
     * Gets the model being wrapped.
     *
     * @return The model or {@code null} if not set.
     */
    public Model getModel()
    {
        return model;
    }

    /**
     * Gets the effective group identifier of the model.
     *
     * @return The effective group identifier of the model or an empty string if unknown, never {@code null}.
     */
    public String getGroupId()
    {
        return ( groupId != null ) ? groupId : "";
    }

    /**
     * Gets the effective artifact identifier of the model.
     *
     * @return The effective artifact identifier of the model or an empty string if unknown, never {@code null}.
     */
    public String getArtifactId()
    {
        return ( artifactId != null ) ? artifactId : "";
    }

    /**
     * Gets the effective version of the model.
     *
     * @return The effective version of the model or an empty string if unknown, never {@code null}.
     */
    public String getVersion()
    {
        return ( version != null ) ? version : "";
    }

    /**
     * Gets unique identifier of the model
     *
     * @return The effective identifier of the model, never {@code null}.
     */
    public String getId()
    {
        // if source is null, it is the supermodel, which can be accessed via empty string
        return Objects.toString( source, "" );
    }

    @Override
    public String toString()
    {
        return String.valueOf( model );
    }

}
