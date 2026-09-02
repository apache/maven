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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.util.List;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.impl.standalone.ApiRunner;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;

/**
 * DI producer for the standalone Maven 4 API {@link Session} used by all
 * {@link UpgradeStrategy} implementations in mvnup.
 *
 * <p>The {@code @Provides @Singleton} method ensures a single Session instance is
 * shared across all strategies. The Session's
 * {@link org.apache.maven.api.cache.RequestCache} deduplicates effective model
 * builds when the same POM path is resolved more than once within a strategy.
 *
 * <p>The Session is created via {@link ApiRunner#createSession} (which bootstraps
 * its own standalone DI container for resolver services), then configured with
 * the remote repositories needed for parent POM resolution.
 */
@Named
class MvnupSessionHolder {

    @Provides
    @Singleton
    @Named("mvnup")
    static Session createSession() {
        Session session = ApiRunner.createSession(injector -> {
            injector.bindInstance(Dispatcher.class, new LegacyDispatcher());
            injector.bindImplicit(TransporterFactoryConfig.class);
        });

        // TODO: we should read settings
        RemoteRepository central =
                session.createRemoteRepository(RemoteRepository.CENTRAL_ID, "https://repo.maven.apache.org/maven2");
        RemoteRepository snapshots = session.getService(RepositoryFactory.class)
                .createRemote(Repository.newBuilder()
                        .id("apache-snapshots")
                        .url("https://repository.apache.org/content/repositories/snapshots/")
                        .releases(RepositoryPolicy.newBuilder().enabled("false").build())
                        .snapshots(RepositoryPolicy.newBuilder().enabled("true").build())
                        .build());

        return session.withRemoteRepositories(List.of(central, snapshots));
    }

    static class TransporterFactoryConfig {
        @Provides
        @Named(ApacheTransporterFactory.NAME)
        static TransporterFactory apacheTransporterFactory(
                ChecksumExtractor checksumExtractor, PathProcessor pathProcessor) {
            return new ApacheTransporterFactory(checksumExtractor, pathProcessor);
        }

        @Provides
        @Named(FileTransporterFactory.NAME)
        static TransporterFactory fileTransporterFactory() {
            return new FileTransporterFactory();
        }
    }
}
