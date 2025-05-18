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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.services.PrompterException;

@Experimental
@Named
@Singleton
public class DefaultPrompter implements Prompter {

    @Override
    public String prompt(String message, List<String> possibleValues, String defaultReply) throws PrompterException {
        return doPrompt(message, possibleValues, defaultReply, false);
    }

    @Override
    public String promptForPassword(String message) throws PrompterException {
        return doPrompt(message, null, null, true);
    }

    @Override
    public void showMessage(String message) throws PrompterException {
        doDisplay(message);
    }

    private String doPrompt(String message, boolean password) {
        try {
            if (message != null) {
                if (!message.endsWith("\n")) {
                    if (message.endsWith(":")) {
                        message += " ";
                    } else if (!message.endsWith(": ")) {
                        message += ": ";
                    }
                }
                int lastNl = message.lastIndexOf('\n');
                String begin = message.substring(0, lastNl + 1);
                message = message.substring(lastNl + 1);
                MessageUtils.terminal.writer().print(begin);
                MessageUtils.terminal.flush();
            }
            return MessageUtils.reader.readLine(message, password ? '*' : null);
        } catch (Exception e) {
            throw new PrompterException("Unable to prompt user", e);
        }
    }

    private void doDisplay(String message) {
        try {
            MessageUtils.terminal.writer().print(message);
            MessageUtils.terminal.flush();
        } catch (Exception e) {
            throw new PrompterException("Unable to display message", e);
        }
    }

    private String doPrompt(String message, List<?> possibleValues, String defaultReply, boolean password) {
        String formattedMessage = formatMessage(message, possibleValues, defaultReply);
        String line;
        do {
            line = doPrompt(formattedMessage, password);
            if (line == null && defaultReply == null) {
                throw new PrompterException("EOF");
            }
            if (line == null || line.isEmpty()) {
                line = defaultReply;
            }
            if (line != null && possibleValues != null && !possibleValues.contains(line)) {
                doDisplay("Invalid selection.\n");
            }
        } while (line == null || possibleValues != null && !possibleValues.contains(line));
        return line;
    }

    private String formatMessage(String message, List<?> possibleValues, String defaultReply) {
        if (message == null) {
            return null;
        }
        StringBuilder formatted = new StringBuilder(message.length() * 2);
        formatted.append(message);
        if (possibleValues != null && !possibleValues.isEmpty()) {
            formatted.append(" (");
            for (Iterator<?> it = possibleValues.iterator(); it.hasNext(); ) {
                String possibleValue = String.valueOf(it.next());
                formatted.append(possibleValue);
                if (it.hasNext()) {
                    formatted.append('/');
                }
            }
            formatted.append(')');
        }
        if (defaultReply != null) {
            formatted.append(' ').append(defaultReply).append(": ");
        }
        return formatted.toString();
    }
}
