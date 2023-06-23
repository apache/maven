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
package org.apache.maven.cli.jansi;

import java.util.Locale;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Configurable message styles.
 * @since 4.0.0
 */
enum Style {
    DEBUG("bold,cyan"),
    INFO("bold,blue"),
    WARNING("bold,yellow"),
    ERROR("bold,red"),
    SUCCESS("bold,green"),
    FAILURE("bold,red"),
    STRONG("bold"),
    MOJO("green"),
    PROJECT("cyan");

    private final boolean bold;

    private final boolean bright;

    private final Color color;

    private final boolean bgBright;

    private final Color bgColor;

    Style(String defaultValue) {
        boolean currentBold = false;
        boolean currentBright = false;
        Color currentColor = null;
        boolean currentBgBright = false;
        Color currentBgColor = null;

        String value = System.getProperty("style." + name().toLowerCase(Locale.ENGLISH), defaultValue)
                .toLowerCase(Locale.ENGLISH);

        for (String token : value.split(",")) {
            if ("bold".equals(token)) {
                currentBold = true;
            } else if (token.startsWith("bg")) {
                token = token.substring(2);
                if (token.startsWith("bright")) {
                    currentBgBright = true;
                    token = token.substring(6);
                }
                currentBgColor = toColor(token);
            } else {
                if (token.startsWith("bright")) {
                    currentBright = true;
                    token = token.substring(6);
                }
                currentColor = toColor(token);
            }
        }

        this.bold = currentBold;
        this.bright = currentBright;
        this.color = currentColor;
        this.bgBright = currentBgBright;
        this.bgColor = currentBgColor;
    }

    private static Color toColor(String token) {
        for (Color color : Color.values()) {
            if (color.toString().equalsIgnoreCase(token)) {
                return color;
            }
        }
        return null;
    }

    Ansi apply(Ansi ansi) {
        if (bold) {
            ansi.bold();
        }
        if (color != null) {
            if (bright) {
                ansi.fgBright(color);
            } else {
                ansi.fg(color);
            }
        }
        if (bgColor != null) {
            if (bgBright) {
                ansi.bgBright(bgColor);
            } else {
                ansi.bg(bgColor);
            }
        }
        return ansi;
    }

    @Override
    public String toString() {
        if (!bold && color == null && bgColor == null) {
            return name();
        }
        StringBuilder sb = new StringBuilder(name() + '=');
        if (bold) {
            sb.append("bold");
        }
        if (color != null) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            if (bright) {
                sb.append("bright");
            }
            sb.append(color.name());
        }
        if (bgColor != null) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append("bg");
            if (bgBright) {
                sb.append("bright");
            }
            sb.append(bgColor.name());
        }
        return sb.toString();
    }
}
