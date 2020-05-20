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
package org.apache.maven.lifecycle;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */

public class DefaultLifecyclesTest
    extends PlexusTestCase
{
    @Inject
    private DefaultLifecycles defaultLifeCycles;

    @Override
    protected void customizeContainerConfiguration(
            ContainerConfiguration configuration)
    {
        super.customizeContainerConfiguration(configuration);
        configuration.setAutoWiring(true);
        configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        getContainer();
    }

    @Override
    protected synchronized void setupContainer()
    {
        super.setupContainer();

        ((DefaultPlexusContainer)getContainer())
                .addPlexusInjector( Collections.emptyList(),
                        binder ->  binder.requestInjection( this ) );
    }

    public void testLifecycle()
    {
        final List<Lifecycle> cycles = defaultLifeCycles.getLifeCycles();
        assertNotNull( cycles );
        final Lifecycle lifecycle = cycles.get( 0 );
        assertEquals( "default", lifecycle.getId() );
        assertEquals( 23, lifecycle.getPhases().size() );

    }

}