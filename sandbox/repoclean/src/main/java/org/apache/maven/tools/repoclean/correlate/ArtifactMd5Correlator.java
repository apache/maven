package org.apache.maven.tools.repoclean.correlate;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.tools.repoclean.digest.ArtifactDigestException;
import org.apache.maven.tools.repoclean.digest.ArtifactDigestor;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author jdcasey
 */
public class ArtifactMd5Correlator
{

    public static final String ROLE = ArtifactMd5Correlator.class.getName();

    private ArtifactDigestor artifactDigestor;

    public void correlateArtifactsToMd5( File repositoryBase, String[] artifacts, Reporter reporter, boolean reportOnly )
    {
        reporter.info( "Starting artifact-to-MD5 correlation." );

        List md5s = scanMd5s( repositoryBase );

        for ( int i = 0; i < artifacts.length; i++ )
        {
            String artifact = artifacts[i];

            String md5 = artifact + ".md5";

            if ( !md5s.contains( md5 ) )
            {
                reporter.warn( "Cannot find digest file for artifact: \'" + artifact + "\'." );

                if ( !reportOnly )
                {
                    reporter.info( "Creating digest file: \'" + md5 + "\'" );

                    File artifactFile = new File( repositoryBase, artifact );
                    File md5File = new File( repositoryBase, md5 );
                    try
                    {
                        artifactDigestor.createArtifactDigest( artifactFile, md5File, ArtifactDigestor.MD5 );
                    }
                    catch ( ArtifactDigestException e )
                    {
                        reporter.error( "Error creating digest for artifact: \'" + artifact + "\'", e );
                    }
                }
            }
        }

        reporter.info( "Finished artifact-to-MD5 correlation." );
    }

    private List scanMd5s( File repositoryBase )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        scanner.setIncludes( new String[] { "**/*.md5" } );

        scanner.scan();

        String[] md5s = scanner.getIncludedFiles();

        return Arrays.asList( md5s );
    }

}