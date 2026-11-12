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
package org.apache.maven.testing.plugin;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MojoTest
class MojoExtensionInlinePomTest {

    private static final String COORDINATES = "test:test-plugin:0.0.1-SNAPSHOT:goal";

    private static final String NAMESPACED_POM = """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <groupId>customGroupId</groupId>
              <artifactId>customArtifactId</artifactId>
              <version>2.0</version>
            </project>
            """;

    private static final String POM_WITH_PROLOG = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <groupId>prologGroupId</groupId>
              <artifactId>customArtifactId</artifactId>
              <version>2.0</version>
            </project>
            """;

    @Inject
    Project project;

    @Test
    @InjectMojo(goal = COORDINATES, pom = NAMESPACED_POM)
    void inlinePomWithNamespaceIsParsedInline() {
        assertEquals("customGroupId", project.getModel().getGroupId());
    }

    @Test
    @InjectMojo(goal = COORDINATES, pom = POM_WITH_PROLOG)
    void inlinePomWithXmlPrologIsParsedInline() {
        assertEquals("prologGroupId", project.getModel().getGroupId());
    }
}
