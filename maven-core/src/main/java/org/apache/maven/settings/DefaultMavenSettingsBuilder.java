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
package org.apache.maven.settings;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author jdcasey
 */
@Component(role = MavenSettingsBuilder.class)
public class DefaultMavenSettingsBuilder extends AbstractLogEnabled implements MavenSettingsBuilder {

    @Requirement
    private SettingsBuilder settingsBuilder;

    public Settings buildSettings() throws IOException, XmlPullParserException {
        File userSettingsFile = getFile(
                "${user.home}/.m2/settings.xml", "user.home", MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION);

        return buildSettings(userSettingsFile);
    }

    public Settings buildSettings(boolean useCachedSettings) throws IOException, XmlPullParserException {
        return buildSettings();
    }

    public Settings buildSettings(File userSettingsFile) throws IOException, XmlPullParserException {
        File globalSettingsFile = getFile(
                "${maven.conf}/settings.xml", "maven.conf", MavenSettingsBuilder.ALT_GLOBAL_SETTINGS_XML_LOCATION);

        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsFile(userSettingsFile);
        request.setGlobalSettingsFile(globalSettingsFile);
        request.setSystemProperties(SystemProperties.getSystemProperties());
        return build(request);
    }

    public Settings buildSettings(File userSettingsFile, boolean useCachedSettings)
            throws IOException, XmlPullParserException {
        return buildSettings(userSettingsFile);
    }

    private Settings build(SettingsBuildingRequest request) throws IOException, XmlPullParserException {
        try {
            return settingsBuilder.build(request).getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /** @since 2.1 */
    public Settings buildSettings(MavenExecutionRequest request) throws IOException, XmlPullParserException {
        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setUserSettingsFile(request.getUserSettingsFile());
        settingsRequest.setGlobalSettingsFile(request.getGlobalSettingsFile());
        settingsRequest.setUserProperties(request.getUserProperties());
        settingsRequest.setSystemProperties(request.getSystemProperties());

        return build(settingsRequest);
    }

    private File getFile(String pathPattern, String basedirSysProp, String altLocationSysProp) {
        // -------------------------------------------------------------------------------------
        // Alright, here's the justification for all the regexp wizardry below...
        //
        // Continuum and other server-like apps may need to locate the user-level and
        // global-level settings somewhere other than ${user.home} and ${maven.home},
        // respectively. Using a simple replacement of these patterns will allow them
        // to specify the absolute path to these files in a customized components.xml
        // file. Ideally, we'd do full pattern-evaluation against the sysprops, but this
        // is a first step. There are several replacements below, in order to normalize
        // the path character before we operate on the string as a regex input, and
        // in order to avoid surprises with the File construction...
        // -------------------------------------------------------------------------------------

        String path = System.getProperty(altLocationSysProp);

        if (StringUtils.isEmpty(path)) {
            // TODO This replacing shouldn't be necessary as user.home should be in the
            // context of the container and thus the value would be interpolated by Plexus
            String basedir = System.getProperty(basedirSysProp);
            if (basedir == null) {
                basedir = System.getProperty("user.dir");
            }

            basedir = basedir.replace("\\", "/");
            basedir = basedir.replace("$", "\\$");

            // basedirSysProp is non regexp and basedir too
            path = pathPattern.replace("${" + basedirSysProp + "}", basedir);
            path = path.replace("\\", "/");
            // ---------------------------------------------------------------------------------
            // I'm not sure if this last regexp was really intended to disallow the usage of
            // network paths as user.home directory. Unfortunately it did. I removed it and
            // have not detected any problems yet.
            // ---------------------------------------------------------------------------------
            // path = path.replaceAll( "//", "/" );

        }
        return new File(path).getAbsoluteFile();
    }
}
