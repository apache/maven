package org.apache.maven.shared.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;
import java.util.List;

/**
 * Provides services for joining, deleting and querying model containers.
 */
public interface ModelDataSource
{   
    /**
     * Join model properties of the specified container a with the specified container b. Any elements of model container
     * a must take precedence over model container b. All elements of model container A must exist in the data source;
     * elements of model container b may or may not exist.
     *
     * @param a model container with precedence
     * @param b model container without precedence
     * @return joined model container
     */
    ModelContainer join( ModelContainer a, ModelContainer b )
        throws DataSourceException;

    /**
     * Deletes properties of the specified model container from the data source.
     *
     * @param modelContainer the model container that holds the properties to be deleted
     */
    void delete( ModelContainer modelContainer );


    /**
     * Return copy of underlying model properties. No changes in this list will be reflected in the data source.
     *
     * @return copy of underlying model properties
     */
    List<ModelProperty> getModelProperties();

    /**
     * Returns model containers for the specified URI.
     *
     * @param uri
     * @return
     */
    List<ModelContainer> queryFor( String uri )
        throws DataSourceException;


    /**
     * Initializes the object with model properties.
     *
     * @param modelProperties the model properties that back the data source
     */
    void init( List<ModelProperty> modelProperties, Collection<ModelContainerFactory> modelContainerFactories );

    /**
     * Return history of all joins and deletes
     *
     * @return history of all joins and deletes
     */
    String getEventHistory();
}
