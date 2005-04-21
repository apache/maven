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

import org.apache.maven.tools.repoclean.report.ReportWriteException;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.apache.maven.tools.repoclean.transaction.RewriteTransaction;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class DigestVerifier
{

    public static final String ROLE = DigestVerifier.class.getName();

    private Digestor artifactDigestor;
    
    public void setArtifactDigestor(Digestor artifactDigestor)
    {
        this.artifactDigestor = artifactDigestor;
    }

    public void verifyDigest( File source, File target, RewriteTransaction transaction, Reporter reporter, boolean reportOnly )
        throws DigestException, ReportWriteException, IOException
    {
        verifyDigestFile( source, target, transaction, reporter, reportOnly, ".md5", Digestor.MD5 );
        
        verifyDigestFile( source, target, transaction, reporter, reportOnly, ".sha1", Digestor.SHA );
    }

    private void verifyDigestFile( File artifactSource, File artifactTarget, RewriteTransaction transaction, Reporter reporter, boolean reportOnly,
                                  String digestExt, String digestAlgorithm )
        throws DigestException, ReportWriteException, IOException
    {
        // create the digest source file from which to copy/verify.
        File digestSourceFile = new File( artifactSource + digestExt );

        // create the digest target file from which to copy/create.
        File digestTargetFile = new File( artifactTarget + digestExt );
        
        transaction.addFile( digestTargetFile );

        boolean verified = false;

        // if the digest source file exists, then verify it.
        if ( digestSourceFile.exists() )
        {
            verified = artifactDigestor.verifyArtifactDigest( artifactTarget, digestTargetFile, digestAlgorithm );

            if ( verified )
            {
                if ( !reportOnly )
                {
                    try
                    {
                        FileUtils.copyFile( digestSourceFile, digestTargetFile );
                    }
                    catch ( IOException e )
                    {
                        reporter.error( "Cannot copy digest file for path [" + artifactSource
                            + "] from source to target for digest algorithm: \'" + digestAlgorithm + "\'.", e );

                        throw e;
                    }
                }
            }
            else
            {
                reporter.warn( digestExt + " for path [" + artifactSource + "] in target repository is wrong." );
            }
        }
        else
        {
            reporter.warn( digestExt + " for path [" + artifactSource + "] is missing in source repository." );
        }

        // if the .md5 was missing or did not verify correctly, create a new one
        // in the target repo.
        if ( !verified )
        {
            if ( !reportOnly )
            {
                artifactDigestor.createArtifactDigest( artifactTarget, digestTargetFile, digestAlgorithm );
            }
        }
    }

}