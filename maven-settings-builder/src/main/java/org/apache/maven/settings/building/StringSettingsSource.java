package org.apache.maven.settings.building;

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

/**
 * Wraps an ordinary {@link CharSequence} as a settings source.
 * 
 * @author Benjamin Bentmann
 */
public class StringSettingsSource
    implements SettingsSource
{

    private String settings;

    private String location;

    /**
     * Creates a new settings source backed by the specified string.
     * 
     * @param settings The settings' string representation, may be empty or {@code null}.
     */
    public StringSettingsSource( CharSequence settings )
    {
        this( settings, null );
    }

    /**
     * Creates a new settings source backed by the specified string.
     * 
     * @param settings The settings' string representation, may be empty or {@code null}.
     * @param location The location to report for this use, may be {@code null}.
     */
    public StringSettingsSource( CharSequence settings, String location )
    {
        this.settings = ( settings != null ) ? settings.toString() : "";
        this.location = ( location != null ) ? location : "(memory)";
    }

    public InputStream getInputStream()
        throws IOException
    {
        return new ByteArrayInputStream( settings.getBytes( "UTF-8" ) );
    }

    public String getLocation()
    {
        return location;
    }

    /**
     * Gets the character sequence of this settings source.
     * 
     * @return The underlying character stream, never {@code null}.
     */
    public String getSettings()
    {
        return settings;
    }

    @Override
    public String toString()
    {
        return getLocation();
    }

}
