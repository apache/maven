package org.apache.maven.tools.repoclean.discover;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.construction.ArtifactConstructionSupport;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * @author jdcasey
 */
public class DefaultArtifactDiscoverer
    implements ArtifactDiscoverer
{

    private ArtifactConstructionSupport artifactConstructionSupport = new ArtifactConstructionSupport();

    public List discoverArtifacts( File repositoryBase, Reporter reporter )
        throws Exception
    {
        List artifacts = new ArrayList();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        scanner.setExcludes( STANDARD_DISCOVERY_EXCLUDES );

        scanner.scan();

        String[] artifactPaths = scanner.getIncludedFiles();

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = buildArtifact( repositoryBase, path, reporter );

            if ( artifact != null )
            {
                artifacts.add( artifact );
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( File repositoryBase, String path, Reporter reporter )
        throws Exception
    {
        Artifact result = null;

        int lastDot = path.lastIndexOf( '.' );

        if ( lastDot < 0 )
        {
            reporter.error( "Found potential artifact file with invalid name. Path: \'" + path
                + "\' doesn't seem to contain a file extension." );
        }
        else
        {
            String pomPath = path.substring( 0, lastDot ) + ".pom";

            File pomFile = new File( repositoryBase, pomPath );
            if ( pomFile.exists() )
            {
                FileReader pomReader = null;
                try
                {
                    pomReader = new FileReader( pomFile );
                    MavenXpp3Reader modelReader = new MavenXpp3Reader();

                    Model model = modelReader.read( pomReader );

                    result = artifactConstructionSupport.createArtifact( model.getGroupId(), model.getArtifactId(),
                                                                         model.getVersion(), Artifact.SCOPE_RUNTIME,
                                                                         model.getPackaging() );
                }
                finally
                {
                    IOUtil.close( pomReader );
                }
            }
            else
            {
                reporter.error( "POM not found for potential artifact at \'" + path
                    + "\'. Cannot create Artifact instance." );
            }
        }

        return result;
    }

}