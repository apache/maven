package org.apache.maven.tools.repoclean.discover;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.construction.ArtifactConstructionSupport;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author jdcasey
 */
public class LegacyArtifactDiscoverer
    extends AbstractLogEnabled
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

            Artifact artifact = buildArtifact( path, reporter );
            if ( artifact != null )
            {
                artifacts.add( artifact );
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( String path, Reporter reporter )
        throws Exception
    {
        StringTokenizer tokens = new StringTokenizer( path, "/\\" );

        int numberOfTokens = tokens.countTokens();

        if ( numberOfTokens != 3 )
        {
            reporter.info( "Artifact path: \'" + path
                + "\' does not match naming convention. Cannot reliably extract artifact information from path." );

            return null;
        }

        String groupId = tokens.nextToken();

        String type = tokens.nextToken();

        if ( type.endsWith( "s" ) )
        {
            type = type.substring( 0, type.length() - 1 );
        }

        // contains artifactId, version, classifier, and extension.
        String avceGlob = tokens.nextToken();

        LinkedList avceTokenList = new LinkedList();

        StringTokenizer avceTokenizer = new StringTokenizer( avceGlob, "-" );
        while ( avceTokenizer.hasMoreTokens() )
        {
            avceTokenList.addLast( avceTokenizer.nextToken() );
        }

        String lastAvceToken = (String) avceTokenList.removeLast();

        if ( lastAvceToken.endsWith( ".tar.gz" ) )
        {
            type = "distribution-tgz";

            lastAvceToken = lastAvceToken.substring( 0, lastAvceToken.length() - ".tar.gz".length() );

            avceTokenList.addLast( lastAvceToken );
        }
        else if ( lastAvceToken.endsWith( ".zip" ) )
        {
            type = "distribution-zip";

            lastAvceToken = lastAvceToken.substring( 0, lastAvceToken.length() - ".zip".length() );

            avceTokenList.addLast( lastAvceToken );
        }
        else
        {
            int extPos = lastAvceToken.lastIndexOf( '.' );

            if ( extPos > 0 )
            {
                lastAvceToken = lastAvceToken.substring( 0, extPos );
            }

            avceTokenList.addLast( lastAvceToken );
        }

        String validVersionParts = "([Dd][Ee][Vv][_.0-9]*)|" + "([Ss][Nn][Aa][Pp][Ss][Hh][Oo][Tt])|" + "([0-9][_.0-9a-zA-Z]*)|"
            + "([Gg]?[_.0-9ab]*([Pp][Rr][Ee]|[Rr][Cc]|[Gg]|[Mm])[_.0-9]*)|" + "([Aa][Ll][Pp][Hh][Aa][_.0-9]*)|"
            + "([Bb][Ee][Tt][Aa][_.0-9]*)|" + "([Rr][Cc][_.0-9]*)|" + "([Tt][Ee][Ss][Tt][_.0-9]*)|"
            + "([Dd][Ee][Bb][Uu][Gg][_.0-9]*)|" + "([Uu][Nn][Oo][Ff][Ff][Ii][Cc][Ii][Aa][Ll][_.0-9]*)|"
            + "([Cc][Uu][Rr][Rr][Ee][Nn][Tt])|" + "([Ll][Aa][Tt][Ee][Ss][Tt])|" + "([Ff][Cc][Ss])|"
            + "([Rr][Ee][Ll][Ee][Aa][Ss][Ee][_.0-9]*)|" + "([Nn][Ii][Gg][Hh][Tt][Ll][Yy])";

        // let's discover the version, and whatever's leftover will be either
        // a classifier, or part of the artifactId, depending on position.
        // Since version is at the end, we have to move in from the back.
        Collections.reverse( avceTokenList );

        String classifier = null;
        StringBuffer versionBuffer = new StringBuffer();

        boolean inFirstToken = true;
        for ( Iterator it = avceTokenList.iterator(); it.hasNext(); )
        {
            String token = (String) it.next();

            boolean tokenIsVersionPart = token.matches( validVersionParts );
            if ( inFirstToken && !tokenIsVersionPart )
            {
                classifier = token;
            }
            else if ( tokenIsVersionPart )
            {
                if ( !inFirstToken )
                {
                    versionBuffer.insert( 0, '-' );
                }

                versionBuffer.insert( 0, token );
            }
            else
            {
                // if we didn't find a version, but we did find a 'classifier', 
                // then push that classifier back onto the list...chances are, 
                // it doesn't have a version or a classifier if this is the case.
                if ( versionBuffer.length() < 1 && classifier != null )
                {
                    avceTokenList.addFirst( classifier );
                }

                // we've discovered all the version parts. break the loop.
                break;
            }

            if ( inFirstToken )
            {
                inFirstToken = false;
            }

            // pop the token off the list so it doesn't appear in the
            // artifactId.
            it.remove();
        }

        // Now, restore the proper ordering so we can build the artifactId.
        Collections.reverse( avceTokenList );

        StringBuffer artifactIdBuffer = new StringBuffer();

        inFirstToken = true;
        for ( Iterator it = avceTokenList.iterator(); it.hasNext(); )
        {
            String token = (String) it.next();

            if ( inFirstToken )
            {
                inFirstToken = false;
            }
            else
            {
                artifactIdBuffer.append( '-' );
            }

            artifactIdBuffer.append( token );
        }

        String artifactId = artifactIdBuffer.toString();
        String version = versionBuffer.toString();

        getLogger().debug(
                           "Extracted artifact information from path:\n" + "groupId: \'" + groupId + "\'\n"
                               + "artifactId: \'" + artifactId + "\'\n" + "type: \'" + type + "\'\n" + "version: \'"
                               + version + "\'\n" + "classifier: \'" + classifier + "\'" );

        if ( classifier != null )
        {
            return artifactConstructionSupport.createArtifactWithClassifier( groupId, artifactId, version,
                                                                             Artifact.SCOPE_RUNTIME, type, classifier );
        }
        else
        {
            return artifactConstructionSupport.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME,
                                                               type );
        }
    }

}