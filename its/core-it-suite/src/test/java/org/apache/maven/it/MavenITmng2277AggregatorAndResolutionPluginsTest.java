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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2277">MNG-2277</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng2277AggregatorAndResolutionPluginsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2277AggregatorAndResolutionPluginsTest()
    {
        super( "(2.0.7,)" ); // 2.0.8+
    }

    @Test
    public void testitMNG2277()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2277" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2277" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-all:aggregator-dependencies" );
        verifier.verifyErrorFreeLog();
    }

}
