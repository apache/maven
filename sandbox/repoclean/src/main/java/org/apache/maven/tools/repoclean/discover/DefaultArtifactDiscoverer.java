package org.apache.maven.tools.repoclean.discover;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.tools.repoclean.report.PathLister;
import org.apache.maven.tools.repoclean.report.Reporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

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
    extends AbstractArtifactDiscoverer
{

    private ArtifactFactory artifactFactory;

    public List discoverArtifacts( File repositoryBase, Reporter reporter, String blacklistedPatterns,
                                  PathLister excludeLister, PathLister kickoutLister )
        throws Exception
    {
        List artifacts = new ArrayList();

        String[] artifactPaths = scanForArtifactPaths( repositoryBase, blacklistedPatterns, excludeLister );

        for ( int i = 0; i < artifactPaths.length; i++ )
        {
            String path = artifactPaths[i];

            Artifact artifact = buildArtifact( repositoryBase, path, kickoutLister );

            if ( artifact != null )
            {
                artifacts.add( artifact );
            }
        }

        return artifacts;
    }

    private Artifact buildArtifact( File repositoryBase, String path, PathLister kickoutLister )
        throws Exception
    {
        Artifact result = null;

        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( path, "/" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );

        int currentPart = 0;

        //discard the actual artifact filename.
        pathParts.remove( 0 );

        // the next one is the version.
        String version = (String) pathParts.get( 0 );
        pathParts.remove( 0 );

        // the next one is the artifactId.
        String artifactId = (String) pathParts.get( 0 );
        pathParts.remove( 0 );

        // the remaining are the groupId.
        StringBuffer groupBuffer = new StringBuffer();

        boolean firstPart = true;
        for ( Iterator it = pathParts.iterator(); it.hasNext(); )
        {
            String part = (String) it.next();

            groupBuffer.append( part );

            if ( firstPart )
            {
                firstPart = false;
            }
            else if ( it.hasNext() )
            {
                groupBuffer.append( "." );
            }
        }

        result = artifactFactory.createArtifact( groupBuffer.toString(), artifactId, version, Artifact.SCOPE_RUNTIME,
                                                 "jar" );

        return result;
    }

}