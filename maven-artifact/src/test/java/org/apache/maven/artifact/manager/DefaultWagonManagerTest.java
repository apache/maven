package org.apache.maven.artifact.manager;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class DefaultWagonManagerTest
    extends PlexusTestCase
{

    private WagonManager wagonManager;

    private TransferListener transferListener = new Debug();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        wagonManager = (WagonManager) lookup( WagonManager.ROLE );
    }

    public void testDefaultWagonManager()
        throws Exception
    {
        assertWagon( "a" );

        assertWagon( "b1" );

        assertWagon( "b2" );

        assertWagon( "c" );

        assertWagon( "noop" );

        try
        {
            assertWagon( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            //ok
            assertTrue( true );
        }
    }

    public void testGetWagonRepository()
        throws Exception
    {
        assertWagonRepository( "a" );

        assertWagonRepository( "b1" );

        assertWagonRepository( "b2" );

        assertWagonRepository( "c" );

        try
        {
            assertWagonRepository( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            //ok
            assertTrue( true );
        }
    }

    public void testGetWagonRepositoryNullProtocol()
        throws Exception
    {
        try
        {
            Repository repository = new Repository();

            repository.setProtocol( null );

            Wagon wagon = (Wagon) wagonManager.getWagon( repository );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            //ok
            assertTrue( true );
        }
    }

    /**
     * Check that transfer listeners are properly removed after getArtifact and putArtifact
     */
    public void testWagonTransferListenerRemovedAfterGetArtifactAndPutArtifact()
        throws Exception
    {
        File tmpFile = File.createTempFile( "mvn-test", ".temp" );

        try
        {
            tmpFile.deleteOnExit();
            Artifact artifact = new DefaultArtifact( "sample.group", "sample-art", VersionRange
                .createFromVersion( "1.0" ), "scope", "type", "classifier", null );
            artifact.setFile( tmpFile );
            ArtifactRepository repo = new DefaultArtifactRepository( "id", "noop://url",
                                                                     new ArtifactRepositoryLayoutStub() );
            WagonNoOp wagon = (WagonNoOp) wagonManager.getWagon( "noop" );

            /* getArtifact */
            assertFalse( "Transfer listener is registered before test", wagon.getTransferEventSupport()
                .hasTransferListener( transferListener ) );
            wagonManager.setDownloadMonitor( transferListener );
            wagonManager.getArtifact( artifact, repo );
            assertFalse( "Transfer listener still registered after getArtifact", wagon.getTransferEventSupport()
                .hasTransferListener( transferListener ) );

            /* putArtifact */
            assertFalse( "Transfer listener is registered before test", wagon.getTransferEventSupport()
                .hasTransferListener( transferListener ) );
            wagonManager.setDownloadMonitor( transferListener );
            wagonManager.putArtifact( new File( "sample file" ), artifact, repo );
            assertFalse( "Transfer listener still registered after putArtifact", wagon.getTransferEventSupport()
                .hasTransferListener( transferListener ) );
        }
        finally
        {
            tmpFile.delete();
        }
    }

    private void assertWagon( String protocol )
        throws Exception
    {
        Wagon wagon = wagonManager.getWagon( protocol );

        assertNotNull( "Check wagon, protocol=" + protocol, wagon );
    }

    private void assertWagonRepository( String protocol )
        throws Exception
    {
        Repository repository = new Repository();

        String s = "value=" + protocol;

        repository.setId( "id=" + protocol );

        repository.setProtocol( protocol );

        Xpp3Dom conf = new Xpp3Dom( "configuration" );

        Xpp3Dom configurableField = new Xpp3Dom( "configurableField" );

        configurableField.setValue( s );

        conf.addChild( configurableField );

        wagonManager.addConfiguration( repository.getId(), conf );

        WagonMock wagon = (WagonMock) wagonManager.getWagon( repository );

        assertNotNull( "Check wagon, protocol=" + protocol, wagon );

        assertEquals( "Check configuration for wagon, protocol=" + protocol, s, wagon.getConfigurableField() );
    }

    private final class ArtifactRepositoryLayoutStub
        implements ArtifactRepositoryLayout
    {
        public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
        {
            return "path";
        }

        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return "path";
        }

        public String pathOf( Artifact artifact )
        {
            return "path";
        }
    }

}
