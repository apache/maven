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
package org.apache.maven.internal.compat.interactivity;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.jline.DefaultPrompter;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.components.interactivity.OutputHandler;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.eclipse.sisu.Priority;

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
    public void write(String line) throws IOException {
        defaultPrompter.doDisplay(line);
    }

    @Override
    public void writeLine(String line) throws IOException {
        defaultPrompter.doDisplay(line + System.lineSeparator());
    }

    @Override
    public String prompt(String message) throws PrompterException {
        try {
            return defaultPrompter.prompt(message, null, null);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to prompt", e);
        }
    }

    @Override
    public String prompt(String message, String defaultReply) throws PrompterException {
        try {
            return defaultPrompter.prompt(message, null, defaultReply);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to prompt", e);
        }
    }

    @Override
    public String prompt(String message, List possibleValues) throws PrompterException {
        try {
            return defaultPrompter.prompt(message, possibleValues, null);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to prompt", e);
        }
    }

    @Override
    public String prompt(String message, List possibleValues, String defaultReply) throws PrompterException {
        try {
            return defaultPrompter.prompt(message, possibleValues, defaultReply);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to prompt", e);
        }
    }

    @Override
    public String promptForPassword(String message) throws PrompterException {
        try {
            return defaultPrompter.promptForPassword(message);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to promptForPassword", e);
        }
    }

    @Override
    public void showMessage(String message) throws PrompterException {
        try {
            defaultPrompter.showMessage(message);
        } catch (org.apache.maven.api.services.PrompterException e) {
            throw new PrompterException("Unable to showMessage", e);
        }
    }
}
