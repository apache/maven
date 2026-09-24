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
package org.apache.maven.logging.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.logging.OutputCapabilities.Destination;
import org.jline.terminal.impl.exec.ExecTerminalProvider;
import org.jline.terminal.spi.SystemStream;
import org.jline.utils.OSUtils;

/** Subprocess entry point for testing the real exec provider with an empty PATH. */
public final class ExecTerminalProbe {
    private ExecTerminalProbe() {}

    public static void main(String[] args) throws Exception {
        ExecTerminalProvider provider = new ExecTerminalProvider();
        for (SystemStream stream : Arrays.asList(SystemStream.Output, SystemStream.Error)) {
            try {
                Process unexpected =
                        new ProcessBuilder(OSUtils.TEST_COMMAND, "-t", Integer.toString(stream.ordinal())).start();
                unexpected.destroyForcibly();
                throw new AssertionError("Expected the external terminal probe command to be unavailable");
            } catch (IOException expected) {
                // The provider swallows this failure and returns false instead of throwing.
            }
            if (provider.isSystemStream(stream)) {
                throw new AssertionError("Expected an inconclusive negative exec probe for " + stream);
            }
            Destination destination = TerminalOutputCapabilities.probe(Collections.singletonList(provider), stream);
            if (destination != Destination.UNKNOWN) {
                throw new AssertionError(stream + ": expected UNKNOWN, got " + destination);
            }
        }
    }
}
