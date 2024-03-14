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

import javax.inject.Named;
import javax.inject.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.inject.AbstractModule;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.Binding;
import org.apache.maven.di.impl.InjectorImpl;
import org.codehaus.plexus.PlexusContainer;

@Named
class SisuDiBridgeModule extends AbstractModule {

    @Override
    protected void configure() {
        Provider<PlexusContainer> containerProvider = getProvider(PlexusContainer.class);

        Injector injector = new InjectorImpl() {
            @Override
            protected <T> Set<Binding<T>> getBindings(Key<T> key) {
                Set<Binding<T>> bindings = super.getBindings(key);
                if (bindings == null && key.getRawType() != List.class && key.getRawType() != Map.class) {
                    try {
                        T t = containerProvider.get().lookup(key.getRawType());
                        bindings = Set.of(new Binding.BindingToInstance<>(t));
                    } catch (Throwable e) {
                        // ignore
                        e.printStackTrace();
                    }
                }
                return bindings;
            }
        };
        injector.bindInstance(Injector.class, injector);
        bind(Injector.class).toInstance(injector);

        Stream.of(
                        DefaultArtifactCoordinateFactory.class,
                        DefaultArtifactDeployer.class,
                        DefaultArtifactFactory.class,
                        DefaultArtifactInstaller.class,
                        DefaultArtifactResolver.class,
                        DefaultChecksumAlgorithmService.class,
                        DefaultDependencyCollector.class,
                        DefaultDependencyCoordinateFactory.class,
                        DefaultLocalRepositoryManager.class,
                        DefaultMessageBuilderFactory.class,
                        DefaultModelXmlFactory.class,
                        DefaultRepositoryFactory.class,
                        DefaultSettingsBuilder.class,
                        DefaultSettingsXmlFactory.class,
                        DefaultToolchainsBuilder.class,
                        DefaultToolchainsXmlFactory.class,
                        DefaultTransportProvider.class,
                        DefaultVersionParser.class,
                        DefaultVersionRangeResolver.class,
                        DefaultVersionResolver.class)
                .forEach((Class<?> clazz) -> {
                    injector.bindImplicit(clazz);
                    Class<Object> itf = (Class) clazz.getInterfaces()[0];
                    bind(itf).toProvider(() -> injector.getInstance(clazz));
                });
    }
}
