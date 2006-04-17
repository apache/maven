package org.apache.maven.archiver;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class MavenArchiverTest
    extends TestCase
{
    static class ArtifactComparator implements Comparator {
        public int compare( Object o1, Object o2 )
        {
          return ((Artifact) o1).getArtifactId().compareTo(((Artifact) o2).getArtifactId());
        }
        public boolean equals(Object o) { return false; }
    }

    public void testGetManifestExtensionList() throws Exception
    {
        MavenArchiver archiver = new MavenArchiver();

        Model model = new Model();
        model.setArtifactId( "dummy" );

        MavenProject project = new MavenProject( model );
        // we need to sort the artifacts for test purposes
        Set artifacts = new TreeSet( new ArtifactComparator() );
        project.setArtifacts( artifacts );

        // there should be a mock or a setter for this field.
        ManifestConfiguration config = new ManifestConfiguration()
        {
            public boolean isAddExtensions()
            {
                return true;
            }
        };

        Manifest manifest;

        manifest = archiver.getManifest( project, config );

        assertNotNull( manifest.getMainSection() );

        java.util.Enumeration enume = manifest.getSectionNames();
        while (enume.hasMoreElements()) {  
           Manifest.Section section = manifest.getSection(enume.nextElement().toString());
           System.out.println( section + " " + section.getAttributeValue( "Extension-List" ) );
        }

        assertEquals( null,
                      manifest.getMainSection().getAttributeValue( "Extension-List" ) );

        MockArtifact artifact1 = new MockArtifact();
        artifact1.setGroupId( "org.apache.dummy" );
        artifact1.setArtifactId( "dummy1" );
        artifact1.setVersion( "1.0" );
        artifact1.setType( "dll" );
        artifact1.setScope( "compile" );

        artifacts.add( artifact1 );

        manifest = archiver.getManifest( project, config );

        assertEquals( null,
                      manifest.getMainSection().getAttributeValue( "Extension-List" ) );

        MockArtifact artifact2 = new MockArtifact();
        artifact2.setGroupId( "org.apache.dummy" );
        artifact2.setArtifactId( "dummy2" );
        artifact2.setVersion( "1.0" );
        artifact2.setType( "jar" );
        artifact2.setScope( "compile" );

        artifacts.add( artifact2 );

        manifest = archiver.getManifest( project, config );

        assertEquals( "dummy2",
                      manifest.getMainSection().getAttributeValue( "Extension-List" ) );

        MockArtifact artifact3 = new MockArtifact();
        artifact3.setGroupId( "org.apache.dummy" );
        artifact3.setArtifactId( "dummy3" );
        artifact3.setVersion( "1.0" );
        artifact3.setScope( "test" );
        artifact3.setType( "jar" );

        artifacts.add( artifact3 );

        manifest = archiver.getManifest( project, config );

        assertEquals( "dummy2",
                      manifest.getMainSection().getAttributeValue( "Extension-List" ) );


        MockArtifact artifact4 = new MockArtifact();
        artifact4.setGroupId( "org.apache.dummy" );
        artifact4.setArtifactId( "dummy4" );
        artifact4.setVersion( "1.0" );
        artifact4.setType( "jar" );
        artifact4.setScope( "compile" );

        artifacts.add( artifact4 );

        manifest = archiver.getManifest( project, config );

        assertEquals( "dummy2 dummy4",
                      manifest.getMainSection().getAttributeValue( "Extension-List" ) );
    }
}
