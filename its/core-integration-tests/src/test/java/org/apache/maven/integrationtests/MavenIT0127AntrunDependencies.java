package org.apache.maven.integrationtests;

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

import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0127AntrunDependencies
    extends AbstractMavenIntegrationTestCase
{
  
    /**
    * MNG-1323
    */
    public void testit0127()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0127-antrunDependencies" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0127", "parent", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0127", "a", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0127", "b",  "1.0-SNAPSHOT", "jar" );
        verifier.executeGoal( "compile" ); 
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
    
    

}
