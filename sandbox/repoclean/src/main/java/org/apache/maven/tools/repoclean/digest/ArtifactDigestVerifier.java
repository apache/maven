package org.apache.maven.tools.repoclean.digest;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class ArtifactDigestVerifier
{

    public static final String ROLE = ArtifactDigestVerifier.class.getName();

    private ArtifactDigestor artifactDigestor;

    public void verifyDigest( Artifact artifact, File artifactTarget, Reporter reporter, boolean reportOnly ) throws Exception
    {
        // create the digest source file from which to copy/verify.
        File digestSourceFile = new File( artifact.getFile() + ".md5" );

        // create the digest target file from which to copy/create.
        File digestTargetFile = new File( artifactTarget + ".md5" );
        
        if(!reportOnly)
        {
            File targetParent = digestTargetFile.getParentFile();

            if ( !targetParent.exists() )
            {
                reporter.info( "MD5 parent directory \'" + targetParent + "\' does not exist. Creating..." );
                targetParent.mkdirs();
            }
        }

        boolean verified = false;

        // if the digest source file exists, then verify it.
        if ( digestSourceFile.exists() )
        {
            verified = artifactDigestor.verifyArtifactDigest( artifactTarget, digestTargetFile,
                                                              ArtifactDigestor.MD5 );

            if ( verified )
            {
                reporter.info( "Source digest file for artifact[" + artifact.getId()
                    + "] is okay, so we'll just copy it." );

                if ( !reportOnly )
                {
                    try
                    {
                        FileUtils.copyFile( digestSourceFile, digestTargetFile );
                    }
                    catch ( IOException e )
                    {
                        reporter.error( "Cannot copy digest file for artifact[" + artifact.getId()
                            + "] from source to target.", e );

                        throw e;
                    }
                }
                else
                {
                    reporter.info( "Skipping transfer of valid MD5 digest file (we're in report-only mode)." );
                }
            }
            else
            {
                reporter.warn( ".md5 for artifact[" + artifact.getId() + "] in target repository is wrong." );
            }
        }
        else
        {
            reporter.warn( ".md5 for artifact[" + artifact.getId() + "] is missing in source repository." );
        }

        // if the .md5 was missing or did not verify correctly, create a new one
        // in the target repo.
        if ( !verified )
        {
            reporter.info( "Creating .md5 for artifact[" + artifact.getId() + "] in target repository." );

            if ( !reportOnly )
            {
                artifactDigestor.createArtifactDigest( artifactTarget, digestTargetFile, ArtifactDigestor.MD5 );
            }
            else
            {
                reporter.info( "Skipping creation of MD5 digest for artifact (we're in report-only mode)." );
            }
        }
    }

}