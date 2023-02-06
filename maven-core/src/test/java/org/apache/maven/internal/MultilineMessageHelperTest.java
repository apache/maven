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
package org.apache.maven.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultilineMessageHelperTest {

    @Test
    public void testBuilderCommon() {
        List<String> msgs = new ArrayList<>();
        msgs.add("*****************************************************************");
        msgs.add("* Your build is requesting parallel execution, but project      *");
        msgs.add("* contains the following plugin(s) that have goals not marked   *");
        msgs.add("* as @threadSafe to support parallel building.                  *");
        msgs.add("* While this /may/ work fine, please look for plugin updates    *");
        msgs.add("* and/or request plugins be made thread-safe.                   *");
        msgs.add("* If reporting an issue, report it against the plugin in        *");
        msgs.add("* question, not against maven-core                              *");
        msgs.add("*****************************************************************");

        assertEquals(
                msgs,
                MultilineMessageHelper.format(
                        "Your build is requesting parallel execution, but project contains the following "
                                + "plugin(s) that have goals not marked as @threadSafe to support parallel building.",
                        "While this /may/ work fine, please look for plugin updates and/or "
                                + "request plugins be made thread-safe.",
                        "If reporting an issue, report it against the plugin in question, not against maven-core"));
    }

    @Test
    public void testMojoExecutor() {
        List<String> msgs = new ArrayList<>();
        msgs.add("*****************************************************************");
        msgs.add("* An aggregator Mojo is already executing in parallel build,    *");
        msgs.add("* but aggregator Mojos require exclusive access to reactor to   *");
        msgs.add("* prevent race conditions. This mojo execution will be blocked  *");
        msgs.add("* until the aggregator work is done.                            *");
        msgs.add("*****************************************************************");

        assertEquals(
                msgs,
                MultilineMessageHelper.format(
                        "An aggregator Mojo is already executing in parallel build, but aggregator "
                                + "Mojos require exclusive access to reactor to prevent race conditions. This "
                                + "mojo execution will be blocked until the aggregator work is done."));
    }
}
