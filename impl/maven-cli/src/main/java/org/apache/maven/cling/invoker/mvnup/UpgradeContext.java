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
package org.apache.maven.cling.invoker.mvnup;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.LookupContext;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Indentation;

@SuppressWarnings("VisibilityModifier")
public class UpgradeContext extends LookupContext {
    public UpgradeContext(InvokerRequest invokerRequest, UpgradeOptions upgradeOptions) {
        super(invokerRequest, true, upgradeOptions);
    }

    public Map<String, Goal> goals;

    public List<AttributedString> header;
    public AttributedStyle style;
    public LineReader reader;

    // Indentation control for nested logging
    private int indentLevel = 0;
    private String indentString = Indentation.DEFAULT;

    // Console compatibility - use ASCII fallbacks for systems that don't support Unicode
    private final boolean useUnicodeIcons = supportsUnicode();

    public void addInHeader(String text) {
        addInHeader(AttributedStyle.DEFAULT, text);
    }

    public void addInHeader(AttributedStyle style, String text) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(style).append(text);
        header.add(asb.toAttributedString());
    }

    /**
     * Increases the indentation level for nested logging.
     */
    public void indent() {
        indentLevel++;
    }

    /**
     * Decreases the indentation level for nested logging.
     */
    public void unindent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    /**
     * Sets the indentation string to use (e.g., "  ", "    ", "\t").
     */
    public void setIndentString(String indentString) {
        this.indentString = indentString != null ? indentString : Indentation.DEFAULT;
    }

    /**
     * Gets the current indentation prefix based on the current level.
     */
    private String getCurrentIndent() {
        if (indentLevel == 0) {
            return "";
        }
        return indentString.repeat(indentLevel);
    }

    /**
     * Logs an informational message with current indentation.
     */
    public void info(String message) {
        logger.info(getCurrentIndent() + message);
    }

    /**
     * Logs a debug message with current indentation.
     */
    public void debug(String message) {
        logger.debug(getCurrentIndent() + message);
    }

    /**
     * Prints a new line.
     */
    public void println() {
        logger.info("");
    }

    // Semantic logging methods with icons for upgrade operations

    /**
     * Logs a successful operation with a checkmark icon.
     */
    public void success(String message) {
        String icon = useUnicodeIcons ? "✓" : "[OK]";
        logger.info(getCurrentIndent() + icon + " " + message);
    }

    /**
     * Logs an error with an X icon.
     */
    public void failure(String message) {
        String icon = useUnicodeIcons ? "✗" : "[ERROR]";
        logger.error(getCurrentIndent() + icon + " " + message);
    }

    /**
     * Logs a warning with a warning icon.
     */
    public void warning(String message) {
        String icon = useUnicodeIcons ? "⚠" : "[WARNING]";
        logger.warn(getCurrentIndent() + icon + " " + message);
    }

    /**
     * Logs detailed information with a bullet point.
     */
    public void detail(String message) {
        String icon = useUnicodeIcons ? "•" : "-";
        logger.info(getCurrentIndent() + icon + " " + message);
    }

    /**
     * Logs a performed action with an arrow icon.
     */
    public void action(String message) {
        String icon = useUnicodeIcons ? "→" : ">";
        logger.info(getCurrentIndent() + icon + " " + message);
    }

    /**
     * Gets the UpgradeOptions from the invoker request.
     * This provides convenient access to upgrade-specific options without casting.
     *
     * @return the UpgradeOptions
     */
    @Nonnull
    public UpgradeOptions options() {
        return (UpgradeOptions) super.options();
    }

    /**
     * Detects if the current console supports Unicode characters.
     * Uses the terminal's stdout encoding to determine Unicode support.
     *
     * @return true if Unicode is likely supported, false otherwise
     */
    private boolean supportsUnicode() {
        try {
            // Use the terminal's actual stdout encoding if available
            if (terminal != null && terminal.stdoutEncoding() != null) {
                String encoding = terminal.stdoutEncoding().name().toLowerCase();
                // UTF-8 and UTF-16 encodings support Unicode
                return encoding.contains("utf");
            }
        } catch (Exception e) {
            // If we can't determine the terminal encoding, fall back to system encoding
        }

        // Fallback to system file encoding
        String systemEncoding = System.getProperty("file.encoding", "").toLowerCase();
        return systemEncoding.contains("utf");
    }
}
