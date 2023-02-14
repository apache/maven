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
package org.apache.maven.plugin.testing;

import java.io.StringReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author Edwin Punzalan
 */
public class ExpressionEvaluatorTest extends AbstractMojoTestCase {
    private Xpp3Dom pomDom;

    private PlexusConfiguration pluginConfiguration;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        StringBuffer pom = new StringBuffer();

        pom.append("<project>").append("\n");
        pom.append("  <build>").append("\n");
        pom.append("    <plugins>").append("\n");
        pom.append("      <plugin>").append("\n");
        pom.append("        <artifactId>maven-test-mojo</artifactId>").append("\n");
        pom.append("        <configuration>").append("\n");
        pom.append("          <basedir>${basedir}</basedir>").append("\n");
        pom.append("          <workdir>${basedir}/workDirectory</workdir>").append("\n");
        pom.append("          <localRepository>${localRepository}</localRepository>")
                .append("\n");
        pom.append("        </configuration>").append("\n");
        pom.append("      </plugin>").append("\n");
        pom.append("    </plugins>").append("\n");
        pom.append("  </build>").append("\n");
        pom.append("</project>").append("\n");

        pomDom = Xpp3DomBuilder.build(new StringReader(pom.toString()));

        pluginConfiguration = extractPluginConfiguration("maven-test-mojo", pomDom);
    }

    /**
     * @throws Exception if any
     */
    public void testInjection() throws Exception {
        ExpressionEvaluatorMojo mojo = new ExpressionEvaluatorMojo();

        mojo = (ExpressionEvaluatorMojo) configureMojo(mojo, pluginConfiguration);

        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail(e.getMessage());
        }
    }
}
