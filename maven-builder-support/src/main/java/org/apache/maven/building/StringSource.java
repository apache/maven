package org.apache.maven.building;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps an ordinary {@link CharSequence} as a source.
 *
 * @author Benjamin Bentmann
 */
public class StringSource
    implements Source
{
    private final String content;

    private final String location;

    private final int hashCode;

    /**
     * Creates a new source backed by the specified string.
     *
     * @param content The String representation, may be empty or {@code null}.
     */
    public StringSource( CharSequence content )
    {
        this( content, null );
    }

    /**
     * Creates a new source backed by the specified string.
     *
     * @param content The String representation, may be empty or {@code null}.
     * @param location The location to report for this use, may be {@code null}.
     */
    public StringSource( CharSequence content, String location )
    {
        this.content = ( content != null ) ? content.toString() : "";
        this.location = ( location != null ) ? location : "(memory)";
        this.hashCode = this.content.hashCode();
    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        return new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) );
    }

    @Override
    public String getLocation()
    {
        return location;
    }

    /**
     * Gets the content of this source.
     *
     * @return The underlying character stream, never {@code null}.
     */
    public String getContent()
    {
        return content;
    }

    @Override
    public String toString()
    {
        return getLocation();
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( !StringSource.class.equals( obj.getClass() ) )
        {
            return false;
        }

        StringSource other = (StringSource) obj;
        return this.content.equals( other.content );
    }
}
