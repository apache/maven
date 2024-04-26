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
package org.apache.maven.api.services;

import org.apache.maven.api.annotations.Nonnull;

/**
 * Message builder that supports configurable styling.
 *
 * @since 4.0.0
 * @see MessageBuilderFactory
 */
public interface MessageBuilder extends Appendable {

    /**
     * Append message content in trace style.
     * By default, bold magenta
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder trace(Object message) {
        return style(".trace:-bold,f:magenta", message);
    }

    /**
     * Append message content in debug style.
     * By default, bold cyan
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder debug(Object message) {
        return style(".debug:-bold,f:cyan", message);
    }

    /**
     * Append message content in info style.
     * By default, bold blue
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder info(Object message) {
        return style(".info:-bold,f:blue", message);
    }

    /**
     * Append message content in warning style.
     * By default, bold yellow
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder warning(Object message) {
        return style(".warning:-bold,f:yellow", message);
    }

    /**
     * Append message content in error style.
     * By default, bold red
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder error(Object message) {
        return style(".error:-bold,f:red", message);
    }

    /**
     * Append message content in success style.
     * By default, bold green
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder success(Object message) {
        return style(".success:-bold,f:green", message);
    }

    /**
     * Append message content in failure style.
     * By default, bold red
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder failure(Object message) {
        return style(".failure:-bold,f:red", message);
    }

    /**
     * Append message content in strong style.
     * By default, bold
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder strong(Object message) {
        return style(".strong:-bold", message);
    }

    /**
     * Append message content in mojo style.
     * By default, green
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder mojo(Object message) {
        return style(".mojo:-f:green", message);
    }

    /**
     * Append message content in project style.
     * By default, cyan
     *
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder project(Object message) {
        return style(".project:-f:cyan", message);
    }

    @Nonnull
    default MessageBuilder style(String style, Object message) {
        return style(style).a(message).resetStyle();
    }

    MessageBuilder style(String style);

    MessageBuilder resetStyle();

    //
    // message building methods modelled after Ansi methods
    //

    @Nonnull
    @Override
    MessageBuilder append(CharSequence cs);

    @Nonnull
    @Override
    MessageBuilder append(CharSequence cs, int start, int end);

    @Nonnull
    @Override
    MessageBuilder append(char c);

    /**
     * Append content to the message buffer.
     *
     * @param value the content to append
     * @param offset the index of the first {@code char} to append
     * @param len the number of {@code char}s to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder a(char[] value, int offset, int len) {
        return append(String.valueOf(value, offset, len));
    }

    /**
     * Append content to the message buffer.
     *
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder a(char[] value) {
        return append(String.valueOf(value));
    }

    /**
     * Append content to the message buffer.
     *
     * @param value the content to append
     * @param start the starting index of the subsequence to be appended
     * @param end the end index of the subsequence to be appended
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder a(CharSequence value, int start, int end) {
        return append(value, start, end);
    }

    /**
     * Append content to the message buffer.
     *
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder a(CharSequence value) {
        return append(value);
    }

    /**
     * Append content to the message buffer.
     *
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder a(Object value) {
        return append(String.valueOf(value));
    }

    /**
     * Append newline to the message buffer.
     *
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder newline() {
        return append(System.lineSeparator());
    }

    /**
     * Append formatted content to the buffer.
     * @see String#format(String, Object...)
     *
     * @param pattern a <a href="../util/Formatter.html#syntax">format string</a>
     * @param args arguments referenced by the format specifiers in the format string
     * @return the current builder
     */
    @Nonnull
    default MessageBuilder format(String pattern, Object... args) {
        return append(String.format(pattern, args));
    }

    /**
     * Set the buffer length.
     *
     * @param length the new length
     * @return the current builder
     */
    MessageBuilder setLength(int length);

    /**
     * Return the built message.
     *
     * @return the message
     */
    @Nonnull
    String build();
}
