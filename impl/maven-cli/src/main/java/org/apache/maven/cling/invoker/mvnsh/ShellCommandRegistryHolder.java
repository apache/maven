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
package org.apache.maven.cling.invoker.mvnsh;

import java.util.ArrayList;
import java.util.List;

import org.jline.console.CommandRegistry;

import static java.util.Objects.requireNonNull;

public class ShellCommandRegistryHolder implements AutoCloseable {
    private final List<CommandRegistry> commandRegistries;

    public ShellCommandRegistryHolder() {
        this.commandRegistries = new ArrayList<>();
    }

    public void addCommandRegistry(CommandRegistry commandRegistry) {
        requireNonNull(commandRegistry, "commandRegistry");
        this.commandRegistries.add(commandRegistry);
    }

    public CommandRegistry[] getCommandRegistries() {
        return commandRegistries.toArray(new CommandRegistry[0]);
    }

    @Override
    public void close() throws Exception {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (CommandRegistry commandRegistry : commandRegistries) {
            if (commandRegistry instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }
        if (!exceptions.isEmpty()) {
            IllegalStateException ex = new IllegalStateException("Could not close commandRegistries");
            exceptions.forEach(ex::addSuppressed);
            throw ex;
        }
    }
}
