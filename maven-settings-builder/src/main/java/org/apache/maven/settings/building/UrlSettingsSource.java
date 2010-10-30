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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Wraps an ordinary {@link URL} as a settings source.
 * 
 * @author Benjamin Bentmann
 */
public class UrlSettingsSource
    implements SettingsSource
{

    private URL settingsUrl;

    /**
     * Creates a new model source backed by the specified URL.
     * 
     * @param settingsUrl The settings URL, must not be {@code null}.
     */
    public UrlSettingsSource( URL settingsUrl )
    {
        if ( settingsUrl == null )
        {
            throw new IllegalArgumentException( "no settings URL specified" );
        }
        this.settingsUrl = settingsUrl;
    }

    public InputStream getInputStream()
        throws IOException
    {
        return settingsUrl.openStream();
    }

    public String getLocation()
    {
        return settingsUrl.toString();
    }

    /**
     * Gets the settings URL of this model source.
     * 
     * @return The underlying settings URL, never {@code null}.
     */
    public URL getSettingsUrl()
    {
        return settingsUrl;
    }

    @Override
    public String toString()
    {
        return getLocation();
    }

}
