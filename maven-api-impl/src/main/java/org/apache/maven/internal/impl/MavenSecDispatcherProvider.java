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
package org.apache.maven.internal.impl;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.codehaus.plexus.components.cipher.PlexusCipher;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;

/**
 * This class implements "Maven specific" {@link SecDispatcher}.
 */
@Named
public class MavenSecDispatcherProvider {
    @Singleton
    @Provides
    public static SecDispatcher secDispatcher(PlexusCipher plexusCipher, Map<String, Dispatcher> dispatchers) {
        return new DefaultSecDispatcher(
                plexusCipher,
                dispatchers,
                Paths.get(System.getProperty(Constants.MAVEN_USER_CONF), "settings-security.xml"));
    }
}
