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

import java.nio.file.Path;

import org.apache.maven.model.Model;

/**
 * Context used to transform a pom file.
 *
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public interface TransformerContext
{
    /**
     * Key to get the TransformerContext from the SessionData
     */
    Object KEY = TransformerContext.class;

    /**
     * Get the value of the commandline argument {@code -Dkey=value}
     * @param key
     * @return
     */
    String getUserProperty( String key );

    /**
     * Get the model based on the path, will be used to resolve the parent based on relativePath
     *
     * @param pomFile the path to the pomFile
     * @return the model, otherwise {@code null}
     */
    Model getRawModel( Path pomFile );

    /**
     * Get the model from the reactor based on the groupId and artifactId, will be used for reactor dependencies
     *
     * @param groupId the groupId
     * @param artifactId the artifactId
     * @return the model, otherwise {@code null}
     * @throws IllegalStateException if multiple versions of the same GA are part of the reactor
     */
    Model getRawModel( String groupId, String artifactId );
}
