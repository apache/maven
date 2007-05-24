package org.apache.maven.settings;

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

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

/**
 * Builder for the user or global settings. By default, the settings files are located:
 * <ul>
 * <li>user settings: ${user.home}/settings.xml</li>
 * <li>global settings: ${maven.home}/conf/settings.xml</li>
 * </ul>
 *
 * @author jdcasey
 * @version $Id$
 */
public interface MavenSettingsBuilder
{
    String ROLE = MavenSettingsBuilder.class.getName();

    /**
     *
     * @param userSettingsFile
     * @param globalSettingsFile
     * @return a <code>Settings</code> object from the user and global settings file.
     * @throws IOException if any
     * @throws XmlPullParserException if any
     * @since 2.1
     */
    Settings buildSettings( File userSettingsFile, File globalSettingsFile )
        throws IOException, XmlPullParserException;
}
