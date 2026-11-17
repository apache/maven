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

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.di.Inject;
import org.apache.maven.di.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
class MojoExtensionMissingGoalTest {

    @Inject
    Injector injector;

    @Test
    @InjectMojo(goal = "test:test-plugin:0.0.1-SNAPSHOT:missing")
    void missingGoalIsReportedByName() {
        Exception e = assertThrows(Exception.class, () -> injector.getInstance(MojoExecution.class));
        String messages = "";
        for (Throwable t = e; t != null; t = t.getCause()) {
            messages += t.getClass().getName() + ": " + t.getMessage() + System.lineSeparator();
        }
        assertTrue(messages.contains("missing"), "exception chain should name the missing goal, got: " + messages);
    }
}
