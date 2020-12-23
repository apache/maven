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

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * MNG-6030 reintroduced ReactorModelCache, but this ignores invalid relativePaths of parents
 *
 * @author Robert Scholte
 */
public class MavenITmng6330RelativePath
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6330RelativePath()
    {
        super( "(,3.5.0),(3.5.2,)" );
    }

    public void testRelativePath() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6330-relative-path" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setForkJvm( true );

        try
        {
            verifier.executeGoal( "validate");
            fail( "Should fail due to non-resolvable parent" );
        }
        catch ( VerificationException e )
        {
            assertThat( e.getMessage(), containsString("Non-resolvable parent POM") );
        }
    }
}
