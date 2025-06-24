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
package org.apache.maven.settings.crypto;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;

/**
 * This class implements "Maven specific" {@link SecDispatcher}.
 *
 * @deprecated since 4.0.0
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class MavenSecDispatcher extends DefaultSecDispatcher {
    private static final String FILE_NAME = "settings-security4.xml";

    @Inject
    public MavenSecDispatcher(Map<String, Dispatcher> dispatchers) {
        super(dispatchers, configurationFile());
    }

    private static Path configurationFile() {
        String mavenUserConf = System.getProperty(Constants.MAVEN_USER_CONF);
        if (mavenUserConf != null) {
            return Paths.get(mavenUserConf, FILE_NAME);
        }
        // this means we are in UT or alike
        return Paths.get(System.getProperty("user.home"), ".m2", FILE_NAME);
    }
}
