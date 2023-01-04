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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6256">MNG-6256</a>: check that directories
 * passed via <code>-f/--file</code> containing special characters do not break the script. E.g
 * <code>-f "directoryWithClosing)Bracket/pom.xml"</code>.
 */
public class MavenITmng6256SpecialCharsAlternatePOMLocation
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6256SpecialCharsAlternatePOMLocation()
    {
        super( "(3.6.0,)" );
    }

    /**
     * check script is working when path to POM is set to <code>directory-with- -space</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testDirectoryWithSpace()
        throws Exception
    {
        runWithMvnFileLongOption( "directory-with- -space" );
        runWithMvnFileShortOption( "directory-with- -space" );
    }

    /**
     * check script is working when path to POM is set to <code>directory-with-)-closing-bracket</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testDirectoryWithClosingBracket()
        throws Exception
    {
        runWithMvnFileLongOption( "directory-with-)-closing-bracket" );
        runWithMvnFileShortOption( "directory-with-)-closing-bracket" );
    }

    private void runWithMvnFileLongOption( String subDir )
        throws Exception
    {
        runCoreExtensionWithOption( "--file", subDir );
    }

    private void runWithMvnFileShortOption( String subDir )
        throws Exception
    {
        runCoreExtensionWithOption( "-f", subDir );
    }

    private void runCoreExtensionWithOption( String option, String subDir )
        throws Exception
    {
        File resourceDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-6256-special-chars-alternate-pom-location" );

        File testDir = new File( resourceDir, "../mng-6256-" + subDir );
        testDir.mkdir();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliArgument( option ); // -f/--file
        verifier.addCliArgument( "\"" + new File( resourceDir, subDir ).getAbsolutePath() + "\"" ); // "<path>"
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
