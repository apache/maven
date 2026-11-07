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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.message.MessageBuilder;
import org.apache.maven.message.MessageBuilderFactory;
import org.eclipse.sisu.Priority;

/**
 * Message builder factory emitting ANSI colors. The class name is kept for compatibility, JLine is not used anymore.
 */
@Named
@Singleton
@Priority(10)
public class JLineMessageBuilderFactory implements MessageBuilderFactory {

    static final String RESET = "\u001B[0m";

    private static final List<String> COLORS =
            Arrays.asList("black", "red", "green", "yellow", "blue", "magenta", "cyan", "white");

    private static final Map<String, String> STYLES = new ConcurrentHashMap<>();

    @Override
    public boolean isColorEnabled() {
        return MessageUtils.isColorEnabled();
    }

    @Override
    public int getTerminalWidth() {
        return MessageUtils.getTerminalWidth();
    }

    @Override
    public MessageBuilder builder() {
        return builder(64);
    }

    @Override
    public MessageBuilder builder(int size) {
        return new AnsiMessageBuilder(size);
    }

    /**
     * Resolves a style to its ANSI escape sequence (empty if it has no effect). A style is either a spec like
     * {@code bold,f:red}, or a reference {@code .name:-default} looked up from the {@code maven.style.name} or
     * {@code style.name} system property, falling back to {@code default}.
     */
    static String resolve(String style) {
        return STYLES.computeIfAbsent(style, JLineMessageBuilderFactory::doResolve);
    }

    private static String doResolve(String style) {
        String spec = style;
        if (spec.startsWith(".")) {
            int i = spec.indexOf(":-");
            String name = i < 0 ? spec.substring(1) : spec.substring(1, i);
            String def = i < 0 ? "" : spec.substring(i + 2);
            spec = System.getProperty("maven.style." + name, System.getProperty("style." + name, def));
        }
        // same order as JLine emitted (ITs match exact bytes): attributes 3-9, foreground, background, bold, faint
        String[] slots = new String[5];
        TreeSet<String> attributes = new TreeSet<>();
        for (String item : spec.split(",")) {
            String code = code(item.trim().toLowerCase(Locale.ROOT));
            if (code == null) {
                continue;
            }
            int colon = item.indexOf(':');
            if (colon > 0) {
                String target = item.trim().substring(0, colon).toLowerCase(Locale.ROOT);
                slots[target.startsWith("f") ? 1 : 2] = code;
            } else if (code.equals("1")) {
                slots[3] = code;
            } else if (code.equals("2")) {
                slots[4] = code;
            } else {
                attributes.add(code);
            }
        }
        slots[0] = attributes.isEmpty() ? null : String.join(";", attributes);
        StringBuilder codes = new StringBuilder();
        for (String slot : slots) {
            if (slot != null) {
                codes.append(codes.length() > 0 ? ";" : "").append(slot);
            }
        }
        return codes.length() > 0 ? "\u001B[" + codes + "m" : "";
    }

    // ponytail: the subset of JLine's style syntax Maven styles use; unknown items are ignored
    private static String code(String item) {
        switch (item) {
            case "bold":
                return "1";
            case "faint":
                return "2";
            case "italic":
                return "3";
            case "underline":
                return "4";
            case "blink":
                return "5";
            case "inverse":
                return "7";
            case "conceal":
            case "hidden":
                return "8";
            case "crossed-out":
                return "9";
            default:
        }
        int colon = item.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String target = item.substring(0, colon);
        boolean fg = target.equals("f") || target.equals("fg") || target.equals("foreground");
        boolean bg = target.equals("b") || target.equals("bg") || target.equals("background");
        return fg || bg ? color(item.substring(colon + 1), fg) : null;
    }

    private static String color(String color, boolean fg) {
        if (color.equals("default")) {
            return fg ? "39" : "49";
        }
        boolean bright = false;
        if (color.startsWith("bright-")) {
            bright = true;
            color = color.substring("bright-".length());
        } else if (color.startsWith("!")) {
            bright = true;
            color = color.substring(1);
        }
        int index = COLORS.indexOf(color);
        if (index >= 0) {
            return String.valueOf((fg ? 30 : 40) + (bright ? 60 : 0) + index);
        }
        try {
            if (color.startsWith("#") || color.startsWith("x")) {
                int rgb = Integer.parseInt(color.substring(1), 16);
                return (fg ? "38" : "48") + ";2;" + (rgb >> 16 & 0xff) + ";" + (rgb >> 8 & 0xff) + ";" + (rgb & 0xff);
            }
            return (fg ? "38" : "48") + ";5;" + (Integer.parseInt(color) & 0xff);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class AnsiMessageBuilder implements MessageBuilder {
        private final StringBuilder builder;
        private boolean styled;

        private AnsiMessageBuilder(int size) {
            this.builder = new StringBuilder(size);
        }

        @Override
        public MessageBuilder style(String style) {
            if (MessageUtils.isColorEnabled()) {
                String ansi = resolve(style);
                if (!ansi.isEmpty()) {
                    if (styled) {
                        builder.append(RESET);
                    }
                    builder.append(ansi);
                    styled = true;
                }
            }
            return this;
        }

        @Override
        public MessageBuilder resetStyle() {
            if (styled) {
                builder.append(RESET);
                styled = false;
            }
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
            if (length == 0) {
                styled = false;
            }
            return this;
        }

        @Override
        public String build() {
            return styled ? builder + RESET : builder.toString();
        }

        @Override
        public String toString() {
            return build();
        }
    }
}
