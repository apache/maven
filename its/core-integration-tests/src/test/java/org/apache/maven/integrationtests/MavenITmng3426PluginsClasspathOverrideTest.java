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

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 *
 */
public class MavenITmng3426PluginsClasspathOverrideTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3426PluginsClasspathOverrideTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // 2.0.8+
    }

    public void testitMNG3426 ()
        throws Exception
    {

        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng3426-overridingPluginDependency" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );

        verifier.setCliOptions( cliOptions );

        verifier.executeGoal( "org.codehaus.mojo:castor-maven-plugin:generate" );
        verifier.verifyErrorFreeLog();

        // The generated file header contains the castor version used for code generation
        // "This class was automatically generated with <a href="http://www.castor.org">Castor 1.1.1</a> ..."
        File generated = new File( testDir, "target/generated-sources/castor/Test.java" );
        String file = FileUtils.fileRead( generated );
        Assert.assertTrue( file.indexOf( "Castor 1.1.1" ) > 0 );
    }
}
