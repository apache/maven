package org.apache.maven.api.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Nonnull;

/**
 * Message builder that supports configurable styling.
 * @see MessageBuilderFactory
 */
public interface MessageBuilder
{
    /**
     * Append message content in success style.
     * By default, bold green
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder success( Object message );
    
    /**
     * Append message content in warning style.
     * By default, bold yellow
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder warning( Object message );
    
    /**
     * Append message content in failure style.
     * By default, bold red
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder failure( Object message );

    /**
     * Append message content in strong style.
     * By default, bold
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder strong( Object message );
    
    /**
     * Append message content in mojo style.
     * By default, green
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder mojo( Object message );
    
    /**
     * Append message content in project style.
     * By default, cyan
     * @param message the message to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder project( Object message );
    
    //
    // message building methods modelled after Ansi methods
    //
    /**
     * Append content to the message buffer.
     * @param value the content to append
     * @param offset the index of the first {@code char} to append
     * @param len the number of {@code char}s to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder a( char[] value, int offset, int len );

    /**
     * Append content to the message buffer.
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder a( char[] value );

    /**
     * Append content to the message buffer.
     * @param value the content to append
     * @param start the starting index of the subsequence to be appended
     * @param end the end index of the subsequence to be appended
     * @return the current builder
     */
    @Nonnull
    MessageBuilder a( CharSequence value, int start, int end );

    /**
     * Append content to the message buffer.
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder a( CharSequence value );

    /**
     * Append content to the message buffer.
     * @param value the content to append
     * @return the current builder
     */
    @Nonnull
    MessageBuilder a( Object value );

    /**
     * Append newline to the message buffer.
     * @return the current builder
     */
    @Nonnull
    MessageBuilder newline();

    /**
     * Append formatted content to the buffer.
     * @see String#format(String, Object...)
     * @param pattern a <a href="../util/Formatter.html#syntax">format string</a>
     * @param args arguments referenced by the format specifiers in the format string.
     * @return the current builder
     */
    @Nonnull
    MessageBuilder format( String pattern, Object... args );
}
