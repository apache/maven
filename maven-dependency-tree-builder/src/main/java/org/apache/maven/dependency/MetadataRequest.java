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

/**
 * Describes a request to resolve the metadata of a dependency.
 * 
 * @author Benjamin Bentmann
 */
public interface MetadataRequest
{

    /**
     * The group id of the dependency.
     * 
     * @return The group id of the dependency, never {@code null}.
     */
    String getGroupId();

    /**
     * The artifact id of the dependency.
     * 
     * @return The artifact id of the dependency, never {@code null}.
     */
    String getArtifactId();

    /**
     * The version of the dependency. This has to be a simple version like "1.0" and must not be range.
     * 
     * @return The version of the dependency.
     */
    String getVersion();

    /**
     * The identifier of the repository from which the metadata should preferrably be fetched.
     * 
     * @return The identifier of the preferred repository or {@code null} if none.
     */
    String getRepositoryId();

    /**
     * The collector used to record any problems that are encountered while reading the metadata.
     * 
     * @return The collector used to record any problems, never {@code null}.
     */
    DependencyProblemCollector getProblems();

}
