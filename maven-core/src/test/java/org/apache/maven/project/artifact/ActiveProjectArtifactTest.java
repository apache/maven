package org.apache.maven.project.artifact;

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

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.mockito.ArgumentCaptor;

import junit.framework.TestCase;

public class ActiveProjectArtifactTest extends TestCase {

    public void testPathYieldsFile() {
        final Artifact a = spy( Artifact.class );
        final ArgumentCaptor<File> file = ArgumentCaptor.forClass( File.class );
        doNothing().when( a ).setFile( file.capture() );
        doAnswer( invocation -> file.getValue() ).when( a ).getFile();
        a.setFile( null ); // init captor

        final MavenProject p = mock( MavenProject.class );
        when( p.getArtifact() ).thenReturn( a );

        final ActiveProjectArtifact artifact = new ActiveProjectArtifact( p, a );
        artifact.setPath( Path.of( "testPath" ) );
        assertEquals( new File("testPath" ), artifact.getFile() );
        artifact.setFile( new File( "testFile" ) );
        assertEquals( Path.of( "testFile" ), artifact.getPath() );
    }
}
