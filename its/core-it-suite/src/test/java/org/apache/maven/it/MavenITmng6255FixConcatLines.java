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
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6255">MNG-6255</a>:
 * Check that the <code>.mvn/jvm.config</code> file contents are concatenated properly, no matter
 * what line endings are used.
 */
public class MavenITmng6255FixConcatLines
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6255FixConcatLines()
    {
        super( "[3.5.3,)" );
    }

    /**
     * Check that <code>CR</code> line endings work.
     * <p>
     * Currently disabled.
     *
     * @throws Exception in case of failure
     */
    public void disabledJvmConfigFileCR()
        throws Exception
    {
        runWithLineEndings( "\r" );
    }

    /**
     * Check that <code>LF</code> line endings work.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testJvmConfigFileLF()
        throws Exception
    {
        runWithLineEndings( "\n" );
    }

    /**
     * Check that <code>CRLF</code> line endings work.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testJvmConfigFileCRLF()
        throws Exception
    {
        runWithLineEndings( "\r\n" );
    }

    protected void runWithLineEndings( String lineEndings )
        throws Exception
    {
        File baseDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6255" );
        File mvnDir = new File( baseDir, ".mvn" );

        File jvmConfig = new File( mvnDir, "jvm.config" );
        createJvmConfigFile( jvmConfig, lineEndings, "-Djvm.config=ok", "-Xms256m", "-Xmx512m" );

        Verifier verifier = newVerifier( baseDir.getAbsolutePath() );
        verifier.addCliOption( "-Dexpression.outputFile=" + new File( baseDir, "expression.properties" ).getAbsolutePath() );
        verifier.setForkJvm( true );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "expression.properties" );
        assertEquals( "ok", props.getProperty( "project.properties.jvm-config" ) );
    }

    protected void createJvmConfigFile( File jvmConfig, String lineEndings, String...lines )
        throws Exception
    {
        String content = Joiner.on(lineEndings).join(lines);
        Files.write( content, jvmConfig, Charsets.UTF_8 );
    }
}
