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
package org.apache.maven.jline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.components.interactivity.OutputHandler;
import org.codehaus.plexus.components.interactivity.Prompter;

/**
 * This class is injected into any legacy component that would want to use legacy "Plexus Interactivity API".
 * It simply delegates to Maven4 API {@link DefaultPrompter}.
 */
@Experimental
@Named
@Singleton
@Priority(10)
public class LegacyPlexusInteractivity implements Prompter, InputHandler, OutputHandler {
    private final DefaultPrompter defaultPrompter;

    @Inject
    public LegacyPlexusInteractivity(DefaultPrompter defaultPrompter) {
        this.defaultPrompter = defaultPrompter;
    }

    @Override
    public String readLine() throws IOException {
        return defaultPrompter.doPrompt(null, false);
    }

    @Override
    public String readPassword() throws IOException {
        return defaultPrompter.doPrompt(null, true);
    }

    @Override
    public List<String> readMultipleLines() throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line = readLine(); line != null && !line.isEmpty(); line = readLine()) {
            lines.add(line);
        }
        return lines;
    }

    @Override
    public void write(String line) {
        defaultPrompter.showMessage(line);
    }

    @Override
    public void writeLine(String line) {
        defaultPrompter.showMessage(line + System.lineSeparator());
    }

    @Override
    public String prompt(String message) {
        return defaultPrompter.prompt(message, null, null);
    }

    @Override
    public String prompt(String message, String defaultReply) {
        return defaultPrompter.prompt(message, null, defaultReply);
    }

    @Override
    public String prompt(String message, List possibleValues) {
        return defaultPrompter.prompt(message, possibleValues, null);
    }

    @Override
    public String prompt(String message, List possibleValues, String defaultReply) {
        return defaultPrompter.prompt(message, possibleValues, defaultReply);
    }

    @Override
    public String promptForPassword(String message) {
        return defaultPrompter.promptForPassword(message);
    }

    @Override
    public void showMessage(String message) {
        defaultPrompter.showMessage(message);
    }
}
