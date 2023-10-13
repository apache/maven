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
package org.apache.maven.cli.jline;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.components.interactivity.OutputHandler;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

import static org.jline.utils.AttributedStyle.DEFAULT;

@Experimental
@Named
@Singleton
@Priority(10)
public class JLineMessageBuilderFactory implements MessageBuilderFactory, Prompter, InputHandler, OutputHandler {

    private final StyleResolver resolver;

    public JLineMessageBuilderFactory() {
        this.resolver = new MavenStyleResolver();
    }

    @Override
    public boolean isColorEnabled() {
        return false;
    }

    @Override
    public int getTerminalWidth() {
        return MessageUtils.getTerminalWidth();
    }

    @Override
    public MessageBuilder builder() {
        return new JlineMessageBuilder();
    }

    @Override
    public MessageBuilder builder(int size) {
        return new JlineMessageBuilder(size);
    }

    @Override
    public String readLine() throws IOException {
        return doPrompt(null, true);
    }

    @Override
    public String readPassword() throws IOException {
        return doPrompt(null, true);
    }

    @Override
    public List<String> readMultipleLines() throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line = this.readLine(); line != null && !line.isEmpty(); line = readLine()) {
            lines.add(line);
        }
        return lines;
    }

    @Override
    public void write(String line) throws IOException {
        doDisplay(line);
    }

    @Override
    public void writeLine(String line) throws IOException {
        doDisplay(line + System.lineSeparator());
    }

    @Override
    public String prompt(String message) throws PrompterException {
        return prompt(message, null, null);
    }

    @Override
    public String prompt(String message, String defaultReply) throws PrompterException {
        return prompt(message, null, defaultReply);
    }

    @Override
    public String prompt(String message, List possibleValues) throws PrompterException {
        return prompt(message, possibleValues, null);
    }

    @Override
    public String prompt(String message, List possibleValues, String defaultReply) throws PrompterException {
        return doPrompt(message, possibleValues, defaultReply, false);
    }

    @Override
    public String promptForPassword(String message) throws PrompterException {
        return doPrompt(message, null, null, true);
    }

    @Override
    public void showMessage(String message) throws PrompterException {
        try {
            doDisplay(message);
        } catch (IOException e) {
            throw new PrompterException("Failed to present prompt", e);
        }
    }

    String doPrompt(String message, List<Object> possibleValues, String defaultReply, boolean password)
            throws PrompterException {
        String formattedMessage = formatMessage(message, possibleValues, defaultReply);
        String line;
        do {
            try {
                line = doPrompt(formattedMessage, password);
                if (line == null && defaultReply == null) {
                    throw new IOException("EOF");
                }
            } catch (IOException e) {
                throw new PrompterException("Failed to prompt user", e);
            }
            if (line == null || line.isEmpty()) {
                line = defaultReply;
            }
            if (line != null && (possibleValues != null && !possibleValues.contains(line))) {
                try {
                    doDisplay("Invalid selection.\n");
                } catch (IOException e) {
                    throw new PrompterException("Failed to present feedback", e);
                }
            }
        } while (line == null || (possibleValues != null && !possibleValues.contains(line)));
        return line;
    }

    private String formatMessage(String message, List<Object> possibleValues, String defaultReply) {
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

    private void doDisplay(String message) throws IOException {
        try {
            MessageUtils.terminal.writer().print(message);
            MessageUtils.terminal.flush();
        } catch (Exception e) {
            throw new IOException("Unable to display message", e);
        }
    }

    private String doPrompt(String message, boolean password) throws IOException {
        try {
            return MessageUtils.reader.readLine(message != null ? message + ": " : null, password ? '*' : null);
        } catch (Exception e) {
            throw new IOException("Unable to prompt user", e);
        }
    }

    class JlineMessageBuilder implements MessageBuilder {

        final AttributedStringBuilder builder;

        JlineMessageBuilder() {
            builder = new AttributedStringBuilder();
        }

        JlineMessageBuilder(int size) {
            builder = new AttributedStringBuilder(size);
        }

        @Override
        public MessageBuilder style(String style) {
            if (MessageUtils.isColorEnabled()) {
                builder.style(resolver.resolve(style));
            }
            return this;
        }

        @Override
        public MessageBuilder resetStyle() {
            builder.style(DEFAULT);
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs) {
            builder.append(cs);
            return this;
        }

        @Override
        public MessageBuilder append(CharSequence cs, int start, int end) {
            builder.append(cs, start, end);
            return this;
        }

        @Override
        public MessageBuilder append(char c) {
            builder.append(c);
            return this;
        }

        @Override
        public MessageBuilder setLength(int length) {
            builder.setLength(length);
            return this;
        }

        @Override
        public String build() {
            return builder.toAnsi(MessageUtils.terminal);
        }

        @Override
        public String toString() {
            return build();
        }
    }

    static class MavenStyleResolver extends StyleResolver {

        private final Map<String, AttributedStyle> styles = new ConcurrentHashMap<>();

        MavenStyleResolver() {
            super(s -> System.getProperty("style." + s));
        }

        @Override
        public AttributedStyle resolve(String spec) {
            return styles.computeIfAbsent(spec, this::doResolve);
        }

        @Override
        public AttributedStyle resolve(String spec, String defaultSpec) {
            return resolve(defaultSpec != null ? spec + ":-" + defaultSpec : spec);
        }

        private AttributedStyle doResolve(String spec) {
            String def = null;
            int i = spec.indexOf(":-");
            if (i != -1) {
                String[] parts = spec.split(":-");
                spec = parts[0].trim();
                def = parts[1].trim();
            }
            return super.resolve(spec, def);
        }
    }
}
