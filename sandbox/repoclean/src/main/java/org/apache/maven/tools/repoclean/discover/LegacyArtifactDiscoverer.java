package org.apache.maven.tools.repoclean.discover;

/*
 * ==================================================================== Copyright 2001-2004 The
 * Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.construction.ArtifactConstructionSupport;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jdcasey
 */
public class LegacyArtifactDiscoverer
    extends AbstractLogEnabled
    implements ArtifactDiscoverer
{

    private ArtifactConstructionSupport artifactConstructionSupport = new ArtifactConstructionSupport();

    public List discoverArtifacts( File repositoryBase, Reporter reporter )
    {
        List artifacts = new ArrayList();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        scanner.setExcludes( new String[] {
            "bin/**",
            "reports/**",
            ".maven/**",
            "**/poms/*.pom",
            "**/*.md5",
            "**/*snapshot-version",
            "*/website/**",
            "*/licenses/**",
            "**/.htaccess",
            "**/REPOSITORY-V*.txt" } );

        scanner.scan();

        String[] artifactPaths = scanner.getIncludedFiles();

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = buildArtifact( path, reporter );
            if ( artifact != null )
            {
                artifacts.add( artifact );
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( String path, Reporter reporter )
    {
        Artifact result = null;

        // TODO: Need to add more test scenarios to the unit test for this
        // pattern.
        // I'm not convinced that this will catch everything.
        Pattern pathInfoPattern = Pattern.compile( "(.+)\\/(.+)s\\/([-a-zA-Z0-9]+)-([0-9]+[-.0-9a-zA-Z]+)\\..+" );

        Matcher matcher = pathInfoPattern.matcher( path );
        if ( !matcher.matches() )
        {
            reporter.info( "Artifact path: \'" + path
                + "\' does not match naming convention. Cannot reliably extract artifact information from path." );
        }
        else
        {
            String groupId = matcher.group( 1 );
            String type = matcher.group( 2 );
            String artifactId = matcher.group( 3 );
            String version = matcher.group( 4 );

            // Commenting this, since the old repo style didn't have a concept
            // of 'maven-plugin'...I've added an additional artifact handler
            // specifically for this, with just enough functionality to get the
            // pathing right.
            //if ( "plugin".equals( type ) )
            //{
            //    type = "maven-plugin";
            //}

            getLogger().debug(
                               "Extracted artifact information from path:\n" + "groupId: \'" + groupId + "\'\n"
                                   + "artifactId: \'" + artifactId + "\'\n" + "type: \'" + type + "\'\n"
                                   + "version: \'" + version + "\'" );

            result = artifactConstructionSupport.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME,
                                                                 type );
        }

        return result;
    }

}