package org.apache.maven.lifecycle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Kristian Rosenvold
 */

public class DefaultLifecyclesTest
    extends PlexusTestCase
{
    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        super.customizeContainerConfiguration( configuration );
        configuration.setAutoWiring( true );
        configuration.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        defaultLifeCycles = lookup( DefaultLifecycles.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        defaultLifeCycles = null;
        super.tearDown();
    }

    public void testDefaultLifecycles()
    {
        final List<Lifecycle> lifecycles = defaultLifeCycles.getLifeCycles();
        assertThat( lifecycles, hasSize( 4 ) );
        assertThat( DefaultLifecycles.STANDARD_LIFECYCLES,  arrayWithSize( 4 ) );
    }

    public void testDefaultLifecycle()
    {
        final Lifecycle lifecycle = getLifeCycleById( "default" );
        assertThat( lifecycle.getId(), is( "default" )  );
        assertThat( lifecycle.getPhases(), hasSize( 23 ) );
    }

    public void testCleanLifecycle()
    {
        final Lifecycle lifecycle = getLifeCycleById( "clean" );
        assertThat( lifecycle.getId(), is( "clean" )  );
        assertThat( lifecycle.getPhases(), hasSize( 3 ) );
    }

    public void testSiteLifecycle()
    {
        final Lifecycle lifecycle = getLifeCycleById( "site" );
        assertThat( lifecycle.getId(), is( "site" )  );
        assertThat( lifecycle.getPhases(), hasSize( 4 ) );
    }

    public void testWrapperLifecycle()
    {
        final Lifecycle lifecycle = getLifeCycleById( "wrapper" );
        assertThat( lifecycle.getId(), is( "wrapper" )  );
        assertThat( lifecycle.getPhases(), hasSize( 1 ) );
    }
    
    public void testCustomLifecycle()
    {
        List<Lifecycle> myLifecycles = new ArrayList<>();
        Lifecycle myLifecycle = new Lifecycle( "etl", 
                                               Arrays.asList( "extract", "transform", "load" ),
                                               Collections.emptyMap() );
        myLifecycles.add( myLifecycle );
        myLifecycles.addAll( defaultLifeCycles.getLifeCycles() );
        
        DefaultLifecycles dl = new DefaultLifecycles( myLifecycles.stream()
                                                            .collect( Collectors.toMap( l -> l.getId(), l -> l ) ), 
                                                      null );
        
        assertThat( dl.getLifeCycles().get( 0 ).getId(), is( "default" ) );
        assertThat( dl.getLifeCycles().get( 1 ).getId(), is( "clean" ) );
        assertThat( dl.getLifeCycles().get( 2 ).getId(), is( "site" ) );
        assertThat( dl.getLifeCycles().get( 3 ).getId(), is( "wrapper" ) );
        assertThat( dl.getLifeCycles().get( 4 ).getId(), is( "etl" ) );
    }
    
    private Lifecycle getLifeCycleById( String id )
    {
        return defaultLifeCycles.getLifeCycles().stream()
                        .filter( l -> id.equals( l.getId() ) )
                        .findFirst()
                        .orElseThrow( IllegalArgumentException::new );
    }
}