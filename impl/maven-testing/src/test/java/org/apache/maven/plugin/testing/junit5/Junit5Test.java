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
package org.apache.maven.plugin.testing.junit5;

import javax.inject.Inject;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.ParametersMojo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MojoTest
class Junit5Test {

    private static final String POM = "<project>"
            + "<build>"
            + " <plugins>"
            + "  <plugin>"
            + "   <artifactId>test-plugin</artifactId>"
            + "   <configuration>"
            + "   </configuration>"
            + "  </plugin>"
            + " </plugins>"
            + "</build>" + "</project>";

    @Inject
    private Log log;

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = POM)
    void simpleMojo(ParametersMojo mojo) {
        assertEquals(log, mojo.getLog());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = POM)
    @MojoParameter(name = "plain", value = "plainValue")
    @MojoParameter(name = "withDefault", value = "withDefaultValue")
    void simpleMojoWithParameters(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.plain);
        assertEquals("withDefaultValue", mojo.withDefault);
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:parameters", pom = POM)
    @MojoParameter(name = "plain", value = "plainValue")
    void simpleMojoWithParameter(ParametersMojo mojo) {
        assertEquals("plainValue", mojo.plain);
        assertDoesNotThrow(mojo::execute);
    }
}
