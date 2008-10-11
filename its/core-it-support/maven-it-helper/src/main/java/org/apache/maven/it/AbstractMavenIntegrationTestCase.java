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

    private boolean skip;

    private ArtifactVersion mavenVersion;

    private VersionRange versionRange;

    protected AbstractMavenIntegrationTestCase()
    {
    }

    protected AbstractMavenIntegrationTestCase( String versionRangeStr )
    {
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
            skip = !versionRange.containsVersion( version );
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
            return versionRange.containsVersion( version );
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
        out.print( getITName() + "(" + getName() + ").." );

        if ( skip )
        {
            out.println( " Skipping - version " + getMavenVersion() + " not in range " + versionRange );
            return;
        }

        if ( "true".equals( System.getProperty( "useEmptyLocalRepository", "false" ) ) )
        {
            setupLocalRepo();
        }

        try
        {
            super.runTest();
            out.println( " Ok" );
        }
        catch ( Throwable t )
        {
            out.println( " Failure" );
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
}
