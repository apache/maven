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
    public UpgradeContext(InvokerRequest invokerRequest) {
        this(invokerRequest, true);
    }

    public UpgradeContext(InvokerRequest invokerRequest, boolean containerCapsuleManaged) {
        super(invokerRequest, containerCapsuleManaged);
    }

    public Map<String, Goal> goals;

    public List<AttributedString> header;
    public AttributedStyle style;
    public LineReader reader;

    // Indentation control for nested logging
    private int indentLevel = 0;
    private String indentString = Indentation.DEFAULT;

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
        logger.info(getCurrentIndent() + "✓ " + message);
    }

    /**
     * Logs an error with an X icon.
     */
    public void failure(String message) {
        logger.error(getCurrentIndent() + "✗ " + message);
    }

    /**
     * Logs a warning with a warning icon.
     */
    public void warning(String message) {
        logger.warn(getCurrentIndent() + "⚠ " + message);
    }

    /**
     * Logs detailed information with a bullet point.
     */
    public void detail(String message) {
        logger.info(getCurrentIndent() + "• " + message);
    }

    /**
     * Logs a performed action with an arrow icon.
     */
    public void action(String message) {
        logger.info(getCurrentIndent() + "→ " + message);
    }

    /**
     * Gets the UpgradeOptions from the invoker request.
     * This provides convenient access to upgrade-specific options without casting.
     *
     * @return the UpgradeOptions
     */
    @Nonnull
    public UpgradeOptions options() {
        return invokerRequest().options();
    }

    /**
     * Gets the upgrade-specific invoker request with proper type casting.
     * This method provides type-safe access to the UpgradeInvokerRequest,
     * which contains upgrade-specific options and configuration.
     *
     * @return the UpgradeInvokerRequest instance, never null
     * @throws ClassCastException if the invokerRequest is not an UpgradeInvokerRequest
     * @see #options() () for convenient access to upgrade options without casting
     */
    @Nonnull
    public UpgradeInvokerRequest invokerRequest() {
        return (UpgradeInvokerRequest) invokerRequest;
    }
}
