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

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.caching.xml.CacheConfigFactory;
import org.apache.maven.caching.xml.XmlService;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;

@Singleton
@Named
public class CacheControllerFactory
{

    private static final Object KEY =  CacheControllerFactory.class.getName();

    private final Logger logger;
    private final XmlService xmlService;
    private final MavenPluginManager mavenPluginManager;
    private final MavenProjectHelper projectHelper;
    private final ArtifactHandlerManager artifactHandlerManager;
    private final RepositorySystem repoSystem;
    private final CacheConfigFactory cacheConfigFactory;
    private final LocalCacheRepositoryFactory localCacheRepositoryFactory;
    private final RemoteCacheRepositoryFactory remoteCacheRepositoryFactory;

    @Inject
    public CacheControllerFactory( Logger logger,
                                   XmlService xmlService,
                                   MavenPluginManager mavenPluginManager,
                                   MavenProjectHelper projectHelper,
                                   ArtifactHandlerManager artifactHandlerManager,
                                   RepositorySystem repoSystem,
                                   CacheConfigFactory cacheConfigFactory,
                                   LocalCacheRepositoryFactory localCacheRepositoryFactory,
                                   RemoteCacheRepositoryFactory remoteCacheRepositoryFactory )
    {
        this.logger = logger;
        this.xmlService = xmlService;
        this.mavenPluginManager = mavenPluginManager;
        this.projectHelper = projectHelper;
        this.artifactHandlerManager = artifactHandlerManager;
        this.repoSystem = repoSystem;
        this.cacheConfigFactory = cacheConfigFactory;
        this.localCacheRepositoryFactory = localCacheRepositoryFactory;
        this.remoteCacheRepositoryFactory = remoteCacheRepositoryFactory;
    }

    public CacheController getCacheContoller( MavenSession session )
    {
        return SessionUtils.getOrCreate( session, KEY,
                () -> new CacheControllerImpl( logger,
                        mavenPluginManager, projectHelper,
                        repoSystem, artifactHandlerManager,
                        xmlService,
                        localCacheRepositoryFactory.getLocalCacheRepository( session ),
                        remoteCacheRepositoryFactory.getRemoteCacheRepository( session ),
                        cacheConfigFactory.getCacheConfig( session ) ) );
    }

}
