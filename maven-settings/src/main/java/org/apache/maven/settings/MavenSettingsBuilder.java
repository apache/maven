package org.apache.maven.settings;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

/**
 * @author jdcasey
 * @version $Id: MavenSettingsBuilder.java 189510 2005-06-08 03:27:43Z jdcasey $
 */
public interface MavenSettingsBuilder
{
    String ROLE = MavenSettingsBuilder.class.getName();
    
    String ALT_USER_SETTINGS_XML_LOCATION = "org.apache.maven.user-settings";
    String ALT_GLOBAL_SETTINGS_XML_LOCATION = "org.apache.maven.global-settings";
    String ALT_LOCAL_REPOSITORY_LOCATION = "maven.repo.local";

    Settings buildSettings()
        throws IOException, XmlPullParserException;
    
    Settings buildSettings( File userSettingsFile )
        throws IOException, XmlPullParserException;
}
