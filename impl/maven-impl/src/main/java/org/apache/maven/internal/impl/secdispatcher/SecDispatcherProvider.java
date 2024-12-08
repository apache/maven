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
package org.apache.maven.internal.impl.secdispatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.codehaus.plexus.components.secdispatcher.Cipher;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.MasterSource;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.cipher.AESGCMNoPadding;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.MasterDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.sources.EnvMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.GpgAgentMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.PinEntryMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.SystemPropertyMasterSource;

/**
 * Delegate that offers just the minimal surface needed to decrypt settings.
 */
@SuppressWarnings("unused")
@Named
public class SecDispatcherProvider {

    @Provides
    public static SecDispatcher secDispatcher(Map<String, Dispatcher> dispatchers) {
        return new DefaultSecDispatcher(dispatchers, configurationFile());
    }

    @Provides
    @Named(LegacyDispatcher.NAME)
    public static Dispatcher legacyDispatcher() {
        return new LegacyDispatcher();
    }

    @Provides
    @Named(MasterDispatcher.NAME)
    public static Dispatcher masterDispatcher(
            Map<String, Cipher> masterCiphers, Map<String, MasterSource> masterSources) {
        return new MasterDispatcher(masterCiphers, masterSources);
    }

    @Provides
    @Named(AESGCMNoPadding.CIPHER_ALG)
    public static Cipher aesGcmNoPaddingCipher() {
        return new AESGCMNoPadding();
    }

    @Provides
    @Named(EnvMasterSource.NAME)
    public static MasterSource envMasterSource() {
        return new EnvMasterSource();
    }

    @Provides
    @Named(GpgAgentMasterSource.NAME)
    public static MasterSource gpgAgentMasterSource() {
        return new GpgAgentMasterSource();
    }

    @Provides
    @Named(PinEntryMasterSource.NAME)
    public static MasterSource pinEntryMasterSource() {
        return new PinEntryMasterSource();
    }

    @Provides
    @Named(SystemPropertyMasterSource.NAME)
    public static MasterSource systemPropertyMasterSource() {
        return new SystemPropertyMasterSource();
    }

    private static Path configurationFile() {
        String settingsSecurity = System.getProperty(Constants.MAVEN_SETTINGS_SECURITY);
        if (settingsSecurity != null) {
            return Paths.get(settingsSecurity);
        }
        String mavenUserConf = System.getProperty(Constants.MAVEN_USER_CONF);
        if (mavenUserConf != null) {
            return Paths.get(mavenUserConf, Constants.MAVEN_SETTINGS_SECURITY_FILE_NAME);
        }
        return Paths.get(System.getProperty("user.home"), ".m2", Constants.MAVEN_SETTINGS_SECURITY_FILE_NAME);
    }
}
