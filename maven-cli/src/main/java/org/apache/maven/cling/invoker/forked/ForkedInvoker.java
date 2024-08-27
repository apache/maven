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
package org.apache.maven.cling.invoker.forked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.cling.invoker.Invoker;
import org.apache.maven.cling.invoker.InvokerException;
import org.apache.maven.cling.invoker.Options;
import org.apache.maven.cling.invoker.Request;
import org.apache.maven.utils.Os;

import static java.util.Objects.requireNonNull;

/**
 * Forked invoker implementation, it spawns a subprocess with Maven.
 */
public class ForkedInvoker implements Invoker {
    @Override
    public int invoke(Request request) throws InvokerException {
        requireNonNull(request);

        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(request.installationDirectory()
                .resolve("bin")
                .resolve(Os.IS_WINDOWS ? "mvn.cmd" : "mvn")
                .toString());

        Options options = request.options();
        if (options.userProperties().isPresent()) {
            for (Map.Entry<String, String> entry :
                    options.userProperties().get().entrySet()) {
                cmdAndArguments.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (options.alternatePomFile().isPresent()) {
            cmdAndArguments.add("-f");
            cmdAndArguments.add(options.alternatePomFile().get());
        }
        if (options.offline().orElse(false)) {
            cmdAndArguments.add("-o");
        }
        if (options.showVersionAndExit().orElse(false)) {
            cmdAndArguments.add("-v");
        }
        if (options.showVersion().orElse(false)) {
            cmdAndArguments.add("-V");
        }
        if (options.quiet().orElse(false)) {
            cmdAndArguments.add("-q");
        }
        if (options.verbose().orElse(false)) {
            cmdAndArguments.add("-X");
        }
        if (options.showErrors().orElse(false)) {
            cmdAndArguments.add("-e");
        }
        if (options.nonRecursive().orElse(false)) {
            cmdAndArguments.add("-N");
        }
        if (options.updateSnapshots().orElse(false)) {
            cmdAndArguments.add("-U");
        }
        if (options.nonInteractive().orElse(false)) {
            cmdAndArguments.add("-B");
        }
        if (options.logFile().isPresent()) {
            cmdAndArguments.add("-l");
            cmdAndArguments.add(options.logFile().get());
        }
        // TODO: etc

        // last the goals
        cmdAndArguments.addAll(options.goals().orElse(Collections.emptyList()));

        try {
            return new ProcessBuilder()
                    .directory(request.cwd().toFile())
                    .command(cmdAndArguments)
                    .start()
                    .waitFor();
        } catch (IOException e) {
            request.logger().error("IO problem while executing command: " + cmdAndArguments, e);
            return 127;
        } catch (InterruptedException e) {
            request.logger().error("Interrupted while executing command: " + cmdAndArguments, e);
            return 127;
        }
    }

    protected void validate(Request request) throws InvokerException {}
}
