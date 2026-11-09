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
package org.apache.maven.api.plugin.testing;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.testing.ExpressionEvaluatorTest.ExpressionEvaluatorMojo;
import org.apache.maven.testing.plugin.InjectMojo;
import org.apache.maven.testing.plugin.MojoExtension;
import org.apache.maven.testing.plugin.MojoTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MojoTest
class MojoExtensionParameterPomTest {

    private static final String COORDINATES = "test:test-plugin:0.0.1-SNAPSHOT:goal";

    private static final String CUSTOM_POM = """
            <project>
              <groupId>customGroupId</groupId>
              <artifactId>customArtifactId</artifactId>
              <version>2.0</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>test</groupId>
                    <artifactId>test-plugin</artifactId>
                    <configuration>
                      <param>fromParameterPom</param>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """;

    @Inject
    Project project;

    @Test
    @InjectMojo(goal = COORDINATES)
    void parameterPomConfiguresBothMojoAndProject(
            @InjectMojo(goal = COORDINATES, pom = CUSTOM_POM) ExpressionEvaluatorMojo mojo) throws Exception {
        assertEquals("fromParameterPom", MojoExtension.getVariableValueFromObject(mojo, "param"));
        assertEquals("customGroupId", project.getModel().getGroupId());
    }

    @Test
    void parameterWithoutPomKeepsTheDefaultProject(@InjectMojo(goal = COORDINATES) ExpressionEvaluatorMojo mojo) {
        // not the plugin's own basedir POM, which only a method-level @InjectMojo reads
        assertEquals("myArtifactId", project.getModel().getArtifactId());
    }
}
