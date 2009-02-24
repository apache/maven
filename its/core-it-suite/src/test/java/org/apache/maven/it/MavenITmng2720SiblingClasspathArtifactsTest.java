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

package org.apache.maven.it;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-2720">MNG-2720</a>.
 * 
 * This test will ensure that running the 'package' phase on a multimodule build with child
 * interdependency will result in one child using the JAR of the other child in its compile
 * classpath, NOT the target/classes directory. This is critical, since sibling projects might
 * use literally ANY artifact produced by another module project, and limiting to target/classes
 * and target/test-classes eliminates many of the options that would be possible if the dependent
 * sibling were built on its own.
 *
 * @author jdcasey
 * 
 */
public class MavenITmng2720SiblingClasspathArtifactsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2720SiblingClasspathArtifactsTest()
        throws InvalidVersionSpecificationException
    {
        super( "[2.1.0,)" );
    }

    public void testIT ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2720" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        List compileClassPath = verifier.loadLines( "child2/target/compile.classpath", "UTF-8" );
        assertTrue( find( "child1-1.jar", compileClassPath ) );
        
        compileClassPath = verifier.loadLines( "child3/target/compile.classpath", "UTF-8" );
        assertFalse( find( "child1-1.jar", compileClassPath ) );
        assertTrue( find( "child1-1-tests.jar", compileClassPath ) );
        
    }

    private boolean find( String pathSubstr, List classPath )
    {
        for ( Iterator it = classPath.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();
            
            if ( path.indexOf( pathSubstr ) > -1 )
            {
                return true;
            }
        }
        
        return false;
    }
}
