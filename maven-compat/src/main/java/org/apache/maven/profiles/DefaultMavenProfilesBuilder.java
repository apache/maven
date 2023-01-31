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
package org.apache.maven.profiles;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.maven.profiles.io.xpp3.ProfilesXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * DefaultMavenProfilesBuilder
 */
@Deprecated
@Component(role = MavenProfilesBuilder.class)
public class DefaultMavenProfilesBuilder extends AbstractLogEnabled implements MavenProfilesBuilder {
    private static final String PROFILES_XML_FILE = "profiles.xml";

    public ProfilesRoot buildProfiles(File basedir) throws IOException, XmlPullParserException {
        File profilesXml = new File(basedir, PROFILES_XML_FILE);

        ProfilesRoot profilesRoot = null;

        if (profilesXml.exists()) {
            ProfilesXpp3Reader reader = new ProfilesXpp3Reader();
            try (Reader profileReader = ReaderFactory.newXmlReader(profilesXml);
                    StringWriter sWriter = new StringWriter()) {
                IOUtil.copy(profileReader, sWriter);

                String rawInput = sWriter.toString();

                try {
                    RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
                    interpolator.addValueSource(new EnvarBasedValueSource());

                    rawInput = interpolator.interpolate(rawInput, "settings");
                } catch (Exception e) {
                    getLogger()
                            .warn("Failed to initialize environment variable resolver. Skipping environment "
                                    + "substitution in " + PROFILES_XML_FILE + ".");
                    getLogger().debug("Failed to initialize envar resolver. Skipping resolution.", e);
                }

                StringReader sReader = new StringReader(rawInput);

                profilesRoot = reader.read(sReader);
            }
        }

        return profilesRoot;
    }
}
