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
public class RemoteCacheRepositoryFactory
{

    private static final Object KEY =  RemoteCacheRepositoryFactory.class.getName();

    private final Logger logger;
    private final XmlService xmlService;
    private final CacheConfigFactory cacheConfigFactory;

    @Inject
    public RemoteCacheRepositoryFactory( Logger logger, XmlService xmlService, CacheConfigFactory cacheConfigFactory )
    {
        this.logger = logger;
        this.xmlService = xmlService;
        this.cacheConfigFactory = cacheConfigFactory;
    }

    public RemoteCacheRepository getRemoteCacheRepository( MavenSession session )
    {
        return SessionUtils.getOrCreate( session, KEY,
                () -> new HttpCacheRepositoryImpl( logger,
                        null,
                        xmlService,
                        cacheConfigFactory.getCacheConfig( session ) ) );
    }

}
