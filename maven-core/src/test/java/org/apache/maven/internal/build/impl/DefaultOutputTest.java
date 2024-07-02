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
package org.apache.maven.internal.build.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.api.build.Output;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultOutputTest {

    @TempDir
    public Path temp;

    private TestBuildContext newBuildContext() {
        Path stateFile = temp.resolve("buildstate.ctx");
        return new TestBuildContext(stateFile, Collections.emptyMap());
    }

    @Test
    public void testOutputStream_createParentDirectories() throws Exception {
        Path outputFile = temp.resolve("sub/dir/outputFile");

        TestBuildContext context = newBuildContext();
        Output output = context.processOutput(outputFile);
        output.newOutputStream().close();

        assertTrue(Files.isReadable(outputFile));
    }
}
