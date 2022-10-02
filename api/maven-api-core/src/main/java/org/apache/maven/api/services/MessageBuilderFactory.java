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

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * A factory for {@link MessageBuilder}.
 *
 * @since 4.0
 */
@Experimental
public interface MessageBuilderFactory extends Service
{
    /**
     * Checks if the underlying output does support styling or not.
     * @return whether color styling is supported or not
     */
    boolean isColorEnabled();

    /**
     * Returns the terminal width or <code>-1</code> if not supported.
     * @return the terminal width
     */
    int getTerminalWidth();

    /**
     * Creates a new message builder.
     * @return a new message builder
     */
    @Nonnull
    MessageBuilder builder();

    /**
     * Creates a new message builder backed by the given string builder.
     * @param stringBuilder a string builder
     * @return a new message builder
     */
    @Nonnull
    MessageBuilder builder( @Nonnull StringBuilder stringBuilder );

    /**
     * Creates a new message builder of the specified size.
     * @param size the initial size of the message builder buffer
     * @return a new message builder
     */
    @Nonnull
    default MessageBuilder builder( int size )
    {
        return builder( new StringBuilder( size ) );
    }
}
