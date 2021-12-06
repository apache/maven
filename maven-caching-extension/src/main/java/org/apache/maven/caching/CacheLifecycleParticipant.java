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

import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.SessionScoped;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.execution.MavenSession;

@SessionScoped
@Named
@SuppressWarnings( "unused" )
public class CacheLifecycleParticipant extends AbstractMavenLifecycleParticipant
{

    private final CacheConfig cacheConfig;
    private final CacheController cacheController;

    @Inject
    public CacheLifecycleParticipant( CacheConfig cacheConfig, CacheController cacheController )
    {
        this.cacheConfig = cacheConfig;
        this.cacheController = cacheController;
    }

    @Override
    public void afterSessionEnd( MavenSession session ) throws MavenExecutionException
    {
        if ( cacheConfig.isEnabled() )
        {
            //            cacheController.saveCacheReport( session );
        }
    }
}
