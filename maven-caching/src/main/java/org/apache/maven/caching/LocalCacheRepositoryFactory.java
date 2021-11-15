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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.caching.xml.CacheConfigFactory;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;

@Singleton
@Named
public class LocalCacheRepositoryFactory
{

    private static final Object KEY =  LocalCacheRepository.class.getName();

    private final Logger logger;
    private final XmlService xmlService;
    private final CacheConfigFactory cacheConfigFactory;
    private final RemoteCacheRepositoryFactory remoteCacheRepositoryFactory;

    @Inject
    public LocalCacheRepositoryFactory( Logger logger, XmlService xmlService,
                                        CacheConfigFactory cacheConfigFactory,
                                        RemoteCacheRepositoryFactory remoteCacheRepositoryFactory )
    {
        this.logger = logger;
        this.xmlService = xmlService;
        this.cacheConfigFactory = cacheConfigFactory;
        this.remoteCacheRepositoryFactory = remoteCacheRepositoryFactory;
    }

    public LocalCacheRepository getLocalCacheRepository( MavenSession session )
    {
        return SessionUtils.getOrCreate( session, KEY,
                () -> new LocalCacheRepositoryImpl( logger,
                        remoteCacheRepositoryFactory.getRemoteCacheRepository( session ),
                        xmlService,
                        cacheConfigFactory.getCacheConfig( session ) ) );
    }

}
