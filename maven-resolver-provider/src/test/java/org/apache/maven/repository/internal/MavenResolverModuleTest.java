package org.apache.maven.repository.internal;

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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.Test;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

public class MavenResolverModuleTest
{
    @Test
    public void smokeTest()
    {
        Guice.createInjector(new MavenResolverModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(RepositoryConnectorFactory.class).to(BasicRepositoryConnectorFactory.class);
                bind(TransporterFactory.class).to(FileTransporterFactory.class);
            }

            @Provides
            @Singleton
            Set<RepositoryConnectorFactory> provideRepositoryConnectorFactory(
                    BasicRepositoryConnectorFactory basicRepositoryConnectorFactory )
            {
                return Collections.singleton( basicRepositoryConnectorFactory );
            }

            @Provides
            @Singleton
            Set<TransporterFactory> provideTransporterFactory(
                    FileTransporterFactory fileTransporterFactory )
            {
                return Collections.singleton( fileTransporterFactory );
            }

        }).getInstance(RepositorySystem.class);
    }
}
