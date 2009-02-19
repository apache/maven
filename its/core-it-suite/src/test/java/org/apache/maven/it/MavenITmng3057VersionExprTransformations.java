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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.IOUtil;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.util.StringUtils;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-3057">MNG-3057</a>.
 *
 * @todo Fill in a better description of what this test verifies!
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 * 
 */
public class MavenITmng3057VersionExprTransformations
    extends AbstractMavenIntegrationTestCase
{
    
    private static final List VERIFICATION_EXPRESSIONS;
    
    static
    {
        List exprs = new ArrayList();
        
        exprs.add( "project.parent.version" );
        exprs.add( "project.version" );
        
        VERIFICATION_EXPRESSIONS = exprs;
    }
    
    public MavenITmng3057VersionExprTransformations()
        throws InvalidVersionSpecificationException
    {
        super( "(2.1.0-M1,2.99.99)" ); // only test in 2.0.9+
    }

    public void testitMNG3057 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3057" );
        
        File localRepo = new File( testDir, "target/local" );
        String remoteRepo = new File( testDir, "target/deployment" ).toURL().toExternalForm();

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng3057", "mng-3057", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng3057", "level2", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng3057", "level3", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng3057", "level3", "1", "jar" );

        Properties properties = verifier.newDefaultFilterProperties();
        properties.setProperty( "@deployTo@", remoteRepo );

        verifier.filterFile( "pom.xml", "pom.xml", "UTF-8", properties );

        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestVersion=1" );
        cliOptions.add( "-Dmaven.repo.local=" + localRepo.getAbsolutePath() );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "deploy" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        assertVersionExpressions( new File( localRepo, "org/apache/maven/its/mng3057/mng-3057/1/mng-3057-1.pom" ) ); 
        assertVersionExpressions( new File( localRepo, "org/apache/maven/its/mng3057/level2/1/level2-1.pom" ) ); 
        assertVersionExpressions( new File( localRepo, "org/apache/maven/its/mng3057/level3/1/level3-1.pom" ) ); 
        
        assertVersionExpressions( new File( remoteRepo, "org/apache/maven/its/mng3057/mng-3057/1/mng-3057-1.pom" ) ); 
        assertVersionExpressions( new File( remoteRepo, "org/apache/maven/its/mng3057/level2/1/level2-1.pom" ) ); 
        assertVersionExpressions( new File( remoteRepo, "org/apache/maven/its/mng3057/level3/1/level3-1.pom" ) ); 
    }

    private void assertVersionExpressions( File pomFile )
        throws VerificationException, IOException
    {
        Verifier verifier = new Verifier( pomFile.getParentFile().getAbsolutePath() );
        
        List cliOptions = new ArrayList();
        cliOptions.add( "-f" );
        cliOptions.add( "-Dexpression.outputFile=expressions.properties" );
        cliOptions.add( "-Dexpression.expressions=" + StringUtils.join( VERIFICATION_EXPRESSIONS.iterator(), "," ) );
        cliOptions.add( pomFile.getName() );
        
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:eval" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        File propsFile = new File( pomFile.getParentFile(), "expressions.properties" );
        InputStream is = null;
        Properties props = new Properties();
        try
        {
            is = new FileInputStream( propsFile );
            props.load( is );
        }
        finally
        {
            IOUtil.close( is );
        }
        
        for ( Iterator it = VERIFICATION_EXPRESSIONS.iterator(); it.hasNext(); )
        {
            String expr = (String ) it.next();
            String value = props.getProperty( expr );
            if ( value != null )
            {
                assertEquals( "POM expression not interpolated: '" + expr + "'\nin: '" + pomFile + "'.", "1", value );
            }
        }
    }
    
}
