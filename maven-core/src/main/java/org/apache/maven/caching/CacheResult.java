package org.apache.maven.caching;

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

import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.CacheSource;

import static java.util.Objects.requireNonNull;

/**
 * CacheResult
 */
public class CacheResult
{
    private final RestoreStatus status;
    private final BuildInfo buildInfo;
    private final CacheContext context;

    private CacheResult( RestoreStatus status, BuildInfo buildInfo, CacheContext context )
    {
        this.status = requireNonNull( status );
        this.buildInfo = buildInfo;
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

    public static CacheResult failure( BuildInfo buildInfo, CacheContext context )
    {
        requireNonNull( buildInfo );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.FAILURE, buildInfo, context );
    }

    public static CacheResult success( BuildInfo buildInfo, CacheContext context )
    {
        requireNonNull( buildInfo );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.SUCCESS, buildInfo, context );
    }

    public static CacheResult partialSuccess( BuildInfo buildInfo, CacheContext context )
    {
        requireNonNull( buildInfo );
        requireNonNull( context );
        return new CacheResult( RestoreStatus.PARTIAL, buildInfo, context );
    }

    public static CacheResult failure( CacheContext context )
    {
        requireNonNull( context );
        return new CacheResult( RestoreStatus.FAILURE, null, context );
    }

    public static CacheResult rebuilded( CacheResult orig, BuildInfo buildInfo )
    {
        requireNonNull( orig );
        requireNonNull( buildInfo );
        return new CacheResult( orig.status, buildInfo, orig.context );
    }

    public boolean isSuccess()
    {
        return status == RestoreStatus.SUCCESS;
    }

    public BuildInfo getBuildInfo()
    {
        return buildInfo;
    }

    public CacheSource getSource()
    {
        return buildInfo != null ? buildInfo.getSource() : null;
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
        return buildInfo != null && buildInfo.getDto().isFinal();
    }
}
