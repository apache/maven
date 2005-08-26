package org.apache.maven.plugin.eclipse;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EclipsePluginTest
    extends PlexusTestCase
{
    public void testProject1()
        throws Exception
    {
        testProject( "project-1", null );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-2", null );
    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-3", null );
    }

    public void testProject4()
        throws Exception
    {
        testProject( "project-4",  getTestFile( "target/project-4-test/" ) );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void testProject( String projectName, File outputDir )
        throws Exception
    {
        File basedir = getTestFile( "src/test/projects/" + projectName );

        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        EclipsePlugin plugin = new EclipsePlugin();

        File repo = getTestFile( "src/test/repository" );

        ArtifactRepositoryLayout localRepositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", "file://" + repo.getAbsolutePath(),
                                                                            localRepositoryLayout );

        MavenProject project = builder.buildWithDependencies( new File( basedir, "project.xml" ), localRepository, null );

        File projectOutputDir = basedir;

        if ( outputDir == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir.mkdirs();

            projectOutputDir = new File( outputDir, project.getArtifactId() );
        }

        System.err.println("basedir: " + basedir+"\noutputdir: " + outputDir+"\nprojectOutputDir: " + projectOutputDir );

        plugin.setOutputDir( outputDir );


        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            artifact.setFile(new File(localRepository.getBasedir(), localRepository.pathOf(artifact)));
        }

        plugin.setProject( project );

        plugin.setLocalRepository( localRepository );

        plugin.execute();

        assertFileEquals( localRepository.getBasedir(), new File( basedir, "project" ), new File( projectOutputDir, ".project" ) );

        assertFileEquals( localRepository.getBasedir(), new File( basedir, "classpath" ), new File( projectOutputDir, ".classpath" ) );
    }

    private void assertFileEquals( String mavenRepo, File expectedFile, File actualFile )
        throws IOException
    {
        List expectedLines = getLines( mavenRepo, expectedFile );

        List actualLines = getLines( mavenRepo, actualFile );

        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();

            // replace some vars in the expected line, to account
            // for absolute paths that are different on each installation.

            expected = StringUtils.replace( expected, "${basedir}", basedir.replace( '\\', '/' ) );

            if ( actualLines.size() < i )
            {
                fail( "Too few lines in the actual file. Was " + actualLines.size() + ", expected: " + expectedLines.size() );
            }

            String actual = actualLines.get( i ).toString();

            assertEquals( "Checking line #" + (i + 1), expected, actual );
        }

        assertTrue( "Unequal number of lines.", expectedLines.size() == actualLines.size() );
    }

    private List getLines( String mavenRepo, File file )
        throws IOException
    {
        List lines = new ArrayList();

        BufferedReader reader = new BufferedReader( new FileReader( file ) );

        String line;

        while ( (line = reader.readLine()) != null )
        {
            lines.add( line );//StringUtils.replace( line, "#ArtifactRepositoryPath#", mavenRepo.replace( '\\', '/' ) ) );
        }

        return lines;
    }
}
