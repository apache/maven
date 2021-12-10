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
package org.apache.maven.buildcache;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.buildcache.xml.CacheConfig;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Remote cache repository.
 */
@Singleton
@Named( "#factory#" )
@Priority( 10 )
public class RemoteCacheRepositoryProvider implements Provider<RemoteCacheRepository>
{

    private final RemoteCacheRepository repository;

    @Inject
    public RemoteCacheRepositoryProvider( CacheConfig config, PlexusContainer container )
            throws ComponentLookupException
    {
        config.initialize();
        String hint = config.getTransport();
        repository = container.lookup( RemoteCacheRepository.class, hint );
    }

    public RemoteCacheRepository get()
    {
        return repository;
    }

}
