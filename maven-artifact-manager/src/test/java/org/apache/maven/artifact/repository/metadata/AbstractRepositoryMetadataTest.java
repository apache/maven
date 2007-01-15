package org.apache.maven.artifact.repository.metadata;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.testutils.MockManager;
import org.apache.maven.artifact.testutils.TestFileManager;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class AbstractRepositoryMetadataTest
    extends TestCase
{

    private MockManager mm = new MockManager();
    private TestFileManager fileManager = new TestFileManager( "AbstractRepositoryMetadataTest.test.", "" );

    public void tearDown() throws IOException
    {
        fileManager.cleanUp();
    }

    public void testUpdateRepositoryMetadata_NoVersionTagIfMainVersionIsLATEST()
        throws IOException, XmlPullParserException
    {
        MockAndControlForArtifactRepository local = new MockAndControlForArtifactRepository();
        MockAndControlForArtifactRepository remote = new MockAndControlForArtifactRepository();

        File basedir = fileManager.createTempDir();

        String path = "metadata.xml";

        Metadata m = new Metadata();
        m.setVersion( Artifact.LATEST_VERSION );

        TestRepoMetadata trm = new TestRepoMetadata( m );

        local.expectGetBasedir( basedir );
        local.expectPathOfLocalRepositoryMetadata( trm, remote.repository, path );

        mm.replayAll();

        trm.updateRepositoryMetadata( local.repository, remote.repository );

        fileManager.assertFileExistence( basedir, path, true );
        assertTrue( fileManager.getFileContents( new File( basedir, path ) ).indexOf( "<version>"
            + Artifact.LATEST_VERSION + "</version>" ) < 0 );

        mm.verifyAll();
    }

    public void testUpdateRepositoryMetadata_NoVersionTagIfVersionIsRELEASE()
        throws IOException, XmlPullParserException
    {
        MockAndControlForArtifactRepository local = new MockAndControlForArtifactRepository();
        MockAndControlForArtifactRepository remote = new MockAndControlForArtifactRepository();

        File basedir = fileManager.createTempDir();

        String path = "metadata.xml";

        Metadata m = new Metadata();
        m.setVersion( Artifact.RELEASE_VERSION );

        TestRepoMetadata trm = new TestRepoMetadata( m );

        local.expectGetBasedir( basedir );
        local.expectPathOfLocalRepositoryMetadata( trm, remote.repository, path );

        mm.replayAll();

        trm.updateRepositoryMetadata( local.repository, remote.repository );

        fileManager.assertFileExistence( basedir, path, true );
        assertTrue( fileManager.getFileContents( new File( basedir, path ) ).indexOf( "<version>"
            + Artifact.RELEASE_VERSION + "</version>" ) < 0 );

        mm.verifyAll();
    }

    private final class MockAndControlForArtifactRepository
    {
        MockControl control;

        ArtifactRepository repository;

        public MockAndControlForArtifactRepository()
        {
            control = MockControl.createControl( ArtifactRepository.class );
            mm.add( control );

            repository = ( ArtifactRepository ) control.getMock();
        }

        public void expectPathOfLocalRepositoryMetadata( TestRepoMetadata trm, ArtifactRepository remote, String path )
        {
            repository.pathOfLocalRepositoryMetadata( trm, remote );
            control.setReturnValue( path, MockControl.ONE_OR_MORE );
        }

        public void expectGetBasedir( File basedir )
        {
            repository.getBasedir();
            control.setReturnValue( basedir.getAbsolutePath(), MockControl.ONE_OR_MORE );
        }
    }

    private static final class TestRepoMetadata
        extends AbstractRepositoryMetadata
    {

        protected TestRepoMetadata( Metadata metadata )
        {
            super( metadata );
        }

        public boolean isSnapshot()
        {
            return false;
        }

        public void setRepository( ArtifactRepository remoteRepository )
        {
        }

        public String getArtifactId()
        {
            return null;
        }

        public String getBaseVersion()
        {
            return null;
        }

        public String getGroupId()
        {
            return null;
        }

        public Object getKey()
        {
            return null;
        }

        public boolean storedInArtifactVersionDirectory()
        {
            return false;
        }

        public boolean storedInGroupDirectory()
        {
            return false;
        }

    }

}
