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
package org.apache.maven.caching;

import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheSource;

import static java.util.Objects.requireNonNull;

/**
 * CacheResult
 */
public class CacheResult
{

    private final RestoreStatus status;
    private final Build build;
    private final CacheContext context;

    private CacheResult( RestoreStatus status, Build build, CacheContext context )
    {
        this.status = requireNonNull( status );
        this.build = build;
        this.context = context;
    }

    public static CacheResult empty( CacheContext context )
    {
        requireNonNull( context );
        return new CacheResult( RestoreStatus.EMPTY, null, context );
    }

    public static CacheResult empty()
    {
        return new CacheResult( RestoreStatus.EMPTY, null, null );
    }

    public static CacheResult failure( Build build, CacheContext context )
    {
        requireNonNull( build );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.FAILURE, build, context );
    }

    public static CacheResult success( Build build, CacheContext context )
    {
        requireNonNull( build );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.SUCCESS, build, context );
    }

    public static CacheResult partialSuccess( Build build, CacheContext context )
    {
        requireNonNull( build );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.PARTIAL, build, context );
    }

    public static CacheResult failure( CacheContext context )
    {
        requireNonNull( context );
        return new CacheResult( RestoreStatus.FAILURE, null, context );
    }

    public static CacheResult rebuilded( CacheResult orig, Build build )
    {
        requireNonNull( orig );
        requireNonNull( build );
        return new CacheResult( orig.status, build, orig.context );
    }

    public boolean isSuccess()
    {
        return status == RestoreStatus.SUCCESS;
    }

    public Build getBuildInfo()
    {
        return build;
    }

    public CacheSource getSource()
    {
        return build != null ? build.getSource() : null;
    }

    public CacheContext getContext()
    {
        return context;
    }

    public boolean isPartialSuccess()
    {
        return status == RestoreStatus.PARTIAL;
    }

    public RestoreStatus getStatus()
    {
        return status;
    }

    public boolean isFinal()
    {
        return build != null && build.getDto().is_final();
    }
}
