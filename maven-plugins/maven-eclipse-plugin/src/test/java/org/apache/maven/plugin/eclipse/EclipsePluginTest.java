package org.apache.maven.plugin.eclipse;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import java.util.Collections;

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
        testProject( "project-1" );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-2" );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void testProject( String projectName )
        throws Exception
    {
        File basedir = getTestFile( "src/test/projects/" + projectName );

        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        EclipsePlugin plugin = new EclipsePlugin();

        File repo = getTestFile( "src/test/repository" );

        ArtifactRepositoryLayout localRepositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        ArtifactRepository localRepository = new ArtifactRepository("local", "file://" + repo.getAbsolutePath(), localRepositoryLayout);

        MavenProject project = builder.build( new File( basedir, "project.xml" ), localRepository, Collections.EMPTY_LIST );

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            artifact.setFile(new File(localRepository.getBasedir(), localRepository.pathOf(artifact)));
        }

        plugin.setProject( project );

        plugin.setLocalRepository( localRepository );

        plugin.execute();

        assertFileEquals( localRepository.getBasedir(), new File( basedir, "project" ), new File( basedir, ".project" ) );

        assertFileEquals( localRepository.getBasedir(), new File( basedir, "classpath" ), new File( basedir, ".classpath" ) );
    }

    private void assertFileEquals( String mavenRepo, File expectedFile, File actualFile )
        throws IOException
    {
        List expectedLines = getLines( mavenRepo, expectedFile );

        List actualLines = getLines( mavenRepo, actualFile );

        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();

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
