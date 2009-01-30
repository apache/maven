package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * @author Jason van Zyl
 * @author Kenney Westerhof
 */
public abstract class AbstractMavenIntegrationTestCase
    extends TestCase
{
    /**
     * Save System.out for progress reports etc.
     */
    private static PrintStream out = System.out;

    /**
     * The format for elapsed time.
     */
    private static final DecimalFormat SECS_FORMAT =
        new DecimalFormat( "(0.0 s)", new DecimalFormatSymbols( Locale.ENGLISH ) );

    /**
     * The zero-based column index where to print the test result.
     */
    private static final int RESULT_COLUMN = 60;
    
    private boolean skip;

    private ArtifactVersion mavenVersion;

    private VersionRange versionRange;

    private String matchPattern;

    protected AbstractMavenIntegrationTestCase()
    {
    }

    protected AbstractMavenIntegrationTestCase( String versionRangeStr )
    {
        this( versionRangeStr, "(.*?)-(RC[0-9]+|SNAPSHOT|RC[0-9]+-SNAPSHOT)" );
    }

    protected AbstractMavenIntegrationTestCase( String versionRangeStr, String matchPattern )
    {
        this.matchPattern = matchPattern;

        try
        {
            versionRange = VersionRange.createFromVersionSpec( versionRangeStr );
        }
        catch ( InvalidVersionSpecificationException e)
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRangeStr ).initCause( e );
        }

        ArtifactVersion version = getMavenVersion();
        if ( version != null )
        {
            skip = !versionRange.containsVersion( removePattern( version ) );
        }
        else
        {
            out.println( "WARNING: " + getITName() + ": version range '" + versionRange
                + "' supplied but no Maven version - not skipping test." );
        }
    }

    /**
     * Gets the Maven version used to run this test.
     * 
     * @return The Maven version or <code>null</code> if unknown.
     */
    private ArtifactVersion getMavenVersion()
    {
        if ( mavenVersion == null )
        {
            String v = System.getProperty( "maven.version" );
            // NOTE: If the version looks like "${...}" it has been configured from an undefined expression
            if ( v != null && v.length() > 0 && !v.startsWith( "${" ) )
            {
                mavenVersion = new DefaultArtifactVersion( v );
            }
        }
        return mavenVersion;
    }

    /**
     * This allows fine-grained control over execution of individual test methods
     * by allowing tests to adjust to the current maven version, or else simply avoid
     * executing altogether if the wrong version is present.
     */
    protected boolean matchesVersionRange( String versionRangeStr )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( versionRangeStr );
        }
        catch ( InvalidVersionSpecificationException e)
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRangeStr ).initCause( e );
        }

        ArtifactVersion version = getMavenVersion();
        if ( version != null )
        {
            return versionRange.containsVersion( removePattern( version ) );
        }
        else
        {
            out.println( "WARNING: " + getITName() + ": version range '" + versionRange
                + "' supplied but no Maven version found - returning true for match check." );

            return true;
        }
    }

    protected void runTest()
        throws Throwable
    {
        String line = getTestName() + "..";
        out.print( line );

        if ( skip )
        {
            out.println( pad( RESULT_COLUMN - line.length() ) + "SKIPPED - version " + getMavenVersion()
                + " not in range " + versionRange );
            return;
        }

        if ( "true".equals( System.getProperty( "useEmptyLocalRepository", "false" ) ) )
        {
            setupLocalRepo();
        }

        long milliseconds = System.currentTimeMillis();
        try
        {
            super.runTest();
            milliseconds = System.currentTimeMillis() - milliseconds;
            String result = "OK " + formatTime( milliseconds );
            out.println( pad( RESULT_COLUMN - line.length() ) + result );
        }
        catch ( Throwable t )
        {
            milliseconds = System.currentTimeMillis() - milliseconds;
            String result = "FAILURE " + formatTime( milliseconds );
            out.println( pad( RESULT_COLUMN - line.length() ) + result );
            throw t;
        }
    }

    private String getITName()
    {
        String simpleName = getClass().getName();
        int idx = simpleName.lastIndexOf( '.' );
        simpleName = idx >= 0 ? simpleName.substring( idx + 1 ) : simpleName;
        simpleName = simpleName.startsWith( "MavenIT" ) ? simpleName.substring( "MavenIT".length() ) : simpleName;
        simpleName = simpleName.endsWith( "Test" ) ? simpleName.substring( 0, simpleName.length() - 4 ) : simpleName;
        return simpleName;
    }

    private String getTestName()
    {
        String className = getITName();
        String methodName = getName();
        if ( methodName.startsWith( "test" ) )
        {
            methodName = methodName.substring( 4 );
        }
        return className + '(' + methodName + ')';
    }

    private String pad( int chars )
    {
        StringBuffer buffer = new StringBuffer( 128 );
        for ( int i = 0; i < chars; i++ )
        {
            buffer.append( '.' );
        }
        return buffer.toString();
    }

    private String formatTime( long milliseconds )
    {
        return SECS_FORMAT.format( milliseconds / 1000.0 );
    }

    protected File setupLocalRepo()
        throws IOException
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File localRepo = new File( tempDirPath, "local-repository/" + getITName() );
        if ( localRepo.isDirectory() )
        {
            FileUtils.deleteDirectory( localRepo );
        }

        System.setProperty( "maven.repo.local", localRepo.getAbsolutePath() );

        return localRepo;
    }

    protected ArtifactVersion removePattern( ArtifactVersion version )
    {
        String v = version.toString();

        Matcher m = Pattern.compile( matchPattern ).matcher( v );

        if ( m.matches() )
        {
            return new DefaultArtifactVersion( m.group( 1 ) );
        }
        return version;
    }
}
