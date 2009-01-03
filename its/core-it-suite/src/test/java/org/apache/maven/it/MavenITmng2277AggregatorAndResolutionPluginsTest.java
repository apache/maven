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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
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

    public void testitMNG2277 ()
        throws Exception
    {
   
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2277" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "parent", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "test", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2277", "assembly", "1.0", "jar" );

        /*
         * The Command Line Options (CLI) are passed to the
         * verifier as a list. This is handy for things like
         * redefining the local repository if needed. In
         * this case, we use the -N flag so that Maven won't
         * recurse. We are only installing the parent pom to
         * the local repo here.
         */
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-all:aggregator-dependencies" );

        verifier.verifyErrorFreeLog();
    }
}
