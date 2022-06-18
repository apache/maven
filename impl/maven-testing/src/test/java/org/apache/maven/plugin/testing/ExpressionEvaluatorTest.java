package org.apache.maven.plugin.testing;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.StringReader;

/**
 * @author Edwin Punzalan
 */
public class ExpressionEvaluatorTest
    extends AbstractMojoTestCase
{

    private PlexusConfiguration pluginConfiguration;

    /** {@inheritDoc} */
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        String pom = "<project>" + "\n"
                + "  <build>" + "\n"
                + "    <plugins>" + "\n"
                + "      <plugin>" + "\n"
                + "        <artifactId>maven-test-mojo</artifactId>" + "\n"
                + "        <configuration>" + "\n"
                + "          <basedir>${basedir}</basedir>" + "\n"
                + "          <workdir>${basedir}/workDirectory</workdir>" + "\n"
                + "          <localRepository>${localRepository}</localRepository>" + "\n"
                + "        </configuration>" + "\n"
                + "      </plugin>" + "\n"
                + "    </plugins>" + "\n"
                + "  </build>" + "\n"
                + "</project>" + "\n";

        Xpp3Dom pomDom = Xpp3DomBuilder.build( new StringReader( pom ) );

        pluginConfiguration = extractPluginConfiguration( "maven-test-mojo", pomDom );
    }

    /**
     * @throws Exception if any
     */
    public void testInjection()
        throws Exception
    {
        ExpressionEvaluatorMojo mojo = new ExpressionEvaluatorMojo();

        mojo = (ExpressionEvaluatorMojo) configureMojo( mojo, pluginConfiguration );

        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getMessage() );
        }
    }
}
