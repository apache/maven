package org.apache.maven.dependency;

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
 * Describes the result of a dependency metadata resolution request.
 * 
 * @author Benjamin Bentmann
 */
public interface MetadataResult
{

    /**
     * The metadata of the dependency.
     * 
     * @return The metadata of the dependency or {@code null} if the metadata could not be read.
     */
    Model getMetadata();

    /**
     * The identifier of the repository from which the metadata was retrieved. A later resolution of the dependency's
     * file is supposed to prefer this repository.
     * 
     * @return The identifier of the repository or {@code null} if unknown.
     */
    String getRepositoryId();

}
