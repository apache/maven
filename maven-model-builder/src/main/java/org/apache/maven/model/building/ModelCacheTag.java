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

import org.apache.maven.model.Model;

/**
 * Describes a tag used by the model builder to access a {@link ModelCache}. This interface simply aggregates a name and
 * a class to provide some type safety when working with the otherwise untyped cache.
 * 
 * @author Benjamin Bentmann
 * @param <T> The type of data associated with the tag.
 */
interface ModelCacheTag<T>
{

    /**
     * Gets the name of the tag.
     * 
     * @return The name of the tag, must not be {@code null}.
     */
    String getName();

    /**
     * Gets the type of data associated with this tag.
     * 
     * @return The type of data, must not be {@code null}.
     */
    Class<T> getType();

    /**
     * The tag used to denote raw model data.
     */
    public static final ModelCacheTag<ModelData> RAW = new ModelCacheTag<ModelData>()
    {

        public String getName()
        {
            return "raw";
        }

        public Class<ModelData> getType()
        {
            return ModelData.class;
        }

    };

    /**
     * The tag used to denote an effective model.
     */
    public static final ModelCacheTag<Model> EFFECTIVE = new ModelCacheTag<Model>()
    {

        public String getName()
        {
            return "effective";
        }

        public Class<Model> getType()
        {
            return Model.class;
        }

    };

}
