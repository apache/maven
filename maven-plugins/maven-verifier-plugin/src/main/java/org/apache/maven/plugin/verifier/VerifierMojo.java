package org.apache.maven.plugin.verifier;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.verifier.model.Verifications;
import org.apache.maven.plugin.verifier.model.io.xpp3.VerificationsXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies existence or non-existence of files/directories an optionally checks file content against a regexp.
 *
 * @goal verify
 * @phase integration-test
 *
 * @author <a href="vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class VerifierMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${basedir}"
     * @required
     */
    private String basedir;

    /**
     * @parameter expression="${basedir}/src/test/verifier/verifications.xml"
     * @required
     */
    private File verificationFile;

    /**
     * @required
     */
    private boolean failOnError = true;

    private VerificationResultPrinter resultPrinter = new ConsoleVerificationResultPrinter( getLog() );

    public void execute()
        throws MojoExecutionException
    {
        VerificationResult results = verify();
        this.resultPrinter.print( results );

        // Fail the build if there are errors
        if ( this.failOnError && results.hasFailures() )
        {
            throw new MojoExecutionException( "There are test failures" );
        }
    }

    /**
     * @param file the file path of the file to check (can be relative or absolute). If relative
     *             the project's basedir will be prefixed.
     * @return the absolute file path of the file to check
     */
    protected File getAbsoluteFileToCheck( File file )
    {
        File result = file;
        if ( !file.isAbsolute() )
        {
            result = new File( new File( this.basedir ), file.getPath() );
        }
        return result;
    }

    private VerificationResult verify()
        throws MojoExecutionException
    {
        VerificationResult results = new VerificationResult();

        Reader reader = null;
        try
        {
            reader = new FileReader( this.verificationFile );

            VerificationsXpp3Reader xppReader = new VerificationsXpp3Reader();
            Verifications verifications = xppReader.read( reader );

            for ( Iterator i = verifications.getFiles().iterator(); i.hasNext(); )
            {
                org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) i.next();

                // Transform the file to check into an absolute path prefixing the basedir if
                // the location is relative
                if ( file.getLocation() != null )
                {
                    file.setLocation( getAbsoluteFileToCheck( new File( file.getLocation() ) ).getPath() );
                    verifyFile( file, results );
                }
                else
                {
                    throw new MojoExecutionException( "Missing <location> element" );
                }
            }
        }
        catch ( org.codehaus.plexus.util.xml.pull.XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error while verifying files", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error while verifying files", e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return results;
    }

    private boolean verifyFile( org.apache.maven.plugin.verifier.model.File fileCheck, VerificationResult results )
        throws IOException
    {
        boolean result;

        result = verifyFileExistence( fileCheck, results );
        if ( result && fileCheck.getContains() != null )
        {
            result = result && verifyFileContent( fileCheck, results );
        }

        return result;
    }

    private boolean verifyFileContent( org.apache.maven.plugin.verifier.model.File fileCheck,
                                       VerificationResult results )
        throws IOException
    {
        boolean result = false;

        Pattern pattern = Pattern.compile( fileCheck.getContains() );

        // Note: Very inefficient way as we load the whole file in memory. If you have a better 
        // idea, please submit it!
        Matcher matcher = pattern.matcher( FileUtils.fileRead( new File( fileCheck.getLocation() ) ) );

        if ( matcher.find() )
        {
            result = true;
        }
        else
        {
            results.addContentFailure( fileCheck );
        }

        return result;
    }

    private boolean verifyFileExistence( org.apache.maven.plugin.verifier.model.File fileCheck,
                                         VerificationResult results )
    {
        boolean result = false;

        File physicalFile = new File( fileCheck.getLocation() );
        if ( fileCheck.isExists() )
        {
            result = physicalFile.exists();
            if ( !result )
            {
                results.addExistenceFailure( fileCheck );
            }
        }
        else
        {
            result = !physicalFile.exists();
            if ( !result )
            {
                results.addNonExistenceFailure( fileCheck );
            }
        }

        return result;
    }

    public void setBaseDir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setVerificationFile( File file )
    {
        this.verificationFile = file;
    }

    public void setVerificationResultPrinter( VerificationResultPrinter printer )
    {
        this.resultPrinter = printer;
    }

    public void setFailOnError( boolean failOnError )
    {
        this.failOnError = failOnError;
    }
}
