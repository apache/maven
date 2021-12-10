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
import javax.inject.Singleton;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import static java.util.Objects.requireNonNull;

/**
 * A wagon provider backed by a Plexus container and the wagons registered with this container.
 */
@Singleton
public class WagonProvider
{

    private final PlexusContainer container;

    /**
     * Creates a wagon provider using the specified Plexus container.
     *
     * @param container The Plexus container instance to use, must not be {@code null}.
     */
    @Inject
    public WagonProvider( final PlexusContainer container )
    {
        this.container = requireNonNull( container, "plexus container cannot be null" );
    }

    public Wagon lookup( String roleHint )
            throws ComponentLookupException
    {
        return container.lookup( Wagon.class, roleHint );
    }

    public void release( Wagon wagon )
            throws ComponentLifecycleException
    {
        if ( wagon != null )
        {
            container.release( wagon );
        }
    }

}
