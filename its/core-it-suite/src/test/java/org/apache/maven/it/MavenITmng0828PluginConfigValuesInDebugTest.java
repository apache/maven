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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-828">MNG-828</a>.
 *
 * @author Slawomir Jaranowski
 */
public class MavenITmng0828PluginConfigValuesInDebugTest
    extends AbstractMavenIntegrationTestCase
{

    private static final String NL = System.lineSeparator();

    public MavenITmng0828PluginConfigValuesInDebugTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that plain plugin configuration values are listed correctly in debug mode.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG0828()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0828" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setMavenDebug( true );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String log = FileUtils.fileRead( new File( verifier.getBasedir(), verifier.getLogFileName() ) );

        checkLog( log, "(f) aliasDefaultExpressionParam = test" );
        checkLog( log, "(f) basedir = " + testDir.getCanonicalPath() );
        checkLog( log,
                  "(f) beanParam = org.apache.maven.plugin.coreit.Bean[fieldParam=field, setterParam=setter, setterCalled=true]" );
        checkLog( log, "(f) booleanParam = true" );
        checkLog( log, "(f) byteParam = 42" );
        checkLog( log, "(f) byteParam = 42" );
        checkLog( log, "(f) characterParam = X" );

        Date date = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.S a" ).parse( "2008-11-09 11:59:03.0 AM" );
        checkLog( log, "(f) dateParam = " + date );

        checkLog( log, "(f) defaultParam = maven-core-it" );
        checkLog( log, "(f) defaultParamWithExpression = org.apache.maven.its.mng0828:test1:1.0-SNAPSHOT" );
        checkLog( log, "(f) domParam = <domParam><echo>one</echo>" + NL
            + "<echo>two</echo>" + NL
            + "<echo>three</echo>" + NL
            + "<echo>four</echo>" + NL
            + "</domParam>" );
        checkLog( log, "(f) doubleParam = -1.5" );
        checkLog( log, "(f) fieldParam = field" );
        checkLog( log, "(f) fileParam = " + new File( testDir, "pom.xml" ).getCanonicalPath() );
        checkLog( log, "(f) floatParam = 0.0" );
        checkLog( log, "(f) integerParam = 0" );
        checkLog( log, "(f) listParam = [one, two, three, four]" );
        checkLog( log, "(f) longParam = 9876543210" );
        checkLog( log, "(f) mapParam = {key1=value1, key2=value2}" );
        checkLog( log, "(f) propertiesFile = " + new File( testDir,
                                                           "target/plugin-config.properties" ).getCanonicalPath() );
        checkLog( log, "(f) propertiesParam = {key2=value2, key1=value1}" );
        checkLog( log, "(f) setParam = [item]" );
        checkLog( log, "(f) shortParam = -12345" );
        checkLog( log, "(f) stringParam = Hello World!" );
        checkLog( log, "(f) stringParams = [one, two, three, four]" );
        checkLog( log, "(f) urlParam = http://maven.apache.org/" );
        checkLog( log, "(s) setterParam = setter" );
    }

    private void checkLog( String log, String expected )
    {
        String toSearch = "[DEBUG]   " + expected + NL;
        assertTrue( NL + ">>>" + NL + log + "<<<" + NL + NL + "does not contains: " + NL + toSearch,
                    log.contains( toSearch ) );
    }

}
