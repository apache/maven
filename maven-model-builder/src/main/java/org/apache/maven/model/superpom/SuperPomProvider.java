package org.apache.maven.model.superpom;

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
 * Provides the super POM that all models implicitly inherit from.
 *
 * @author Benjamin Bentmann
 */
public interface SuperPomProvider
{

    /**
     * Gets the super POM for the specified model version. The returned model is supposed to be read-only, i.e. if the
     * caller intends to make updates to the model the return value must be cloned before updating to ensure the
     * modifications don't affect future retrievals of the super POM.
     *
     * @param version The model version to retrieve the super POM for (e.g. "4.0.0"), must not be {@code null}.
     * @return The super POM, never {@code null}.
     */
    Model getSuperModel( String version );

}
