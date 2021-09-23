package org.apache.maven.settings.crypto;

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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

@Named( "maven" )
@Singleton
public final class MavenSecDispatcherProvider
    implements Provider<SecDispatcher>
{
    private final SecDispatcher secDispatcher;

    @Inject
    public MavenSecDispatcherProvider( final Map<String, PasswordDecryptor> decryptors )
    {
        this.secDispatcher = new DefaultSecDispatcher(
            new DefaultPlexusCipher(),
            decryptors,
            "~/.m2/settings-security.xml"
        );
    }

    @Override
    public SecDispatcher get()
    {
        return secDispatcher;
    }
}
