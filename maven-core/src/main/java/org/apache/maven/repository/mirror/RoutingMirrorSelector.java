/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.mirror;

import org.apache.maven.repository.automirror.MirrorRoute;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;

import java.util.Collections;

public class RoutingMirrorSelector
    implements MirrorSelector
{

    private final MirrorRouter mirrorRouter;

    private final DefaultMirrorSelector delegate = new DefaultMirrorSelector();

    private final Logger logger;
    
    public RoutingMirrorSelector( final MirrorRouter mirrorRouter, final Logger logger )
    {
        this.mirrorRouter = mirrorRouter;
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

            final MirrorRoute route = mirrorRouter.getMirror( repoUrl );

            if ( route != null )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "\t==> " + route );
                }

                mirror = new RemoteRepository();

                mirror.setRepositoryManager( false );
                mirror.setId( route.getId() );
                mirror.setUrl( route.getMirrorUrl() );
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
