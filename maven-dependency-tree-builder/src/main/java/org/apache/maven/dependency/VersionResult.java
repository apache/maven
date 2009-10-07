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

import java.util.List;
import java.util.Map;

/**
 * Describes the result of a dependency version resolution request.
 * 
 * @author Benjamin Bentmann
 */
public interface VersionResult
{

    /**
     * Gets the versions matching the corresponding request.
     * 
     * @return The matching versions, can be empty but never {@code null}.
     */
    List<String> getVersions();

    /**
     * Gets the identifiers of the repositories from which the versions were resolved. A later resolution of the
     * dependency metadata for a particular version is supposed to prefer to repository in which the version was
     * originally discovered.
     * 
     * @return The identifiers of the repositories, indexed by version, can be empty but never {@code null}.
     */
    Map<String, String> getRepositoryIds();

}
