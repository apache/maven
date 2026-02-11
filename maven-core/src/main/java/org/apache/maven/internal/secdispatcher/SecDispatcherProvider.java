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
package org.apache.maven.internal.secdispatcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Map;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

/**
 * Provides "maven" named security dispatcher, used by Maven. This component is configured slightly differently than the
 * default one, as default is unaware of Maven specific configuration file locations. The default named component
 * should not be used (injected or referenced in any way) at all in any Maven related codebase.
 * <p>
 * Note: This whole stuff is really deprecated and replaced with proper security in Maven 4, while this one is
 * just "security through obscurity".
 *
 * @since 3.9.13
 */
@Singleton
@Named("maven")
public class SecDispatcherProvider implements Provider<SecDispatcher> {
    private final SecDispatcher instance;

    @Inject
    public SecDispatcherProvider(
            PlexusCipher plexusCipher,
            Map<String, PasswordDecryptor> decryptors,
            @Named("${maven.settings.security.configurationFile:-~/.m2/settings-security.xml}")
                    String configurationFile) {
        instance = new DefaultSecDispatcher(plexusCipher, decryptors, configurationFile);
    }

    @Override
    public SecDispatcher get() {
        return instance;
    }
}
