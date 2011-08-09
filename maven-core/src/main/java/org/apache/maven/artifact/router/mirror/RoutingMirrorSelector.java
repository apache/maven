package org.apache.maven.artifact.router.mirror;

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

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.MirrorRoute;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;

import java.util.Collections;

public class RoutingMirrorSelector
    implements MirrorSelector
{

    private final ArtifactRouter router;

    private final DefaultMirrorSelector delegate = new DefaultMirrorSelector();

    private final Logger logger;
    
    public RoutingMirrorSelector( final ArtifactRouter router, final Logger logger )
    {
        this.router = router;
        this.logger = logger;
    }

    public RemoteRepository getMirror( final RemoteRepository repository )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "AETHER-SELECT: " + repository.getUrl() );
        }

        RemoteRepository mirror = delegate.getMirror( repository );
        if ( mirror != null )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "AETHER-SELECT using mirror from settings.xml." );
            }

            return mirror;
        }

        if ( mirror == null )
        {
            final String repoUrl = repository.getUrl();

            final MirrorRoute route = router.selectSingleMirror( repoUrl );

            if ( route != null )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "\t==> " + route );
                }

                mirror = new RemoteRepository();

                mirror.setRepositoryManager( false );
                mirror.setId( route.getId() );
                mirror.setUrl( route.getRouteUrl() );
                mirror.setContentType( repository.getContentType() );
                mirror.setPolicy( true, repository.getPolicy( true ) );
                mirror.setPolicy( false, repository.getPolicy( false ) );

                mirror.setMirroredRepositories( Collections.singletonList( repository ) );
            }
            else
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "AETHER-SELECT: no auto-mirror found." );
                }
            }
        }

        return mirror;
    }

    public RoutingMirrorSelector add( final String id, final String url, final String type,
                                      final boolean repositoryManager, final String mirrorOfIds,
                                      final String mirrorOfTypes )
    {
        delegate.add( id, url, type, repositoryManager, mirrorOfIds, mirrorOfTypes );
        return this;
    }

}
