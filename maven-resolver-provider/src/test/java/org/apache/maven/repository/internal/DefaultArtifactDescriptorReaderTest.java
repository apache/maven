package org.apache.maven.repository.internal;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.mockito.ArgumentCaptor;

public class DefaultArtifactDescriptorReaderTest
    extends AbstractRepositoryTestCase
{

    private DefaultArtifactDescriptorReader reader;

    private RepositoryEventDispatcher mockEventDispatcher;

    private ArgumentCaptor<RepositoryEvent> eventCaptor;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        reader = (DefaultArtifactDescriptorReader) lookup( ArtifactDescriptorReader.class );

        mockEventDispatcher = mock( RepositoryEventDispatcher.class );

        eventCaptor = ArgumentCaptor.forClass( RepositoryEvent.class );

        reader.setRepositoryEventDispatcher( mockEventDispatcher );
    }

    public void testMng5459()
        throws Exception
    {
        // prepare
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();

        request.addRepository( newTestRepository() );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "dep-mng5459", "jar", "0.4.0-SNAPSHOT" ) );

        // execute
        reader.readArtifactDescriptor( session, request );

        // verify
        verify( mockEventDispatcher ).dispatch( eventCaptor.capture() );

        boolean missingArtifactDescriptor = false;

        for( RepositoryEvent evt : eventCaptor.getAllValues() )
        {
            if ( EventType.ARTIFACT_DESCRIPTOR_MISSING.equals( evt.getType() ) )
            {
                assertEquals( "Could not find artifact org.apache.maven.its:dep-mng5459:pom:0.4.0-20130404.090532-2 in repo (" + newTestRepository().getUrl() + ")", evt.getException().getMessage() );
                missingArtifactDescriptor = true;
            }
        }

        if( !missingArtifactDescriptor )
        {
            fail( "Expected missing artifact descriptor for org.apache.maven.its:dep-mng5459:pom:0.4.0-20130404.090532-2" );
        }
    }

    public void testNonexistentRepository()
        throws Exception
    {
        // prepare
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();

        // [MNG-6732] DefaultArtifactDescriptorReader.loadPom to check IGNORE_MISSING policy upon ArtifactTransferException
        RemoteRepository nonexistentRepository = new RemoteRepository.Builder( "repo", "default", "http://nonexistent.domain" ).build();

        request.addRepository( nonexistentRepository );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "dep-mng6732", "jar", "0.0.1" ) );

        // execute
        reader.readArtifactDescriptor( session, request );

        // verify
        verify(mockEventDispatcher).dispatch( eventCaptor.capture() );

        boolean artifactTransferExceptionFound = false;

        for( RepositoryEvent evt : eventCaptor.getAllValues() )
        {
            if ( EventType.ARTIFACT_DESCRIPTOR_MISSING.equals( evt.getType() ) )
            {
                assertEquals( "Could not transfer artifact org.apache.maven.its:dep-mng6732:pom:0.0.1 from/to repo (" + nonexistentRepository.getUrl()
                    + "): Cannot access http://nonexistent.domain with type default using the available connector factories: BasicRepositoryConnectorFactory", evt.getException().getMessage() );
                artifactTransferExceptionFound = true;
            }
        }

        if( !artifactTransferExceptionFound )
        {
            fail( "Expected missing artifact descriptor for org.apache.maven.its:dep-mng6732:pom:0.0.1" );
        }
    }
}
