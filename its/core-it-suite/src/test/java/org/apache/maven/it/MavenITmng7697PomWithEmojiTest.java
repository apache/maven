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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7697">MNG-7697</a>.
 * Verifies if pom with emoji in comments are parsed.
 */
class MavenITmng7697PomWithEmojiTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7697PomWithEmojiTest()
    {
        // affected Maven versions: 3.9.0, 4.0.0-alpha-4
        super( "(,3.9.0),(3.9.0,4.0.0-alpha-4),(4.0.0-alpha-4,)" );
    }

    /**
     * Pom read successful.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testPomRead()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7697-emoji" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "verify" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
