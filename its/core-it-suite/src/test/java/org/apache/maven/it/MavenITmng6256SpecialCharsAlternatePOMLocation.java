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

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6256">MNG-6256</a>: check that directories
 * passed via <code>-f/--file</code> containing special characters do not break the script. E.g
 * <code>-f "folderWithClosing)Bracket/pom.xml"</code>.
 */
public class MavenITmng6256SpecialCharsAlternatePOMLocation
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6256SpecialCharsAlternatePOMLocation()
    {
        super( "(3.6.0,)" );
    }

    protected MavenITmng6256SpecialCharsAlternatePOMLocation( String constraint )
    {
        super( constraint );
    }

    /**
     * check script is working when path to POM is set to <code>folder-with- -space</code>
     */
    public void testFolderWithSpace()
        throws Exception
    {
        runWithMvnFileLongOption( "folder-with- -space" );
        runWithMvnFileShortOption( "folder-with- -space" );
    }

    /**
     * check script is working when path to POM is set to <code>folder-with-)-closing-bracket</code>
     */
    public void testFolderWithClosingBracket()
        throws Exception
    {
        runWithMvnFileLongOption( "folder-with-)-closing-bracket" );
        runWithMvnFileShortOption( "folder-with-)-closing-bracket" );
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
        verifier.getCliOptions().add( option ); // -f/--file
        verifier.getCliOptions().add( "\"" + new File( resourceDir, subDir ).getAbsolutePath() + "\"" ); // "<path>"
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
