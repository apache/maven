package org.apache.maven.usability;

import org.apache.maven.artifact.DefaultArtifact;

import junit.framework.TestCase;

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

public class InvalidArtifactDiagnoserTest
    extends TestCase
{

    private InvalidArtifactDiagnoser diagnoser = new InvalidArtifactDiagnoser();

    public void testShouldDiagnoseArtifactWithMissingGroupId() throws Throwable
    {
        testDiagnosis( "Test diagnosis for missing groupId", null, "test-artifact", "1.0", "jar" );
    }

    public void testShouldDiagnoseArtifactWithMissingArtifactId() throws Throwable
    {
        testDiagnosis( "Test diagnosis for missing artifactId", "test.group.id", null, "1.0", "jar" );
    }

    public void testShouldDiagnoseArtifactWithMissingVersion() throws Throwable
    {
        testDiagnosis( "Test diagnosis for missing version", "test.group.id", "test-artifact", null, "jar" );
    }

    public void testShouldDiagnoseArtifactWithMissingType() throws Throwable
    {
        testDiagnosis( "Test diagnosis for missing type", "test.group.id", "test-artifact", "1.0", null );
    }

    public void testShouldDiagnoseArtifactWithMissingGroupIdAndArtifactId() throws Throwable
    {
        testDiagnosis( "Test diagnosis for missing groupId and artifactId", null, null, "1.0", "jar" );
    }

    private void testDiagnosis( String testHeader, String groupId, String artifactId, String version, String type )
        throws Throwable
    {
        System.out.println( "------------------------------------------------------------" );
        System.out.println( "|  " + testHeader );
        System.out.println( "------------------------------------------------------------" );
        System.out.println();

        try
        {
            new DefaultArtifact( groupId, artifactId, version, null, type, null, null );

            fail( "artifact creation did not fail; nothing to diagnose." );
        }
        catch ( Throwable error )
        {
            assertTrue( "Unexpected error while constructing artifact: " + error, diagnoser.canDiagnose( error ) );

            if ( diagnoser.canDiagnose( error ) )
            {
                System.out.println( diagnoser.diagnose( error ) );
            }
            else
            {
                throw error;
            }
        }
    }
}
