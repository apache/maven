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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.AbstractModule;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.api.spi.LanguageProvider;
import org.apache.maven.api.spi.LifecycleProvider;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.PackagingProvider;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.Binding;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.di.impl.InjectorImpl;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.internal.aether.LegacyRepositorySystemSessionExtender;
import org.apache.maven.internal.impl.model.BuildModelTransformer;
import org.apache.maven.internal.impl.model.DefaultDependencyManagementImporter;
import org.apache.maven.internal.impl.model.DefaultDependencyManagementInjector;
import org.apache.maven.internal.impl.model.DefaultInheritanceAssembler;
import org.apache.maven.internal.impl.model.DefaultLifecycleBindingsInjector;
import org.apache.maven.internal.impl.model.DefaultModelBuilder;
import org.apache.maven.internal.impl.model.DefaultModelInterpolator;
import org.apache.maven.internal.impl.model.DefaultModelNormalizer;
import org.apache.maven.internal.impl.model.DefaultModelPathTranslator;
import org.apache.maven.internal.impl.model.DefaultModelProcessor;
import org.apache.maven.internal.impl.model.DefaultModelValidator;
import org.apache.maven.internal.impl.model.DefaultModelVersionProcessor;
import org.apache.maven.internal.impl.model.DefaultPathTranslator;
import org.apache.maven.internal.impl.model.DefaultPluginManagementInjector;
import org.apache.maven.internal.impl.model.DefaultProfileInjector;
import org.apache.maven.internal.impl.model.DefaultProfileSelector;
import org.apache.maven.internal.impl.model.DefaultRootLocator;
import org.apache.maven.internal.impl.model.ProfileActivationFilePathInterpolator;
import org.apache.maven.internal.impl.model.profile.FileProfileActivator;
import org.apache.maven.internal.impl.model.profile.JdkVersionProfileActivator;
import org.apache.maven.internal.impl.model.profile.OperatingSystemProfileActivator;
import org.apache.maven.internal.impl.model.profile.PackagingProfileActivator;
import org.apache.maven.internal.impl.model.profile.PropertyProfileActivator;
import org.apache.maven.internal.impl.resolver.DefaultArtifactDescriptorReader;
import org.apache.maven.internal.impl.resolver.DefaultVersionSchemeProvider;
import org.apache.maven.internal.impl.resolver.relocation.DistributionManagementArtifactRelocationSource;
import org.apache.maven.internal.impl.resolver.relocation.UserPropertiesArtifactRelocationSource;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.version.VersionScheme;

@Named
class SisuDiBridgeModule extends AbstractModule {

    @Override
    protected void configure() {
        Provider<PlexusContainer> containerProvider = getProvider(PlexusContainer.class);

        Injector injector = new InjectorImpl() {
            @Override
            public <Q> Supplier<Q> getCompiledBinding(Key<Q> key) {
                Set<Binding<Q>> res = getBindings(key);
                if (res != null && !res.isEmpty()) {
                    List<Binding<Q>> bindingList = new ArrayList<>(res);
                    Comparator<Binding<Q>> comparing = Comparator.comparing(Binding::getPriority);
                    bindingList.sort(comparing.reversed());
                    Binding<Q> binding = bindingList.get(0);
                    return compile(binding);
                }
                if (key.getRawType() == List.class) {
                    Set<Binding<Object>> res2 = getBindings(key.getTypeParameter(0));
                    Set<Binding<Object>> res3 = res2 != null ? new HashSet<>(res2) : new HashSet<>();
                    try {
                        List<Object> l = containerProvider
                                .get()
                                .lookupList(key.getTypeParameter(0).getRawType());
                        l.forEach(o -> res3.add(new Binding.BindingToInstance<>(o)));
                    } catch (Throwable e) {
                        // ignore
                        e.printStackTrace();
                    }
                    List<Supplier<Object>> list =
                            res3.stream().map(this::compile).collect(Collectors.toList());
                    //noinspection unchecked
                    return () -> (Q) list(list);
                }
                if (key.getRawType() == Map.class) {
                    Key<?> k = key.getTypeParameter(0);
                    Key<Object> v = key.getTypeParameter(1);
                    if (k.getRawType() == String.class) {
                        Set<Binding<Object>> res2 = getBindings(v);
                        Set<Binding<Object>> res3 = res2 != null ? new HashSet<>(res2) : new HashSet<>();
                        Map<String, Supplier<Object>> map = res3.stream()
                                .filter(b -> b.getOriginalKey() == null
                                        || b.getOriginalKey().getQualifier() == null
                                        || b.getOriginalKey().getQualifier() instanceof String)
                                .collect(Collectors.toMap(
                                        b -> (String)
                                                (b.getOriginalKey() != null
                                                        ? b.getOriginalKey().getQualifier()
                                                        : null),
                                        this::compile));
                        //noinspection unchecked
                        return () -> (Q) map(map);
                    }
                }
                try {
                    Q t = containerProvider.get().lookup(key.getRawType());
                    return compile(new Binding.BindingToInstance<>(t));
                } catch (Throwable e) {
                    // ignore
                    e.printStackTrace();
                }
                throw new DIException("No binding to construct an instance for key "
                        + key.getDisplayString() + ".  Existing bindings:\n"
                        + getBoundKeys().stream()
                                .map(Key::toString)
                                .map(String::trim)
                                .sorted()
                                .distinct()
                                .collect(Collectors.joining("\n - ", " - ", "")));
            }
        };
        injector.bindInstance(Injector.class, injector);
        bind(Injector.class).toInstance(injector);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        try {
            for (Iterator<URL> it = classLoader
                            .getResources("META-INF/maven/org.apache.maven.api.di.Inject")
                            .asIterator();
                    it.hasNext(); ) {
                URL url = it.next();
                try (InputStream is = url.openStream()) {
                    String[] lines = new String(is.readAllBytes()).split("\n");
                    for (String className : lines) {
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            injector.bindImplicit(clazz);
                            Class<Object> itf = (Class)
                                    (clazz.isInterface()
                                            ? clazz
                                            : clazz.getInterfaces().length > 0 ? clazz.getInterfaces()[0] : null);
                            if (itf != null) {
                                bind(itf).toProvider(() -> injector.getInstance(clazz));
                            }
                        } catch (ClassNotFoundException e) {
                            // ignore
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new MavenException(e);
        }
        Stream.of(
                        LanguageProvider.class,
                        LifecycleProvider.class,
                        PackagingProvider.class,
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
                        DefaultVersionResolver.class,
                        DefaultVersionSchemeProvider.class,
                        VersionScheme.class,
                        DefaultModelVersionParser.class,
                        DefaultRepositorySystemSessionFactory.class,
                        LegacyRepositorySystemSessionExtender.class,
                        ExtensibleEnumRegistries.DefaultLanguageRegistry.class,
                        ExtensibleEnumRegistries.DefaultPathScopeRegistry.class,
                        ExtensibleEnumRegistries.DefaultProjectScopeRegistry.class,
                        DefaultModelBuilder.class,
                        DefaultModelProcessor.class,
                        ModelParser.class,
                        DefaultModelValidator.class,
                        DefaultModelVersionProcessor.class,
                        DefaultModelNormalizer.class,
                        DefaultModelInterpolator.class,
                        DefaultPathTranslator.class,
                        DefaultUrlNormalizer.class,
                        DefaultRootLocator.class,
                        DefaultModelPathTranslator.class,
                        DefaultModelUrlNormalizer.class,
                        DefaultSuperPomProvider.class,
                        DefaultInheritanceAssembler.class,
                        DefaultProfileSelector.class,
                        ProfileActivator.class,
                        DefaultProfileInjector.class,
                        DefaultPluginManagementInjector.class,
                        DefaultDependencyManagementInjector.class,
                        DefaultDependencyManagementImporter.class,
                        DefaultLifecycleBindingsInjector.class,
                        DefaultPluginConfigurationExpander.class,
                        ProfileActivationFilePathInterpolator.class,
                        BuildModelTransformer.class,
                        DefaultArtifactDescriptorReader.class,
                        DistributionManagementArtifactRelocationSource.class,
                        UserPropertiesArtifactRelocationSource.class,
                        FileProfileActivator.class,
                        JdkVersionProfileActivator.class,
                        OperatingSystemProfileActivator.class,
                        PackagingProfileActivator.class,
                        PropertyProfileActivator.class)
                .forEach((Class<?> clazz) -> {
                    injector.bindImplicit(clazz);
                    Class<Object> itf = (Class)
                            (clazz.isInterface()
                                    ? null
                                    : clazz.getInterfaces().length > 0 ? clazz.getInterfaces()[0] : null);
                    if (itf != null) {
                        bind(itf).toProvider(() -> injector.getInstance(clazz));
                    }
                });
    }
}
