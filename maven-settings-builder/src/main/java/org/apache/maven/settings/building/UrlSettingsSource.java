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

import java.net.URL;

import org.apache.maven.building.UrlSource;

/**
 * Wraps an ordinary {@link URL} as a settings source.
 *
 * @author Benjamin Bentmann
 *
 * @deprecated instead use {@link UrlSource}
 */
@Deprecated
public class UrlSettingsSource extends UrlSource
    implements SettingsSource
{

    /**
     * Creates a new model source backed by the specified URL.
     *
     * @param settingsUrl The settings URL, must not be {@code null}.
     */
    public UrlSettingsSource( URL settingsUrl )
    {
        super( settingsUrl );
    }

    /**
     * Gets the settings URL of this model source.
     *
     * @return The underlying settings URL, never {@code null}.
     * @deprecated instead use {@link #getUrl()}
     */
    @Deprecated
    public URL getSettingsUrl()
    {
        return getUrl();
    }

}
