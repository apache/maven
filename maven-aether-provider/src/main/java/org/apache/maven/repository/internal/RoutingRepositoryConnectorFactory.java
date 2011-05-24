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

package org.apache.maven.repository.internal;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component( role=RepositoryConnectorFactory.class, hint="routing" )
public class RoutingRepositoryConnectorFactory
    implements RepositoryConnectorFactory, Service
{

    @Requirement( role = RepositoryConnectorFactory.class )
    private List<RepositoryConnectorFactory> connectorFactories = new ArrayList<RepositoryConnectorFactory>();
    
    private static final Comparator<RepositoryConnectorFactory> COMPARATOR =
        new Comparator<RepositoryConnectorFactory>()
        {

            public int compare( RepositoryConnectorFactory o1, RepositoryConnectorFactory o2 )
            {
                return o2.getPriority() - o1.getPriority();
            }

        };

    public RepositoryConnector newInstance( RepositorySystemSession session, RemoteRepository repository )
        throws NoRepositoryConnectorException
    {
        return new RoutingRepositoryConnector( session, repository, connectorFactories );
    }

    public RoutingRepositoryConnectorFactory addRepositoryConnectorFactory( RepositoryConnectorFactory factory )
    {
        if ( factory == null )
        {
            throw new IllegalArgumentException( "repository connector factory has not been specified" );
        }
        
        if ( !( factory instanceof RoutingRepositoryConnectorFactory ) )
        {
            connectorFactories.add( factory );
        }
        
        return this;
    }

    public RoutingRepositoryConnectorFactory setRepositoryConnectorFactories( List<RepositoryConnectorFactory> factories )
    {
        this.connectorFactories.clear();
        
        if ( factories != null )
        {
            for ( RepositoryConnectorFactory fac : factories )
            {
                if ( !(fac instanceof RoutingRepositoryConnectorFactory ) )
                {
                    connectorFactories.add( fac );
                }
            }
        }
        
        return this;
    }
    
    public int getPriority()
    {
        return Integer.MIN_VALUE;
    }

    public void initService( ServiceLocator locator )
    {
//        setLogger( locator.getService( Logger.class ) );
//        setFileProcessor( locator.getService( FileProcessor.class ) );
//        setWagonProvider( locator.getService( WagonProvider.class ) );
//        setWagonConfigurator( locator.getService( WagonConfigurator.class ) );
    }

}
