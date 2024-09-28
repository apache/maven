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
import org.apache.maven.cling.invoker.InvokerRequest;
import org.apache.maven.cling.invoker.MavenOptions;
import org.apache.maven.utils.Os;

import static java.util.Objects.requireNonNull;

/**
 * Forked invoker implementation, it spawns a subprocess with Maven.
 */
public class ForkedInvoker implements Invoker {
    @Override
    public int invoke(InvokerRequest invokerRequest) throws InvokerException {
        requireNonNull(invokerRequest);

        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(invokerRequest
                .installationDirectory()
                .resolve("bin")
                .resolve(Os.IS_WINDOWS ? "mvn.cmd" : "mvn")
                .toString());

        MavenOptions mavenOptions = invokerRequest.options();
        if (mavenOptions.userProperties().isPresent()) {
            for (Map.Entry<String, String> entry :
                    mavenOptions.userProperties().get().entrySet()) {
                cmdAndArguments.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (mavenOptions.alternatePomFile().isPresent()) {
            cmdAndArguments.add("-f");
            cmdAndArguments.add(mavenOptions.alternatePomFile().get());
        }
        if (mavenOptions.offline().orElse(false)) {
            cmdAndArguments.add("-o");
        }
        if (mavenOptions.showVersionAndExit().orElse(false)) {
            cmdAndArguments.add("-v");
        }
        if (mavenOptions.showVersion().orElse(false)) {
            cmdAndArguments.add("-V");
        }
        if (mavenOptions.quiet().orElse(false)) {
            cmdAndArguments.add("-q");
        }
        if (mavenOptions.verbose().orElse(false)) {
            cmdAndArguments.add("-X");
        }
        if (mavenOptions.showErrors().orElse(false)) {
            cmdAndArguments.add("-e");
        }
        if (mavenOptions.nonRecursive().orElse(false)) {
            cmdAndArguments.add("-N");
        }
        if (mavenOptions.updateSnapshots().orElse(false)) {
            cmdAndArguments.add("-U");
        }
        if (mavenOptions.nonInteractive().orElse(false)) {
            cmdAndArguments.add("-B");
        }
        if (mavenOptions.logFile().isPresent()) {
            cmdAndArguments.add("-l");
            cmdAndArguments.add(mavenOptions.logFile().get());
        }
        // TODO: etc

        // last the goals
        cmdAndArguments.addAll(mavenOptions.goals().orElse(Collections.emptyList()));

        try {
            return new ProcessBuilder()
                    .directory(invokerRequest.cwd().toFile())
                    .command(cmdAndArguments)
                    .start()
                    .waitFor();
        } catch (IOException e) {
            invokerRequest.logger().error("IO problem while executing command: " + cmdAndArguments, e);
            return 127;
        } catch (InterruptedException e) {
            invokerRequest.logger().error("Interrupted while executing command: " + cmdAndArguments, e);
            return 127;
        }
    }

    protected void validate(InvokerRequest invokerRequest) throws InvokerException {}
}
