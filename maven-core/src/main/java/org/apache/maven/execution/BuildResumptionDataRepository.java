package org.apache.maven.execution;

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

import org.apache.maven.project.MavenProject;

/**
 * Instances of this interface retrieve and store data for the --resume / -r feature. This data is used to ensure newer
 * builds of the same project, that have the -r command-line flag, skip successfully built projects during earlier
 * invocations of Maven.
 */
public interface BuildResumptionDataRepository
{
    /**
     * Persists any data needed to resume the build at a later point in time, using a new Maven invocation. This method
     * may also decide it is not needed or meaningful to persist such data, and return <code>false</code> to indicate
     * so.
     *
     * @param rootProject The root project that is being built.
     * @param buildResumptionData Information needed to resume the build.
     * @throws BuildResumptionPersistenceException When an error occurs while persisting data.
     */
    void persistResumptionData( MavenProject rootProject, BuildResumptionData buildResumptionData )
            throws BuildResumptionPersistenceException;

    /**
     * Uses previously stored resumption data to enrich an existing execution request.
     * @param request The execution request that will be enriched.
     * @param rootProject The root project that is being built.
     */
    void applyResumptionData( MavenExecutionRequest request, MavenProject rootProject );

    /**
     * Removes previously stored resumption data.
     * @param rootProject The root project that is being built.
     */
    void removeResumptionData( MavenProject rootProject );

}
