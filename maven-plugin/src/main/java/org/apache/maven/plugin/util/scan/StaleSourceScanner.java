package org.apache.maven.plugin.util.scan;

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

import org.apache.maven.plugin.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author jdcasey
 */
public class StaleSourceScanner
    extends AbstractSourceInclusionScanner
{

    private final long lastUpdatedWithinMsecs;

    private final Set sourceIncludes;

    private final Set sourceExcludes;

    public StaleSourceScanner( long lastUpdatedWithinMsecs )
    {
        this( lastUpdatedWithinMsecs, Collections.singleton("**/*"), Collections.EMPTY_SET );
    }

    public StaleSourceScanner()
    {
        this( 0, Collections.singleton("**/*"), Collections.EMPTY_SET );
    }

    public StaleSourceScanner( long lastUpdatedWithinMsecs, Set sourceIncludes, Set sourceExcludes )
    {
        this.lastUpdatedWithinMsecs = lastUpdatedWithinMsecs;

        this.sourceIncludes = sourceIncludes;
        this.sourceExcludes = sourceExcludes;
    }

    public Set getIncludedSources( File sourceDir, File targetDir )
        throws InclusionScanException
    {
        Set matchingSources = new HashSet();

        List srcMappings = getSourceMappings();

        String[] potentialIncludes = scanForSources( sourceDir );
        for ( int i = 0; i < potentialIncludes.length; i++ )
        {
            String path = potentialIncludes[i];

            File sourceFile = new File( sourceDir, path );

            staleSourceFileTesting: for ( Iterator patternIt = srcMappings.iterator(); patternIt.hasNext(); )
            {
                SourceMapping mapping = (SourceMapping) patternIt.next();

                Set targetFiles = mapping.getTargetFiles( targetDir, path );
                
                // never include files that don't have corresponding target mappings.
                // the targets don't have to exist on the filesystem, but the 
                // mappers must tell us to look for them.
                for ( Iterator targetIt = targetFiles.iterator(); targetIt.hasNext(); )
                {
                    File targetFile = (File) targetIt.next();

                    if ( !targetFile.exists()
                        || ( targetFile.lastModified() + lastUpdatedWithinMsecs < sourceFile.lastModified() ) )
                    {
                        matchingSources.add( sourceFile );
                        break staleSourceFileTesting;
                    }
                }
            }
        }

        return matchingSources;
    }

    private String[] scanForSources( File sourceDir )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setFollowSymlinks(true);
        ds.setBasedir( sourceDir );

        String[] includes = null;
        if ( sourceIncludes.isEmpty() )
        {
            includes = new String[0];
        }
        else
        {
            includes = (String[]) sourceIncludes.toArray( new String[sourceIncludes.size()] );
        }

        ds.setIncludes( includes );

        String[] excludes = null;
        if ( sourceExcludes.isEmpty() )
        {
            excludes = new String[0];
        }
        else
        {
            excludes = (String[]) sourceExcludes.toArray( new String[sourceExcludes.size()] );
        }

        ds.setExcludes( excludes );

        ds.scan();

        return ds.getIncludedFiles();
    }

}