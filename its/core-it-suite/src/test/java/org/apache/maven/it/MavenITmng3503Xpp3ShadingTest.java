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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3503">MNG-3503</a>. The first test verifies that
 * a plugin using plexus-utils-1.1 does not cause linkage errors. The second test verifies that a plugin with a
 * different implementation of the shaded classes is used instead.
 */
public class MavenITmng3503Xpp3ShadingTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3503Xpp3ShadingTest()
    {
        super( "(2.0.9,2.1.0-M1),(2.1.0-M1,)" ); // only test in 2.0.10+, and not in 2.1.0-M1
    }

    public void testitMNG3503NoLinkageErrors()
        throws Exception
    {
        File dir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3503/mng-3503-xpp3Shading-pu11" );

        Verifier verifier;

        verifier = newVerifier( dir.getAbsolutePath() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        assertEquals( "<root />", FileUtils.fileRead( new File( dir, "target/serialized.xml" ), "UTF-8" ) );
    }

    public void testitMNG3503Xpp3Shading()
        throws Exception
    {
        File dir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3503/mng-3503-xpp3Shading-pu-new" );
        Verifier verifier = newVerifier( dir.getAbsolutePath() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        assertEquals( "root", FileUtils.fileRead( new File( dir, "target/serialized.xml" ), "UTF-8" ) );
    }
}
