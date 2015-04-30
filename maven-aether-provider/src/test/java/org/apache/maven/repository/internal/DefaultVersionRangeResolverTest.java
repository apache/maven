package org.apache.maven.repository.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Iterator;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

/**
 * Tests the {@link DefaultVersionRangeResolver} based on 'virtual' repository data stored at
 * {@literal /maven-aether-provider/src/test/resources/repo/org/apache/maven/its/mng-3092/maven-metadata.xml}
 * <p>
 * Note: Information about the version scheme: {@link org.eclipse.aether.util.version.GenericVersionScheme}.<br/>
 * Design document for dependency version ranges: <a
 * href="https://cwiki.apache.org/confluence/display/MAVENOLD/Dependency+Mediation+and+Conflict+Resolution"
 * >https://cwiki.apache.org/confluence/display/MAVENOLD/Dependency+Mediation+and+Conflict+Resolution</a>
 * </p>
 */
public class DefaultVersionRangeResolverTest
        extends AbstractRepositoryTestCase
{
    private final VersionScheme versionScheme = new GenericVersionScheme();

    private DefaultVersionRangeResolver sut;

    private VersionRangeRequest request;

    @Override
    protected void setUp()
            throws Exception
    {
        super.setUp();
        // be sure we're testing the right class, i.e. DefaultVersionRangeResolver.class
        sut = ( DefaultVersionRangeResolver ) lookup( VersionRangeResolver.class, "default" );
        request = new VersionRangeRequest();
        request.addRepository( newTestRepository() );
    }

    @Override
    protected void tearDown()
            throws Exception
    {
        sut = null;
        super.tearDown();
    }

    /**
     * Test resolves version range {@code (,2.0.0]} (x &lt;= 2.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.0-SNAPSHOT} and ends with the highest
     * inclusive version {@code 2.0.0}.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftOpenRightInclusive()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.0-SNAPSHOT" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0" );
        final Version expectedReleaseVersion = versionScheme.parseVersion( "1.3.0" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.2.1-SNAPSHOT" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "(,2.0.0]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 34, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedReleaseVersion ) );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
    }

    /**
     * Test resolves version range {@code 1.2.0}.
     * <p>
     * The passed value is a 'soft' requirement on {@code 1.2.0} and <b>not</b> a real version range pattern. The
     * resolver does nothing but insert the passed value into {@link VersionRangeResult}.
     * </p>
     *
     * @throws Exception
     */
    public void testSoft()
            throws Exception
    {
        final Version expectedVersion = versionScheme.parseVersion( "1.2.0" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "1.2.0" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedVersion, result.getLowestVersion() );
        assertEquals( expectedVersion, result.getHighestVersion() );
        assertEquals( 1, result.getVersions().size() );
    }

    /**
     * Test resolves version range {@code 1.2.4} for a <b>unknown</b> version.
     * <p>
     * The passed value is a 'soft' requirement on {@code 1.2.4} and <b>not</b> a real version range pattern. The
     * resolver does nothing but insert the passed value into {@link VersionRangeResult}.
     * </p>
     *
     * @throws Exception
     */
    public void testSoft_unknown()
            throws Exception
    {
        final Version expectedVersion = versionScheme.parseVersion( "1.2.4" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "1.2.4" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedVersion, result.getLowestVersion() );
        assertEquals( expectedVersion, result.getHighestVersion() );
        assertEquals( 1, result.getVersions().size() );
    }

    /**
     * Test resolves version range {@code [1.2.0]}.
     * <p>
     * The passed value is a 'hard' requirement on {@code 1.2.0}.
     * </p>
     *
     * @throws Exception
     */
    public void testHard()
            throws Exception
    {
        final Version expectedVersion = versionScheme.parseVersion( "1.2.0" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.2.0]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedVersion, result.getLowestVersion() );
        assertEquals( expectedVersion, result.getHighestVersion() );
        assertEquals( 1, result.getVersions().size() );
    }

    /**
     * Test resolves version range {@code [1.2.4]} for a <b>unknown</b> version.
     * <p>
     * The passed value is a 'hard' requirement on the unknown version {@code 1.2.4}. The resolver does nothing but
     * insert the passed value into {@link VersionRangeResult}.
     * </p>
     *
     * @throws Exception
     */
    public void testHard_unknown()
            throws Exception
    {

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.2.4]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertNull( result.getLowestVersion() );
        assertNull( result.getHighestVersion() );
        assertTrue( result.getVersions().isEmpty() );
    }

    /**
     * Test resolves version range {@code [1.2]}.
     * <p>
     * Based on javadoc of {@link GenericVersionScheme}:<br>
     * <blockquote> An empty segment/string is equivalent to 0. </blockquote>
     * </p>
     *
     * @see GenericVersionScheme
     * @throws Exception
     */
    public void testHard_short()
            throws Exception
    {
        final Version expectedVersion = versionScheme.parseVersion( "1.2.0" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.2]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedVersion, result.getLowestVersion() );
        assertEquals( expectedVersion, result.getHighestVersion() );
        assertEquals( 1, result.getVersions().size() );
    }

    /**
     * Test resolves version range {@code [1.2.*]}.
     * <p>
     * Based on javadoc of {@link GenericVersionScheme}:<br>
     * <blockquote> In addition to the above mentioned qualifiers, the tokens "min" and "max" may be used as final
     * version segment to denote the smallest/greatest version having a given prefix. For example, "1.2.min" denotes the
     * smallest version in the 1.2 line, "1.2.max" denotes the greatest version in the 1.2 line. A version range of the
     * form "[M.N.*]" is short for "[M.N.min, M.N.max]". </blockquote>
     * </p>
     *
     * @see GenericVersionScheme
     * @throws Exception
     */
    public void testHard_wildcard()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.2.0-SNAPSHOT" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "1.2.3" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.2.2-SNAPSHOT" );
        final Version expectedReleaseVersion = versionScheme.parseVersion( "1.2.1" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.2.*]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 8, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
        assertTrue( result.getVersions().contains( expectedReleaseVersion ) );
    }

    /**
     * Test resolves version range {@code [1.0.0,2.0.0]} (1.0.0 &lt;= x &lt;= 2.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.0} and ends with the highest inclusive
     * version {@code 2.0.0}.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftInclusiveRightInclusive()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.0" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.3.1-SNAPSHOT" );
        final Version expectedReleaseVersion = versionScheme.parseVersion( "1.3.1" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.0.0,2.0.0]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 33, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
        assertTrue( result.getVersions().contains( expectedReleaseVersion ) );
    }

    /**
     * Test resolves version range {@code [1.0.0,2.0.0)} (1.0.0 &lt;= x &lt; 2.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.0} and ends with the highest inclusive
     * version {@code 2.0.0-SNAPSHOT}.
     * </p>
     * <p>
     * Note: {@code 2.0.0-SNAPSHOT} is 'lower' than {@code 2.0.0} and will be part of this version range.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftInclusiveRightExclusive()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.0" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0-SNAPSHOT" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.3.1-SNAPSHOT" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.0.0,2.0.0)" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( 32, result.getVersions().size() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
    }

    /**
     * Test resolves version range {@code (1.0.0,2.0.0]} (1.0.0 &lt; x &lt;= 2.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.1-SNAPSHOT} and ends with the highest
     * inclusive version {@code 2.0.0}.
     * </p>
     * <p>
     * Note: The version {@code 1.0.0} will be excluded by pattern and {@code 1.0.1-SNAPSHOT} is the lowest version
     * instead.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftExclusiveRightInclusive()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.1-SNAPSHOT" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.3.1-SNAPSHOT" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "(1.0.0,2.0.0]" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 32, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
    }

    /**
     * Test resolves version range {@code (1.0.0,2.0.0)} (1.0.0 &lt; x &lt; 2.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.1-SNAPSHOT} and ends with the highest
     * inclusive version {@code 2.0.0-SNAPSHOT}.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftExclusiveRightExclusive()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.1-SNAPSHOT" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0-SNAPSHOT" );
        final Version expectedSnapshotVersion = versionScheme.parseVersion( "1.3.1-SNAPSHOT" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "(1.0.0,2.0.0)" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 31, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedSnapshotVersion ) );
    }

    /**
     * Test resolves version range {@code [1.0.0,)} (x &lt; 1.0.0).
     * <p>
     * The expected version range starts with the lowest version {@code 1.0.0} and ends with the highest inclusive
     * version {@code 3.1.0-SNAPSHOT}.
     * </p>
     *
     * @throws Exception
     */
    public void testLeftInclusiveRightOpen()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.0" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "3.1.0-SNAPSHOT" );
        final Version expectedReleaseVersion = versionScheme.parseVersion( "2.0.0" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "[1.0.0,)" ) );

        final VersionRangeResult result = sut.resolveVersionRange( session, request );
        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 69, result.getVersions().size() );
        assertTrue( result.getVersions().contains( expectedReleaseVersion ) );
    }

    /**
     * Test filter of resolved version range {@code (,2.0.0]} (x &lt;= 2.0.0).
     * <p>
     * The expected versions are only non {@code SNAPSHOT}. The version range starts with the lowest version
     * {@code 1.0.0} and ends with the highest inclusive version {@code 2.0.0}.
     * </p>
     *
     * @throws Exception
     */
    public void testVersionRangeResultFilter()
            throws Exception
    {
        final Version expectedLowestVersion = versionScheme.parseVersion( "1.0.0" );
        final Version expectedHighestVersion = versionScheme.parseVersion( "2.0.0" );

        request.setArtifact( new DefaultArtifact( "org.apache.maven.its", "mng-3092", "jar", "(,2.0.0]" ) );

        sut.setVersionRangeResultFilter( new TestOnlyVersionRangeResultFilter() );
        final VersionRangeResult result = sut.resolveVersionRange( session, request );

        assertNotNull( result );
        assertEquals( expectedLowestVersion, result.getLowestVersion() );
        assertEquals( expectedHighestVersion, result.getHighestVersion() );
        assertEquals( 17, result.getVersions().size() );
        for ( Iterator<Version> it = result.getVersions().iterator(); it.hasNext(); )
        {
            // XXX: better way to identify a SNAPSHOT version
            if ( String.valueOf( it.next() ).endsWith( "SNAPSHOT" ) )
            {
                fail( "Non filtered SNAPSHOT version in version range result." );
            }
        }
    }

    /**
     * Test error handling if invalid {@link VersionRangeResultFilter} will be set.
     *
     * @throws Exception
     */
    public void testInvalidVersionRangeResultFilter()
            throws Exception
    {
        try
        {
            sut.setVersionRangeResultFilter( null );
            fail( "Exception expected by set invalid version range result filter." );
        }
        catch ( NullPointerException iae )
        {
          assertEquals( "versionRangeResultFilter cannot be null", iae.getMessage() );
        }
    }

    private final class TestOnlyVersionRangeResultFilter implements VersionRangeResultFilter
    {

        @Override
        public VersionRangeResult filterVersionRangeResult( VersionRangeResult versionRangeResult )
                throws VersionRangeResolutionException
        {
            for ( Iterator<Version> it = versionRangeResult.getVersions().iterator(); it.hasNext(); )
            {
                // XXX: better way to identify a SNAPSHOT version
                if ( String.valueOf( it.next() ).endsWith( "SNAPSHOT" ) )
                {
                    it.remove();
                }
            }
            return versionRangeResult;
        }

    }
}
