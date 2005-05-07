package org.apache.maven.tools.repoclean.discover;

import org.apache.maven.tools.repoclean.report.PathLister;
import org.apache.maven.tools.repoclean.report.ReportWriteException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;

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

public abstract class AbstractArtifactDiscoverer
    extends AbstractLogEnabled
    implements ArtifactDiscoverer
{

    protected String[] scanForArtifactPaths( File repositoryBase, String blacklistedPatterns, PathLister excludesLister )
        throws ReportWriteException
    {
        String[] blacklisted = null;
        if ( blacklistedPatterns != null && blacklistedPatterns.length() > 0 )
        {
            blacklisted = blacklistedPatterns.split( "," );
        }
        else
        {
            blacklisted = new String[0];
        }

        String[] allExcludes = new String[STANDARD_DISCOVERY_EXCLUDES.length + blacklisted.length];

        System.arraycopy( STANDARD_DISCOVERY_EXCLUDES, 0, allExcludes, 0, STANDARD_DISCOVERY_EXCLUDES.length );
        System.arraycopy( blacklisted, 0, allExcludes, STANDARD_DISCOVERY_EXCLUDES.length, blacklisted.length );

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        scanner.setExcludes( allExcludes );

        scanner.scan();

        String[] artifactPaths = scanner.getIncludedFiles();

        String[] excludedPaths = scanner.getExcludedFiles();

        for ( int i = 0; i < excludedPaths.length; i++ )
        {
            String excludedPath = excludedPaths[i];
            excludesLister.addPath( excludedPath );
        }

        return artifactPaths;
    }

}
