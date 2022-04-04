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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.Session;

/**
 * Base class for requests.
 */
abstract class BaseRequest
{

    private final Session session;

    protected BaseRequest( @Nonnull Session session )
    {
        this.session = nonNull( session, "session can not be null" );
    }

    @Nonnull
    public Session getSession()
    {
        return session;
    }

    public static <T> T nonNull( T obj, String message )
    {
        if ( obj == null )
        {
            throw new IllegalArgumentException( message );
        }
        return obj;
    }

    protected static <T> Collection<T> unmodifiable( Collection<T> obj )
    {
        return obj != null && !obj.isEmpty()
                ? Collections.unmodifiableCollection( new ArrayList<>( obj ) ) : Collections.emptyList();
    }
}
