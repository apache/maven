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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;

/**
 * Provides a simple repository cache whose lifetime is scoped to a single Maven session.
 * 
 * @author Benjamin Bentmann
 */
class SessionRepositoryCache
    implements RepositoryCache
{

    private Map<Object, Object> cache = new HashMap<Object, Object>( 256 );

    public Object get( RepositoryRequest request, Object key )
    {
        return cache.get( key );
    }

    public void put( RepositoryRequest request, Object key, Object data )
    {
        cache.put( key, data );
    }

}
