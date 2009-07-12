package org.apache.maven.embedder;

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

import java.io.File;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/** @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a> */
public class TestComponentOverride
    extends PlexusTestCase
{
    private MavenEmbedder maven;

    protected PlexusContainer container;

    protected void setUp()
        throws Exception
    {
        Configuration request = new SimpleConfiguration();

        File extensions = new File( getBasedir(), "src/test/extensions" );
        
        assertTrue( extensions.exists() );
        
        request.addExtension( extensions.toURI().toURL() );

        maven = new MavenEmbedder( request );
        
        container = maven.getPlexusContainer();
    }

    public void testComponentOverride()
        throws ComponentLookupException
    {
        ArtifactFactory factory = container.lookup( ArtifactFactory.class );

        assertNotNull( factory );

        assertTrue( "Expecting " + CustomArtifactFactory.class.getName() + " but was " + factory.getClass().getName(), CustomArtifactFactory.class.isAssignableFrom( factory.getClass() ) );

        // test wheter the requirement is injected - if not, it nullpointers
        factory.createArtifact( "testGroupId", "testArtifactId", "testVersion", "compile", "jar" );
    }
}
