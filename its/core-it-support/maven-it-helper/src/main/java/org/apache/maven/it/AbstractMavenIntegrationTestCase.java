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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.opentest4j.TestAbortedException;

/**
 * @author Jason van Zyl
 * @author Kenney Westerhof
 */
public abstract class AbstractMavenIntegrationTestCase
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

    private BrokenMavenVersionException invert;

    private static ArtifactVersion javaVersion;

    private ArtifactVersion mavenVersion;

    private VersionRange versionRange;

    private String matchPattern;

    private String testName;

    private static final String DEFAULT_MATCH_PATTERN = "(.*?)-(RC[0-9]+|SNAPSHOT|RC[0-9]+-SNAPSHOT)";

    protected static final String ALL_MAVEN_VERSIONS = "[2.0,)";

    protected AbstractMavenIntegrationTestCase( String versionRangeStr )
    {
        this( versionRangeStr, DEFAULT_MATCH_PATTERN );
    }

    protected AbstractMavenIntegrationTestCase( String versionRangeStr, String matchPattern )
    {
        this.matchPattern = matchPattern;

        requiresMavenVersion( versionRangeStr );
    }


    @BeforeAll
    static void setupInputStream()
    {
        if ( !( System.in instanceof NonCloseableInputStream ) )
        {
            System.setIn( new NonCloseableInputStream( System.in ) );
        }
    }

    /**
     * Gets the Java version used to run this test.
     *
     * @return The Java version, never <code>null</code>.
     */
    private ArtifactVersion getJavaVersion()
    {
        if ( javaVersion == null )
        {
            String version = System.getProperty( "java.version" );
            version = version.replaceAll( "[_-]", "." );
            Matcher matcher = Pattern.compile( "(?s).*?(([0-9]+\\.[0-9]+)(\\.[0-9]+)?).*" ).matcher( version );
            if ( matcher.matches() )
            {
                version = matcher.group( 1 );
            }
            javaVersion = new DefaultArtifactVersion( version );
        }
        return javaVersion;
    }

    /**
     * Gets the Maven version used to run this test.
     *
     * @return The Maven version or <code>null</code> if unknown.
     */
    protected final ArtifactVersion getMavenVersion()
    {
        if ( mavenVersion == null )
        {
            String version = System.getProperty( "maven.version", "" );

            if ( version.length() <= 0 || version.startsWith( "${" ) )
            {
                try
                {
                    Verifier verifier = new Verifier( "" );
                    version = verifier.getMavenVersion();
                    System.setProperty( "maven.version", version );
                }
                catch ( VerificationException e )
                {
                    e.printStackTrace();
                }
            }

            // NOTE: If the version looks like "${...}" it has been configured from an undefined expression
            if ( version != null && version.length() > 0 && !version.startsWith( "${" ) )
            {
                mavenVersion = new DefaultArtifactVersion( version );
            }
        }
        return mavenVersion;
    }

    /**
     * This allows fine-grained control over execution of individual test methods
     * by allowing tests to adjust to the current Maven version, or else simply avoid
     * executing altogether if the wrong version is present.
     */
    protected boolean matchesVersionRange( String versionRangeStr )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( versionRangeStr );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRangeStr, e );
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

    /**
     * Can be called by version specific setUp calls
     *
     * @return
     */
    protected final boolean isSkipped()
    {
        return skip;
    }

    protected void runTest()
        throws Throwable
    {
        String testName = getTestName();

        if ( testName.startsWith( "mng" ) || Character.isDigit( testName.charAt( 0 ) ) )
        {
            int mng = 4;
            while ( Character.isDigit( testName.charAt( mng ) ) )
            {
                mng++;
            }
            out.print( AnsiSupport.bold( testName.substring( 0, mng ) ) );
            out.print( ' ' );
            out.print( testName.substring( mng ) );
        }
        else
        {
            int index = testName.indexOf( ' ' );
            if ( index == -1 )
            {
                out.print( testName );
            }
            else
            {
                out.print( AnsiSupport.bold( testName.substring( 0, index ) ) );
                out.print( testName.substring( index ) );
            }
            out.print( '.' );
        }

        out.print( pad( RESULT_COLUMN - testName.length() ) );
        out.print( ' ' );

        if ( skip )
        {
            out.println( AnsiSupport.warning( "SKIPPED" ) + " - Maven version " + getMavenVersion() + " not in range "
                + versionRange );
            return;
        }

        if ( "true".equals( System.getProperty( "useEmptyLocalRepository", "false" ) ) )
        {
            setupLocalRepo();
        }

        invert = null;
        long milliseconds = System.currentTimeMillis();
        try
        {
            // TODO: JUNIT5
            //super.runTest();
            milliseconds = System.currentTimeMillis() - milliseconds;
            if ( invert != null )
            {
                throw invert;
            }
            out.println( AnsiSupport.success( "OK" ) + " " + formatTime( milliseconds ) );
        }
        catch ( UnsupportedJavaVersionException e )
        {
            out.println( AnsiSupport.warning( "SKIPPED" ) + " - Java version " + e.javaVersion + " not in range "
                + e.supportedRange );
            return;
        }
        catch ( UnsupportedMavenVersionException e )
        {
            out.println( AnsiSupport.warning( "SKIPPED" ) + " - Maven version " + e.mavenVersion + " not in range "
                + e.supportedRange );
            return;
        }
        catch ( BrokenMavenVersionException e )
        {
            out.println( AnsiSupport.error( "UNEXPECTED OK" ) + " - Maven version " + e.mavenVersion
                + " expected to fail " + formatTime( milliseconds ) );
            fail( "Expected failure when with Maven version " + e.mavenVersion );
        }
        catch ( Throwable t )
        {
            milliseconds = System.currentTimeMillis() - milliseconds;
            if ( invert != null )
            {
                out.println( AnsiSupport.success( "EXPECTED FAIL" ) + " - Maven version " + invert.mavenVersion
                    + " expected to fail " + formatTime( milliseconds ) );
            }
            else
            {
                out.println( AnsiSupport.error( "FAILURE" ) + " " + formatTime( milliseconds ) );
                throw t;
            }
        }
    }

    /**
     * Guards the execution of a test case by checking that the current Java version matches the specified version
     * range. If the check fails, an exception will be thrown which aborts the current test and marks it as skipped. One
     * would usually call this method right at the start of a test method.
     *
     * @param versionRange The version range that specifies the acceptable Java versions for the test, must not be
     *                     <code>null</code>.
     */
    protected void requiresJavaVersion( String versionRange )
    {
        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( versionRange );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRange, e );
        }

        ArtifactVersion version = getJavaVersion();
        if ( !range.containsVersion( version ) )
        {
            throw new UnsupportedJavaVersionException( version, range );
        }
    }

    /**
     * Guards the execution of a test case by checking that the current Maven version matches the specified version
     * range. If the check fails, an exception will be thrown which aborts the current test and marks it as skipped. One
     * would usually call this method right at the start of a test method.
     *
     * @param versionRange The version range that specifies the acceptable Maven versions for the test, must not be
     *                     <code>null</code>.
     */
    protected void requiresMavenVersion( String versionRange )
    {
        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( versionRange );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRange, e );
        }

        ArtifactVersion version = getMavenVersion();
        if ( version != null )
        {
            if ( !range.containsVersion( removePattern( version ) ) )
            {
                throw new UnsupportedMavenVersionException( version, range );
            }
        }
        else
        {
            out.println( "WARNING: " + getITName() + ": version range '" + versionRange
                             + "' supplied but no Maven version found - not skipping test." );
        }
    }

    /**
     * Inverts the execution of a test case for cases where we discovered a bug in the test case, have corrected the
     * test case and shipped versions of Maven with a bug because of the faulty test case. This method allows the
     * tests to continue passing against the historical releases as they historically would, as well as verifying that
     * the test is no longer providing a false positive.
     *
     * @param versionRange
     */
    protected void failingMavenVersions( String versionRange )
    {
        assertNull( "Only call failingMavenVersions at most once per test", invert );
        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( versionRange );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw (RuntimeException) new IllegalArgumentException( "Invalid version range: " + versionRange, e );
        }

        ArtifactVersion version = getMavenVersion();
        if ( version != null )
        {
            if ( range.containsVersion( removePattern( version ) ) )
            {
                invert = new BrokenMavenVersionException( version, range );
            }
        }
        else
        {
            out.println( "WARNING: " + getITName() + ": version range '" + versionRange
                             + "' supplied but no Maven version found - not marking test as expected to fail." );
        }
    }

    private static class NonCloseableInputStream extends FilterInputStream
    {
        NonCloseableInputStream( InputStream delegate )
        {
            super( delegate );
        }

        @Override
        public void close() throws IOException
        {
        }
    }

    private class UnsupportedJavaVersionException
        extends TestAbortedException
    {
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public ArtifactVersion javaVersion;

        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public VersionRange supportedRange;

        private UnsupportedJavaVersionException( ArtifactVersion javaVersion, VersionRange supportedRange )
        {
            super( "Java version " + javaVersion + " not in range " + supportedRange );
            this.javaVersion = javaVersion;
            this.supportedRange = supportedRange;
        }

    }

    private class UnsupportedMavenVersionException
            extends TestAbortedException
    {
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public ArtifactVersion mavenVersion;

        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public VersionRange supportedRange;

        private UnsupportedMavenVersionException( ArtifactVersion mavenVersion, VersionRange supportedRange )
        {
            super( "Maven version " + mavenVersion + " not in range " + supportedRange );
            this.mavenVersion = mavenVersion;
            this.supportedRange = supportedRange;
        }

    }

    private class BrokenMavenVersionException
        extends RuntimeException
    {
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public ArtifactVersion mavenVersion;

        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public VersionRange supportedRange;

        private BrokenMavenVersionException( ArtifactVersion mavenVersion, VersionRange supportedRange )
        {
            this.mavenVersion = mavenVersion;
            this.supportedRange = supportedRange;
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
        return className + '.' + methodName + "()";
    }

    private String pad( int chars )
    {
        StringBuilder buffer = new StringBuilder( 128 );
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
        String tempDirPath = System.getProperty( "maven.it.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File localRepo = new File( tempDirPath, "local-repository/" + getITName() );
        if ( localRepo.isDirectory() )
        {
            FileUtils.deleteDirectory( localRepo );
        }

        System.setProperty( "maven.repo.local", localRepo.getAbsolutePath() );

        return localRepo;
    }

    ArtifactVersion removePattern( ArtifactVersion version )
    {
        String v = version.toString();

        Matcher m = Pattern.compile( matchPattern ).matcher( v );

        if ( m.matches() )
        {
            return new DefaultArtifactVersion( m.group( 1 ) );
        }
        return version;
    }

    protected Verifier newVerifier( String basedir )
        throws VerificationException
    {
        return newVerifier( basedir, false );
    }

    protected Verifier newVerifier( String basedir, String settings )
        throws VerificationException
    {
        return newVerifier( basedir, settings, false );
    }

    protected Verifier newVerifier( String basedir, boolean debug )
        throws VerificationException
    {
        return newVerifier( basedir, "", debug );
    }

    protected Verifier newVerifier( String basedir, String settings, boolean debug )
        throws VerificationException
    {
        Verifier verifier = new Verifier( basedir, debug );

        verifier.setAutoclean( false );

        if ( settings != null )
        {
            File settingsFile;
            if ( settings.length() > 0 )
            {
                settingsFile = new File( "settings-" + settings + ".xml" );
            }
            else
            {
                settingsFile = new File( "settings.xml" );
            }

            if ( !settingsFile.isAbsolute() )
            {
                String settingsDir = System.getProperty( "maven.it.global-settings.dir", "" );
                if ( settingsDir.length() > 0 )
                {
                    settingsFile = new File( settingsDir, settingsFile.getPath() );
                }
                else
                {
                    //
                    // Make is easier to run ITs from m2e in Maven IT mode without having to set any additional
                    // properties.
                    //
                    settingsFile = new File( "target/test-classes", settingsFile.getPath() );
                }
            }

            String path = settingsFile.getAbsolutePath();

            // dedicated CLI option only available since MNG-3914
            if ( matchesVersionRange( "[2.1.0,)" ) )
            {
                verifier.addCliOption( "--global-settings" );
                if ( path.indexOf( ' ' ) < 0 )
                {
                    verifier.addCliOption( path );
                }
                else
                {
                    verifier.addCliOption( '"' + path + '"' );
                }
            }
            else
            {
                verifier.getSystemProperties().put( "org.apache.maven.global-settings", path );
            }
        }

        try
        {
            // Java7 TLS protocol
            if ( VersionRange.createFromVersionSpec( "(,1.8.0)" ).containsVersion( getJavaVersion() ) )
            {
                verifier.addCliOption( "-Dhttps.protocols=TLSv1.2" );
            }

            // auto set source+target to lowest reasonable java version
            // Java9 requires at least 1.6
            if ( VersionRange.createFromVersionSpec( "[9,12)" ).containsVersion( getJavaVersion() ) )
            {
                verifier.getSystemProperties().put( "maven.compiler.source", "1.8" );
                verifier.getSystemProperties().put( "maven.compiler.target", "1.8" );
                verifier.getSystemProperties().put( "maven.compiler.release", "8" );
            }
            // Java12 requires at least 7
            if ( VersionRange.createFromVersionSpec( "[12,)" ).containsVersion( getJavaVersion() ) )
            {
                verifier.getSystemProperties().put( "maven.compiler.source", "8" );
                verifier.getSystemProperties().put( "maven.compiler.target", "8" );
                verifier.getSystemProperties().put( "maven.compiler.release", "8" );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // noop
        }

        return verifier;
    }

    @BeforeEach
    void setupContext( TestInfo testInfo )
    {
        testName = testInfo.getTestMethod().get().getName();
    }

    protected String getName()
    {
        return testName;
    }

    public static void assertCanonicalFileEquals( String message, File expected, File actual )
        throws IOException
    {
        assertEquals( message, expected.getCanonicalFile(), actual.getCanonicalFile() );
    }

    public static void assertCanonicalFileEquals( File expected, File actual )
        throws IOException
    {
        assertCanonicalFileEquals( null, expected, actual );
    }

    public static void assertCanonicalFileEquals( String message, String expected, String actual )
        throws IOException
    {
        assertCanonicalFileEquals( message, new File( expected ), new File( actual ) );
    }

    public static void assertCanonicalFileEquals( String expected, String actual )
        throws IOException
    {
        assertCanonicalFileEquals( null, new File( expected ), new File( actual ) );
    }

    public static void assertEquals( Object o1, Object o2 )
    {
        assertEquals( null, o1, o2 );
    }

    public static void assertEquals( String message, Object o1, Object o2 )
    {
        org.junit.jupiter.api.Assertions.assertEquals( o1, o2, message );
    }

    public static void assertNotEquals( Object o1, Object o2 )
    {
        assertNotEquals( null, o1, o2 );
    }

    public static void assertNotEquals( String message, Object o1, Object o2 )
    {
        org.junit.jupiter.api.Assertions.assertNotEquals( o1, o2, message );
    }

    public static void assertTrue( boolean test )
    {
        assertTrue( null, test );
    }

    public static void assertTrue( String message, boolean test )
    {
        org.junit.jupiter.api.Assertions.assertTrue( test, message );
    }

    public static void assertFalse( boolean test )
    {
        assertFalse( null, test );
    }

    public static void assertFalse( String message, boolean test )
    {
        org.junit.jupiter.api.Assertions.assertFalse( test, message );
    }

    public static void assertNotNull( Object o )
    {
        assertNotNull( null, o );
    }

    public static void assertNotNull( String message, Object o )
    {
        org.junit.jupiter.api.Assertions.assertNotNull( o, message );
    }

    public static void assertNull( Object o )
    {
        assertNull( null, o );
    }

    public static void assertNull( String message, Object o )
    {
        org.junit.jupiter.api.Assertions.assertNull( o, message );
    }

    public static void fail( String message )
    {
        org.junit.jupiter.api.Assertions.fail( message );
    }

}
