package org.apache.maven.container;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.inject.Module;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.eclipse.sisu.plexus.Hints;

import static java.util.Objects.requireNonNull;

public class DefaultContainer implements Container
{

    private final DefaultPlexusContainer container;

    public DefaultContainer( ClassRealm realm )
    {
        try
        {
            ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld( realm.getWorld() )
                    .setRealm( realm ).setClassPathScanning( PlexusConstants.SCANNING_INDEX ).setAutoWiring( true )
                    .setJSR250Lifecycle( true ).setName( "maven" );
            this.container = new DefaultPlexusContainer( cc );
            this.container.addComponent( this, Container.class.getName() );
        }
        catch ( PlexusContainerException e )
        {
            throw new RuntimeException( "Unable to create container", e );
        }
    }

    public DefaultContainer( final DefaultPlexusContainer container )
    {
        this.container = requireNonNull( container );
        this.container.addComponent( this, Container.class.getName() );
    }

    public void setLoggerManager( LoggerManager loggerManager )
    {
        container.setLoggerManager( loggerManager );
    }

    /**
     * Performs a component lookup obeying "plexus visibility rules". Returns {@code null} if no component found.
     */
    public <T> T lookup( final Class<T> role )
    {
        return lookup( role, "" );
    }

    /**
     * Performs a component lookup obeying "plexus visibility rules". Returns {@code null} if no component found.
     */
    public <T> T lookup( final Class<T> role, final String hint )
    {
        try
        {
            return container.lookup( role, hint );
        }
        catch ( ComponentLookupException e )
        {
            if ( e.getCause() instanceof NoSuchElementException )
            {
                return null;
            }
            throw new RuntimeException( e );
        }
    }

    /**
     * Performs a map component lookup obeying "plexus visibility rules". Never returns {@code null}.
     */
    public <T> Map<String, T> lookupMap( final Class<T> role )
    {
        try
        {
            return container.lookupMap( role );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public <T> List<T> lookupList( Class<T> role )
    {
        try
        {
            return container.lookupList( role );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void release( Object component )
    {
        container.release( component );
    }

    @Override
    public <R, I extends R> void addComponent( Class<R> role, I impl )
    {
        container.addComponent( impl, role, Hints.DEFAULT_HINT );
    }

    @Override
    public <R, I extends R> void addComponent( Class<R> role, Class<I> impl )
    {
        ComponentDescriptor<? extends R> cd = new ComponentDescriptor<>( impl, getContainerRealm() );
        cd.setRoleClass( role );
        addComponent( cd );
    }

    @Override
    public void addComponent( ComponentDescriptor<?> componentDescriptor )
    {
        container.addComponentDescriptor( componentDescriptor );
    }

    @Override
    public ClassRealm setLookupRealm( ClassRealm realm )
    {
        return container.setLookupRealm( realm );
    }

    @Override
    public void discoverComponents( ClassRealm realm, Module... modules )
    {
        container.discoverComponents( realm, modules );
    }

    @Override
    public ClassWorld getClassWorld()
    {
        return getContainerRealm().getWorld();
    }

    @Override
    public ClassRealm getContainerRealm()
    {
        return container.getContainerRealm();
    }

    @Override
    public void dispose()
    {
        container.dispose();
    }

}
