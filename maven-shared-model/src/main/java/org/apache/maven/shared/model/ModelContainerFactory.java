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
 * Factory for returning model container instances. Unlike most factories, implementations of this class are meant to
 * create only one type of model container instance.
 */
public interface ModelContainerFactory
{

    /**
     * Returns collection of URIs associated with this factory.
     *
     * @return collection of URIs associated with this factory
     */
    Collection<String> getUris();

    /**
     * Creates a model container instance that contains the specified model properties. The implementing class instance may
     * modify, add, delete or reorder the list of model properties before placing them into the returned model
     * container.
     *
     * @param modelProperties the model properties to be contained within the model container
     * @return the model container
     */
    ModelContainer create( List<ModelProperty> modelProperties );
}
