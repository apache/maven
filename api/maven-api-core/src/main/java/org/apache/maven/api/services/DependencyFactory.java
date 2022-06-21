package org.apache.maven.api.services;

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

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

@Experimental
public interface DependencyFactory extends Service
{

    /**
     * Creates a new {@link Dependency} object from the request.
     *
     * @param request the request containing the various data
     * @return a new {@link Dependency} object.
     *
     * @throws IllegalArgumentException if {@code request} is null or
     *         if {@code request.getSession()} is null or invalid
     */
    @Nonnull
    Dependency create( @Nonnull DependencyFactoryRequest request );

    @Nonnull
    default Dependency create( @Nonnull Session session, @Nonnull Artifact artifact )
    {
        return create( DependencyFactoryRequest.build( session, artifact ) );
    }
}
