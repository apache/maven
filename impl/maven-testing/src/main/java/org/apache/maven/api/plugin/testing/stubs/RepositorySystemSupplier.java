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
package org.apache.maven.api.plugin.testing.stubs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.impl.DefaultModelUrlNormalizer;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.apache.maven.impl.DefaultPluginConfigurationExpander;
import org.apache.maven.impl.DefaultSuperPomProvider;
import org.apache.maven.impl.DefaultUrlNormalizer;
import org.apache.maven.impl.model.DefaultDependencyManagementImporter;
import org.apache.maven.impl.model.DefaultDependencyManagementInjector;
import org.apache.maven.impl.model.DefaultInheritanceAssembler;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.impl.model.DefaultModelBuilder;
import org.apache.maven.impl.model.DefaultModelInterpolator;
import org.apache.maven.impl.model.DefaultModelNormalizer;
import org.apache.maven.impl.model.DefaultModelPathTranslator;
import org.apache.maven.impl.model.DefaultModelProcessor;
import org.apache.maven.impl.model.DefaultModelValidator;
import org.apache.maven.impl.model.DefaultPathTranslator;
import org.apache.maven.impl.model.DefaultPluginManagementInjector;
import org.apache.maven.impl.model.DefaultProfileInjector;
import org.apache.maven.impl.model.DefaultProfileSelector;
import org.apache.maven.impl.model.rootlocator.DefaultRootLocator;
import org.apache.maven.impl.resolver.DefaultArtifactDescriptorReader;
import org.apache.maven.impl.resolver.DefaultModelResolver;
import org.apache.maven.impl.resolver.DefaultVersionRangeResolver;
import org.apache.maven.impl.resolver.DefaultVersionResolver;
import org.apache.maven.impl.resolver.MavenArtifactRelocationSource;
import org.apache.maven.impl.resolver.PluginsMetadataGeneratorFactory;
import org.apache.maven.impl.resolver.SnapshotMetadataGeneratorFactory;
import org.apache.maven.impl.resolver.VersionsMetadataGeneratorFactory;
import org.apache.maven.impl.resolver.relocation.DistributionManagementArtifactRelocationSource;
import org.apache.maven.impl.resolver.relocation.UserPropertiesArtifactRelocationSource;
import org.apache.maven.impl.resolver.validator.MavenValidatorFactory;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.OfflineController;
import org.eclipse.aether.impl.RemoteRepositoryFilterManager;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.RepositorySystemLifecycle;
import org.eclipse.aether.impl.RepositorySystemValidator;
import org.eclipse.aether.impl.UpdateCheckManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactPredicateFactory;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultChecksumProcessor;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultPathProcessor;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultRepositorySystemLifecycle;
import org.eclipse.aether.internal.impl.DefaultRepositorySystemValidator;
import org.eclipse.aether.internal.impl.DefaultTrackingFileManager;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.internal.impl.checksum.DefaultChecksumAlgorithmFactorySelector;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha256ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha512ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.SparseDirectoryTrustedChecksumsSource;
import org.eclipse.aether.internal.impl.checksum.SummaryFileTrustedChecksumsSource;
import org.eclipse.aether.internal.impl.checksum.TrustedToProvidedChecksumsSourceAdapter;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate;
import org.eclipse.aether.internal.impl.collect.bf.BfDependencyCollector;
import org.eclipse.aether.internal.impl.collect.df.DfDependencyCollector;
import org.eclipse.aether.internal.impl.filter.DefaultRemoteRepositoryFilterManager;
import org.eclipse.aether.internal.impl.filter.FilteringPipelineRepositoryConnectorFactory;
import org.eclipse.aether.internal.impl.filter.GroupIdRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.filter.PrefixesRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.offline.OfflinePipelineRepositoryConnectorFactory;
import org.eclipse.aether.internal.impl.resolution.TrustedChecksumsArtifactResolverPostProcessor;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NameMappers;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactoryImpl;
import org.eclipse.aether.internal.impl.transport.http.DefaultChecksumExtractor;
import org.eclipse.aether.internal.impl.transport.http.Nx2ChecksumExtractor;
import org.eclipse.aether.internal.impl.transport.http.XChecksumExtractor;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.providers.FileLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalReadWriteLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalSemaphoreNamedLockFactory;
import org.eclipse.aether.named.providers.NoopNamedLockFactory;
import org.eclipse.aether.spi.artifact.ArtifactPredicateFactory;
import org.eclipse.aether.spi.artifact.decorator.ArtifactDecoratorFactory;
import org.eclipse.aether.spi.artifact.generator.ArtifactGeneratorFactory;
import org.eclipse.aether.spi.artifact.transformer.ArtifactTransformer;
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.checksums.TrustedChecksumsSource;
import org.eclipse.aether.spi.connector.PipelineRepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractorStrategy;
import org.eclipse.aether.spi.io.ChecksumProcessor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.spi.validator.ValidatorFactory;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;

/**
 * A simple memorizing {@link Supplier} of {@link RepositorySystem} instance, that on first call
 * supplies lazily constructed instance, and on each subsequent call same instance. Hence, this instance should be
 * thrown away immediately once repository system was created and there is no need for more instances. If new
 * repository system instance needed, new instance of this class must be created. For proper shut down of returned
 * repository system instance(s) use {@link RepositorySystem#shutdown()} method on supplied instance(s).
 * <p>
 * Since Resolver 2.0 this class offers access to various components via public getters, and allows even partial object
 * graph construction.
 * <p>
 * Extend this class {@code createXXX()} methods and override to customize, if needed. The contract of this class makes
 * sure that these (potentially overridden) methods are invoked only once, and instance created by those methods are
 * memorized and kept as long as supplier instance is kept open.
 * <p>
 * This class is not thread safe and must be used from one thread only, while the constructed {@link RepositorySystem}
 * is thread safe.
 * <p>
 * Important: Given the instance of supplier memorizes the supplier {@link RepositorySystem} instance it supplies,
 * their lifecycle is shared as well: once supplied repository system is shut-down, this instance becomes closed as
 * well. Any subsequent {@code getXXX} method invocation attempt will fail with {@link IllegalStateException}.
 */
public class RepositorySystemSupplier implements Supplier<RepositorySystem> {
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RepositorySystemSupplier() {}

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Supplier is closed");
        }
    }

    private PathProcessor pathProcessor;

    public final PathProcessor getPathProcessor() {
        checkClosed();
        if (pathProcessor == null) {
            pathProcessor = createPathProcessor();
        }
        return pathProcessor;
    }

    protected PathProcessor createPathProcessor() {
        return new DefaultPathProcessor();
    }

    private ChecksumProcessor checksumProcessor;

    public final ChecksumProcessor getChecksumProcessor() {
        checkClosed();
        if (checksumProcessor == null) {
            checksumProcessor = createChecksumProcessor();
        }
        return checksumProcessor;
    }

    protected ChecksumProcessor createChecksumProcessor() {
        return new DefaultChecksumProcessor(getPathProcessor());
    }

    private TrackingFileManager trackingFileManager;

    public final TrackingFileManager getTrackingFileManager() {
        checkClosed();
        if (trackingFileManager == null) {
            trackingFileManager = createTrackingFileManager();
        }
        return trackingFileManager;
    }

    protected TrackingFileManager createTrackingFileManager() {
        return new DefaultTrackingFileManager();
    }

    private LocalPathComposer localPathComposer;

    public final LocalPathComposer getLocalPathComposer() {
        checkClosed();
        if (localPathComposer == null) {
            localPathComposer = createLocalPathComposer();
        }
        return localPathComposer;
    }

    protected LocalPathComposer createLocalPathComposer() {
        return new DefaultLocalPathComposer();
    }

    private LocalPathPrefixComposerFactory localPathPrefixComposerFactory;

    public final LocalPathPrefixComposerFactory getLocalPathPrefixComposerFactory() {
        checkClosed();
        if (localPathPrefixComposerFactory == null) {
            localPathPrefixComposerFactory = createLocalPathPrefixComposerFactory();
        }
        return localPathPrefixComposerFactory;
    }

    protected LocalPathPrefixComposerFactory createLocalPathPrefixComposerFactory() {
        return new DefaultLocalPathPrefixComposerFactory();
    }

    private RepositorySystemLifecycle repositorySystemLifecycle;

    public final RepositorySystemLifecycle getRepositorySystemLifecycle() {
        checkClosed();
        if (repositorySystemLifecycle == null) {
            repositorySystemLifecycle = createRepositorySystemLifecycle();
            repositorySystemLifecycle.addOnSystemEndedHandler(() -> closed.set(true));
        }
        return repositorySystemLifecycle;
    }

    protected RepositorySystemLifecycle createRepositorySystemLifecycle() {
        return new DefaultRepositorySystemLifecycle();
    }

    private OfflineController offlineController;

    public final OfflineController getOfflineController() {
        checkClosed();
        if (offlineController == null) {
            offlineController = createOfflineController();
        }
        return offlineController;
    }

    protected OfflineController createOfflineController() {
        return new DefaultOfflineController();
    }

    private UpdatePolicyAnalyzer updatePolicyAnalyzer;

    public final UpdatePolicyAnalyzer getUpdatePolicyAnalyzer() {
        checkClosed();
        if (updatePolicyAnalyzer == null) {
            updatePolicyAnalyzer = createUpdatePolicyAnalyzer();
        }
        return updatePolicyAnalyzer;
    }

    protected UpdatePolicyAnalyzer createUpdatePolicyAnalyzer() {
        return new DefaultUpdatePolicyAnalyzer();
    }

    private ChecksumPolicyProvider checksumPolicyProvider;

    public final ChecksumPolicyProvider getChecksumPolicyProvider() {
        checkClosed();
        if (checksumPolicyProvider == null) {
            checksumPolicyProvider = createChecksumPolicyProvider();
        }
        return checksumPolicyProvider;
    }

    protected ChecksumPolicyProvider createChecksumPolicyProvider() {
        return new DefaultChecksumPolicyProvider();
    }

    private UpdateCheckManager updateCheckManager;

    public final UpdateCheckManager getUpdateCheckManager() {
        checkClosed();
        if (updateCheckManager == null) {
            updateCheckManager = createUpdateCheckManager();
        }
        return updateCheckManager;
    }

    protected UpdateCheckManager createUpdateCheckManager() {
        return new DefaultUpdateCheckManager(getTrackingFileManager(), getUpdatePolicyAnalyzer(), getPathProcessor());
    }

    private Map<String, NamedLockFactory> namedLockFactories;

    public final Map<String, NamedLockFactory> getNamedLockFactories() {
        checkClosed();
        if (namedLockFactories == null) {
            namedLockFactories = createNamedLockFactories();
        }
        return namedLockFactories;
    }

    protected Map<String, NamedLockFactory> createNamedLockFactories() {
        HashMap<String, NamedLockFactory> result = new HashMap<>();
        result.put(NoopNamedLockFactory.NAME, new NoopNamedLockFactory());
        result.put(LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory());
        result.put(LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory());
        result.put(FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory());
        return result;
    }

    private Map<String, NameMapper> nameMappers;

    public final Map<String, NameMapper> getNameMappers() {
        checkClosed();
        if (nameMappers == null) {
            nameMappers = createNameMappers();
        }
        return nameMappers;
    }

    protected Map<String, NameMapper> createNameMappers() {
        HashMap<String, NameMapper> result = new HashMap<>();
        result.put(NameMappers.STATIC_NAME, NameMappers.staticNameMapper());
        result.put(NameMappers.GAV_NAME, NameMappers.gavNameMapper());
        result.put(NameMappers.GAECV_NAME, NameMappers.gaecvNameMapper());
        result.put(NameMappers.DISCRIMINATING_NAME, NameMappers.discriminatingNameMapper());
        result.put(NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper());
        result.put(NameMappers.FILE_GAECV_NAME, NameMappers.fileGaecvNameMapper());
        result.put(NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper());
        result.put(NameMappers.FILE_HGAECV_NAME, NameMappers.fileHashingGaecvNameMapper());
        return result;
    }

    private NamedLockFactoryAdapterFactory namedLockFactoryAdapterFactory;

    public final NamedLockFactoryAdapterFactory getNamedLockFactoryAdapterFactory() {
        checkClosed();
        if (namedLockFactoryAdapterFactory == null) {
            namedLockFactoryAdapterFactory = createNamedLockFactoryAdapterFactory();
        }
        return namedLockFactoryAdapterFactory;
    }

    protected NamedLockFactoryAdapterFactory createNamedLockFactoryAdapterFactory() {
        return new NamedLockFactoryAdapterFactoryImpl(
                getNamedLockFactories(), getNameMappers(), getRepositorySystemLifecycle());
    }

    private SyncContextFactory syncContextFactory;

    public final SyncContextFactory getSyncContextFactory() {
        checkClosed();
        if (syncContextFactory == null) {
            syncContextFactory = createSyncContextFactory();
        }
        return syncContextFactory;
    }

    protected SyncContextFactory createSyncContextFactory() {
        return new DefaultSyncContextFactory(getNamedLockFactoryAdapterFactory());
    }

    private Map<String, ChecksumAlgorithmFactory> checksumAlgorithmFactories;

    public final Map<String, ChecksumAlgorithmFactory> getChecksumAlgorithmFactories() {
        checkClosed();
        if (checksumAlgorithmFactories == null) {
            checksumAlgorithmFactories = createChecksumAlgorithmFactories();
        }
        return checksumAlgorithmFactories;
    }

    protected Map<String, ChecksumAlgorithmFactory> createChecksumAlgorithmFactories() {
        HashMap<String, ChecksumAlgorithmFactory> result = new HashMap<>();
        result.put(Sha512ChecksumAlgorithmFactory.NAME, new Sha512ChecksumAlgorithmFactory());
        result.put(Sha256ChecksumAlgorithmFactory.NAME, new Sha256ChecksumAlgorithmFactory());
        result.put(Sha1ChecksumAlgorithmFactory.NAME, new Sha1ChecksumAlgorithmFactory());
        result.put(Md5ChecksumAlgorithmFactory.NAME, new Md5ChecksumAlgorithmFactory());
        return result;
    }

    private ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector;

    public final ChecksumAlgorithmFactorySelector getChecksumAlgorithmFactorySelector() {
        checkClosed();
        if (checksumAlgorithmFactorySelector == null) {
            checksumAlgorithmFactorySelector = createChecksumAlgorithmFactorySelector();
        }
        return checksumAlgorithmFactorySelector;
    }

    protected ChecksumAlgorithmFactorySelector createChecksumAlgorithmFactorySelector() {
        return new DefaultChecksumAlgorithmFactorySelector(getChecksumAlgorithmFactories());
    }

    private ArtifactPredicateFactory artifactPredicateFactory;

    public final ArtifactPredicateFactory getArtifactPredicateFactory() {
        checkClosed();
        if (artifactPredicateFactory == null) {
            artifactPredicateFactory = createArtifactPredicateFactory();
        }
        return artifactPredicateFactory;
    }

    protected ArtifactPredicateFactory createArtifactPredicateFactory() {
        return new DefaultArtifactPredicateFactory(getChecksumAlgorithmFactorySelector());
    }

    private Map<String, RepositoryLayoutFactory> repositoryLayoutFactories;

    public final Map<String, RepositoryLayoutFactory> getRepositoryLayoutFactories() {
        checkClosed();
        if (repositoryLayoutFactories == null) {
            repositoryLayoutFactories = createRepositoryLayoutFactories();
        }
        return repositoryLayoutFactories;
    }

    protected Map<String, RepositoryLayoutFactory> createRepositoryLayoutFactories() {
        HashMap<String, RepositoryLayoutFactory> result = new HashMap<>();
        result.put(
                Maven2RepositoryLayoutFactory.NAME,
                new Maven2RepositoryLayoutFactory(
                        getChecksumAlgorithmFactorySelector(), getArtifactPredicateFactory()));
        return result;
    }

    private RepositoryLayoutProvider repositoryLayoutProvider;

    public final RepositoryLayoutProvider getRepositoryLayoutProvider() {
        checkClosed();
        if (repositoryLayoutProvider == null) {
            repositoryLayoutProvider = createRepositoryLayoutProvider();
        }
        return repositoryLayoutProvider;
    }

    protected RepositoryLayoutProvider createRepositoryLayoutProvider() {
        return new DefaultRepositoryLayoutProvider(getRepositoryLayoutFactories());
    }

    private LocalRepositoryProvider localRepositoryProvider;

    public final LocalRepositoryProvider getLocalRepositoryProvider() {
        checkClosed();
        if (localRepositoryProvider == null) {
            localRepositoryProvider = createLocalRepositoryProvider();
        }
        return localRepositoryProvider;
    }

    protected LocalRepositoryProvider createLocalRepositoryProvider() {
        LocalPathComposer localPathComposer = getLocalPathComposer();
        HashMap<String, LocalRepositoryManagerFactory> localRepositoryProviders = new HashMap<>(2);
        localRepositoryProviders.put(
                SimpleLocalRepositoryManagerFactory.NAME, new SimpleLocalRepositoryManagerFactory(localPathComposer));
        localRepositoryProviders.put(
                EnhancedLocalRepositoryManagerFactory.NAME,
                new EnhancedLocalRepositoryManagerFactory(
                        localPathComposer, getTrackingFileManager(), getLocalPathPrefixComposerFactory()));
        return new DefaultLocalRepositoryProvider(localRepositoryProviders);
    }

    private RemoteRepositoryManager remoteRepositoryManager;

    public final RemoteRepositoryManager getRemoteRepositoryManager() {
        checkClosed();
        if (remoteRepositoryManager == null) {
            remoteRepositoryManager = createRemoteRepositoryManager();
        }
        return remoteRepositoryManager;
    }

    protected RemoteRepositoryManager createRemoteRepositoryManager() {
        return new DefaultRemoteRepositoryManager(getUpdatePolicyAnalyzer(), getChecksumPolicyProvider());
    }

    private Map<String, RemoteRepositoryFilterSource> remoteRepositoryFilterSources;

    public final Map<String, RemoteRepositoryFilterSource> getRemoteRepositoryFilterSources() {
        checkClosed();
        if (remoteRepositoryFilterSources == null) {
            remoteRepositoryFilterSources = createRemoteRepositoryFilterSources();
        }
        return remoteRepositoryFilterSources;
    }

    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        HashMap<String, RemoteRepositoryFilterSource> result = new HashMap<>();
        result.put(
                GroupIdRemoteRepositoryFilterSource.NAME,
                new GroupIdRemoteRepositoryFilterSource(getRepositorySystemLifecycle(), getPathProcessor()));
        result.put(
                PrefixesRemoteRepositoryFilterSource.NAME,
                new PrefixesRemoteRepositoryFilterSource(
                        this::getMetadataResolver, this::getRemoteRepositoryManager, getRepositoryLayoutProvider()));
        return result;
    }

    private RemoteRepositoryFilterManager remoteRepositoryFilterManager;

    public final RemoteRepositoryFilterManager getRemoteRepositoryFilterManager() {
        checkClosed();
        if (remoteRepositoryFilterManager == null) {
            remoteRepositoryFilterManager = createRemoteRepositoryFilterManager();
        }
        return remoteRepositoryFilterManager;
    }

    protected RemoteRepositoryFilterManager createRemoteRepositoryFilterManager() {
        return new DefaultRemoteRepositoryFilterManager(getRemoteRepositoryFilterSources());
    }

    private Map<String, RepositoryListener> repositoryListeners;

    public final Map<String, RepositoryListener> getRepositoryListeners() {
        checkClosed();
        if (repositoryListeners == null) {
            repositoryListeners = createRepositoryListeners();
        }
        return repositoryListeners;
    }

    protected Map<String, RepositoryListener> createRepositoryListeners() {
        return new HashMap<>();
    }

    private RepositoryEventDispatcher repositoryEventDispatcher;

    public final RepositoryEventDispatcher getRepositoryEventDispatcher() {
        checkClosed();
        if (repositoryEventDispatcher == null) {
            repositoryEventDispatcher = createRepositoryEventDispatcher();
        }
        return repositoryEventDispatcher;
    }

    protected RepositoryEventDispatcher createRepositoryEventDispatcher() {
        return new DefaultRepositoryEventDispatcher(getRepositoryListeners());
    }

    private Map<String, TrustedChecksumsSource> trustedChecksumsSources;

    public final Map<String, TrustedChecksumsSource> getTrustedChecksumsSources() {
        checkClosed();
        if (trustedChecksumsSources == null) {
            trustedChecksumsSources = createTrustedChecksumsSources();
        }
        return trustedChecksumsSources;
    }

    protected Map<String, TrustedChecksumsSource> createTrustedChecksumsSources() {
        HashMap<String, TrustedChecksumsSource> result = new HashMap<>();
        result.put(
                SparseDirectoryTrustedChecksumsSource.NAME,
                new SparseDirectoryTrustedChecksumsSource(getChecksumProcessor(), getLocalPathComposer()));
        result.put(
                SummaryFileTrustedChecksumsSource.NAME,
                new SummaryFileTrustedChecksumsSource(
                        getLocalPathComposer(), getRepositorySystemLifecycle(), getPathProcessor()));
        return result;
    }

    private Map<String, ProvidedChecksumsSource> providedChecksumsSources;

    public final Map<String, ProvidedChecksumsSource> getProvidedChecksumsSources() {
        checkClosed();
        if (providedChecksumsSources == null) {
            providedChecksumsSources = createProvidedChecksumsSources();
        }
        return providedChecksumsSources;
    }

    protected Map<String, ProvidedChecksumsSource> createProvidedChecksumsSources() {
        HashMap<String, ProvidedChecksumsSource> result = new HashMap<>();
        result.put(
                TrustedToProvidedChecksumsSourceAdapter.NAME,
                new TrustedToProvidedChecksumsSourceAdapter(getTrustedChecksumsSources()));
        return result;
    }

    private Map<String, ChecksumExtractorStrategy> checksumExtractorStrategies;

    public final Map<String, ChecksumExtractorStrategy> getChecksumExtractorStrategies() {
        checkClosed();
        if (checksumExtractorStrategies == null) {
            checksumExtractorStrategies = createChecksumExtractorStrategies();
        }
        return checksumExtractorStrategies;
    }

    protected Map<String, ChecksumExtractorStrategy> createChecksumExtractorStrategies() {
        HashMap<String, ChecksumExtractorStrategy> result = new HashMap<>();
        result.put(XChecksumExtractor.NAME, new XChecksumExtractor());
        result.put(Nx2ChecksumExtractor.NAME, new Nx2ChecksumExtractor());
        return result;
    }

    private ChecksumExtractor checksumExtractor;

    public final ChecksumExtractor getChecksumExtractor() {
        checkClosed();
        if (checksumExtractor == null) {
            checksumExtractor = createChecksumExtractor();
        }
        return checksumExtractor;
    }

    protected ChecksumExtractor createChecksumExtractor() {
        return new DefaultChecksumExtractor(getChecksumExtractorStrategies());
    }

    private Map<String, TransporterFactory> transporterFactories;

    public final Map<String, TransporterFactory> getTransporterFactories() {
        checkClosed();
        if (transporterFactories == null) {
            transporterFactories = createTransporterFactories();
        }
        return transporterFactories;
    }

    protected Map<String, TransporterFactory> createTransporterFactories() {
        HashMap<String, TransporterFactory> result = new HashMap<>();
        result.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        result.put(
                ApacheTransporterFactory.NAME,
                new ApacheTransporterFactory(getChecksumExtractor(), getPathProcessor()));
        return result;
    }

    private TransporterProvider transporterProvider;

    public final TransporterProvider getTransporterProvider() {
        checkClosed();
        if (transporterProvider == null) {
            transporterProvider = createTransporterProvider();
        }
        return transporterProvider;
    }

    protected TransporterProvider createTransporterProvider() {
        return new DefaultTransporterProvider(getTransporterFactories());
    }

    private BasicRepositoryConnectorFactory basicRepositoryConnectorFactory;

    public final BasicRepositoryConnectorFactory getBasicRepositoryConnectorFactory() {
        checkClosed();
        if (basicRepositoryConnectorFactory == null) {
            basicRepositoryConnectorFactory = createBasicRepositoryConnectorFactory();
        }
        return basicRepositoryConnectorFactory;
    }

    protected BasicRepositoryConnectorFactory createBasicRepositoryConnectorFactory() {
        return new BasicRepositoryConnectorFactory(
                getTransporterProvider(),
                getRepositoryLayoutProvider(),
                getChecksumPolicyProvider(),
                getPathProcessor(),
                getChecksumProcessor(),
                getProvidedChecksumsSources());
    }

    private Map<String, RepositoryConnectorFactory> repositoryConnectorFactories;

    public final Map<String, RepositoryConnectorFactory> getRepositoryConnectorFactories() {
        checkClosed();
        if (repositoryConnectorFactories == null) {
            repositoryConnectorFactories = createRepositoryConnectorFactories();
        }
        return repositoryConnectorFactories;
    }

    protected Map<String, RepositoryConnectorFactory> createRepositoryConnectorFactories() {
        HashMap<String, RepositoryConnectorFactory> result = new HashMap<>();
        result.put(BasicRepositoryConnectorFactory.NAME, getBasicRepositoryConnectorFactory());
        return result;
    }

    private Map<String, PipelineRepositoryConnectorFactory> pipelineRepositoryConnectorFactories;

    public final Map<String, PipelineRepositoryConnectorFactory> getPipelineRepositoryConnectorFactories() {
        checkClosed();
        if (pipelineRepositoryConnectorFactories == null) {
            pipelineRepositoryConnectorFactories = createPipelineRepositoryConnectorFactories();
        }
        return pipelineRepositoryConnectorFactories;
    }

    protected Map<String, PipelineRepositoryConnectorFactory> createPipelineRepositoryConnectorFactories() {
        HashMap<String, PipelineRepositoryConnectorFactory> result = new HashMap<>();
        result.put(
                OfflinePipelineRepositoryConnectorFactory.NAME,
                new OfflinePipelineRepositoryConnectorFactory(getOfflineController()));
        result.put(
                FilteringPipelineRepositoryConnectorFactory.NAME,
                new FilteringPipelineRepositoryConnectorFactory(getRemoteRepositoryFilterManager()));
        return result;
    }

    private RepositoryConnectorProvider repositoryConnectorProvider;

    public final RepositoryConnectorProvider getRepositoryConnectorProvider() {
        checkClosed();
        if (repositoryConnectorProvider == null) {
            repositoryConnectorProvider = createRepositoryConnectorProvider();
        }
        return repositoryConnectorProvider;
    }

    protected RepositoryConnectorProvider createRepositoryConnectorProvider() {
        return new DefaultRepositoryConnectorProvider(
                getRepositoryConnectorFactories(), getPipelineRepositoryConnectorFactories());
    }

    private Installer installer;

    public final Installer getInstaller() {
        checkClosed();
        if (installer == null) {
            installer = createInstaller();
        }
        return installer;
    }

    protected Installer createInstaller() {
        return new DefaultInstaller(
                getPathProcessor(),
                getRepositoryEventDispatcher(),
                getArtifactGeneratorFactories(),
                getMetadataGeneratorFactories(),
                getArtifactTransformers(),
                getSyncContextFactory());
    }

    private Deployer deployer;

    public final Deployer getDeployer() {
        checkClosed();
        if (deployer == null) {
            deployer = createDeployer();
        }
        return deployer;
    }

    protected Deployer createDeployer() {
        return new DefaultDeployer(
                getPathProcessor(),
                getRepositoryEventDispatcher(),
                getRepositoryConnectorProvider(),
                getRemoteRepositoryManager(),
                getUpdateCheckManager(),
                getArtifactGeneratorFactories(),
                getMetadataGeneratorFactories(),
                getArtifactTransformers(),
                getSyncContextFactory(),
                getOfflineController());
    }

    private Map<String, DependencyCollectorDelegate> dependencyCollectorDelegates;

    public final Map<String, DependencyCollectorDelegate> getDependencyCollectorDelegates() {
        checkClosed();
        if (dependencyCollectorDelegates == null) {
            dependencyCollectorDelegates = createDependencyCollectorDelegates();
        }
        return dependencyCollectorDelegates;
    }

    protected Map<String, DependencyCollectorDelegate> createDependencyCollectorDelegates() {
        RemoteRepositoryManager remoteRepositoryManager = getRemoteRepositoryManager();
        ArtifactDescriptorReader artifactDescriptorReader = getArtifactDescriptorReader();
        VersionRangeResolver versionRangeResolver = getVersionRangeResolver();
        HashMap<String, DependencyCollectorDelegate> result = new HashMap<>();
        result.put(
                DfDependencyCollector.NAME,
                new DfDependencyCollector(
                        remoteRepositoryManager,
                        artifactDescriptorReader,
                        versionRangeResolver,
                        getArtifactDecoratorFactories()));
        result.put(
                BfDependencyCollector.NAME,
                new BfDependencyCollector(
                        remoteRepositoryManager,
                        artifactDescriptorReader,
                        versionRangeResolver,
                        getArtifactDecoratorFactories()));
        return result;
    }

    private DependencyCollector dependencyCollector;

    public final DependencyCollector getDependencyCollector() {
        checkClosed();
        if (dependencyCollector == null) {
            dependencyCollector = createDependencyCollector();
        }
        return dependencyCollector;
    }

    protected DependencyCollector createDependencyCollector() {
        return new DefaultDependencyCollector(getDependencyCollectorDelegates());
    }

    private Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors;

    public final Map<String, ArtifactResolverPostProcessor> getArtifactResolverPostProcessors() {
        checkClosed();
        if (artifactResolverPostProcessors == null) {
            artifactResolverPostProcessors = createArtifactResolverPostProcessors();
        }
        return artifactResolverPostProcessors;
    }

    protected Map<String, ArtifactResolverPostProcessor> createArtifactResolverPostProcessors() {
        HashMap<String, ArtifactResolverPostProcessor> result = new HashMap<>();
        result.put(
                TrustedChecksumsArtifactResolverPostProcessor.NAME,
                new TrustedChecksumsArtifactResolverPostProcessor(
                        getChecksumAlgorithmFactorySelector(), getTrustedChecksumsSources()));
        return result;
    }

    private ArtifactResolver artifactResolver;

    public final ArtifactResolver getArtifactResolver() {
        checkClosed();
        if (artifactResolver == null) {
            artifactResolver = createArtifactResolver();
        }
        return artifactResolver;
    }

    protected ArtifactResolver createArtifactResolver() {
        return new DefaultArtifactResolver(
                getPathProcessor(),
                getRepositoryEventDispatcher(),
                getVersionResolver(),
                getUpdateCheckManager(),
                getRepositoryConnectorProvider(),
                getRemoteRepositoryManager(),
                getSyncContextFactory(),
                getOfflineController(),
                getArtifactResolverPostProcessors(),
                getRemoteRepositoryFilterManager());
    }

    private MetadataResolver metadataResolver;

    public final MetadataResolver getMetadataResolver() {
        checkClosed();
        if (metadataResolver == null) {
            metadataResolver = createMetadataResolver();
        }
        return metadataResolver;
    }

    protected MetadataResolver createMetadataResolver() {
        return new DefaultMetadataResolver(
                getRepositoryEventDispatcher(),
                getUpdateCheckManager(),
                getRepositoryConnectorProvider(),
                getRemoteRepositoryManager(),
                getSyncContextFactory(),
                getOfflineController(),
                getRemoteRepositoryFilterManager(),
                getPathProcessor());
    }

    private VersionScheme versionScheme;

    public final VersionScheme getVersionScheme() {
        checkClosed();
        if (versionScheme == null) {
            versionScheme = createVersionScheme();
        }
        return versionScheme;
    }

    protected VersionScheme createVersionScheme() {
        return new GenericVersionScheme();
    }

    private Map<String, ArtifactGeneratorFactory> artifactGeneratorFactories;

    public final Map<String, ArtifactGeneratorFactory> getArtifactGeneratorFactories() {
        checkClosed();
        if (artifactGeneratorFactories == null) {
            artifactGeneratorFactories = createArtifactGeneratorFactories();
        }
        return artifactGeneratorFactories;
    }

    protected Map<String, ArtifactGeneratorFactory> createArtifactGeneratorFactories() {
        // by default none, this is extension point
        return new HashMap<>();
    }

    private Map<String, ArtifactDecoratorFactory> artifactDecoratorFactories;

    public final Map<String, ArtifactDecoratorFactory> getArtifactDecoratorFactories() {
        checkClosed();
        if (artifactDecoratorFactories == null) {
            artifactDecoratorFactories = createArtifactDecoratorFactories();
        }
        return artifactDecoratorFactories;
    }

    protected Map<String, ArtifactDecoratorFactory> createArtifactDecoratorFactories() {
        // by default none, this is extension point
        return new HashMap<>();
    }

    protected Map<String, ArtifactTransformer> artifactTransformers;

    public final Map<String, ArtifactTransformer> getArtifactTransformers() {
        checkClosed();
        if (artifactTransformers == null) {
            artifactTransformers = createArtifactTransformers();
        }
        return artifactTransformers;
    }

    protected Map<String, ArtifactTransformer> createArtifactTransformers() {
        return new HashMap<>();
    }

    // Maven provided

    private Map<String, MetadataGeneratorFactory> metadataGeneratorFactories;

    public final Map<String, MetadataGeneratorFactory> getMetadataGeneratorFactories() {
        checkClosed();
        if (metadataGeneratorFactories == null) {
            metadataGeneratorFactories = createMetadataGeneratorFactories();
        }
        return metadataGeneratorFactories;
    }

    protected Map<String, MetadataGeneratorFactory> createMetadataGeneratorFactories() {
        // from maven-resolver-provider
        HashMap<String, MetadataGeneratorFactory> result = new HashMap<>();
        result.put(PluginsMetadataGeneratorFactory.NAME, new PluginsMetadataGeneratorFactory());
        result.put(VersionsMetadataGeneratorFactory.NAME, new VersionsMetadataGeneratorFactory());
        result.put(SnapshotMetadataGeneratorFactory.NAME, new SnapshotMetadataGeneratorFactory());
        return result;
    }

    private LinkedHashMap<String, MavenArtifactRelocationSource> artifactRelocationSources;

    public final LinkedHashMap<String, MavenArtifactRelocationSource> getMavenArtifactRelocationSources() {
        checkClosed();
        if (artifactRelocationSources == null) {
            artifactRelocationSources = createMavenArtifactRelocationSources();
        }
        return artifactRelocationSources;
    }

    protected LinkedHashMap<String, MavenArtifactRelocationSource> createMavenArtifactRelocationSources() {
        // from maven-resolver-provider
        LinkedHashMap<String, MavenArtifactRelocationSource> result = new LinkedHashMap<>();
        result.put(UserPropertiesArtifactRelocationSource.NAME, new UserPropertiesArtifactRelocationSource());
        result.put(
                DistributionManagementArtifactRelocationSource.NAME,
                new DistributionManagementArtifactRelocationSource());
        return result;
    }

    private ArtifactDescriptorReader artifactDescriptorReader;

    public final ArtifactDescriptorReader getArtifactDescriptorReader() {
        checkClosed();
        if (artifactDescriptorReader == null) {
            artifactDescriptorReader = createArtifactDescriptorReader();
        }
        return artifactDescriptorReader;
    }

    protected ArtifactDescriptorReader createArtifactDescriptorReader() {
        // from maven-resolver-provider
        return new DefaultArtifactDescriptorReader(
                getVersionResolver(),
                getArtifactResolver(),
                getModelBuilder(),
                getRepositoryEventDispatcher(),
                getMavenArtifactRelocationSources());
    }

    private VersionResolver versionResolver;

    public final VersionResolver getVersionResolver() {
        checkClosed();
        if (versionResolver == null) {
            versionResolver = createVersionResolver();
        }
        return versionResolver;
    }

    protected VersionResolver createVersionResolver() {
        // from maven-resolver-provider
        return new DefaultVersionResolver(
                getMetadataResolver(), getSyncContextFactory(), getRepositoryEventDispatcher());
    }

    private VersionRangeResolver versionRangeResolver;

    public final VersionRangeResolver getVersionRangeResolver() {
        checkClosed();
        if (versionRangeResolver == null) {
            versionRangeResolver = createVersionRangeResolver();
        }
        return versionRangeResolver;
    }

    protected VersionRangeResolver createVersionRangeResolver() {
        // from maven-resolver-provider
        return new DefaultVersionRangeResolver(
                getMetadataResolver(), getSyncContextFactory(), getRepositoryEventDispatcher(), getVersionScheme());
    }

    private ModelBuilder modelBuilder;

    public final ModelBuilder getModelBuilder() {
        checkClosed();
        if (modelBuilder == null) {
            modelBuilder = createModelBuilder();
        }
        return modelBuilder;
    }

    protected ModelBuilder createModelBuilder() {
        // from maven-model-builder
        DefaultModelProcessor modelProcessor = new DefaultModelProcessor(new DefaultModelXmlFactory(), List.of());
        return new DefaultModelBuilder(
                modelProcessor,
                new DefaultModelValidator(),
                new DefaultModelNormalizer(),
                new DefaultModelInterpolator(
                        new DefaultPathTranslator(),
                        new DefaultUrlNormalizer(),
                        new DefaultRootLocator(),
                        new DefaultInterpolator()),
                new DefaultModelPathTranslator(new DefaultPathTranslator()),
                new DefaultModelUrlNormalizer(new DefaultUrlNormalizer()),
                new DefaultSuperPomProvider(modelProcessor),
                new DefaultInheritanceAssembler(),
                new DefaultProfileSelector(),
                new DefaultProfileInjector(),
                new DefaultPluginManagementInjector(),
                new DefaultDependencyManagementInjector(),
                new DefaultDependencyManagementImporter(),
                new DefaultPluginConfigurationExpander(),
                new DefaultModelVersionParser(getVersionScheme()),
                List.of(),
                new DefaultModelResolver(),
                new DefaultInterpolator(),
                new DefaultPathTranslator(),
                new DefaultRootLocator());
    }

    private RepositorySystemValidator repositorySystemValidator;

    public RepositorySystemValidator getRepositorySystemValidator() {
        checkClosed();
        if (repositorySystemValidator == null) {
            repositorySystemValidator = createRepositorySystemValidator();
        }
        return repositorySystemValidator;
    }

    protected RepositorySystemValidator createRepositorySystemValidator() {
        return new DefaultRepositorySystemValidator(getValidatorFactories());
    }

    private List<ValidatorFactory> validatorFactories;

    public final List<ValidatorFactory> getValidatorFactories() {
        checkClosed();
        if (validatorFactories == null) {
            validatorFactories = createValidatorFactories();
        }
        return validatorFactories;
    }

    protected List<ValidatorFactory> createValidatorFactories() {
        List<ValidatorFactory> result = new ArrayList<>();
        result.add(new MavenValidatorFactory());
        return result;
    }

    private RepositorySystem repositorySystem;

    public final RepositorySystem getRepositorySystem() {
        checkClosed();
        if (repositorySystem == null) {
            repositorySystem = createRepositorySystem();
        }
        return repositorySystem;
    }

    protected RepositorySystem createRepositorySystem() {
        return new DefaultRepositorySystem(
                getVersionResolver(),
                getVersionRangeResolver(),
                getArtifactResolver(),
                getMetadataResolver(),
                getArtifactDescriptorReader(),
                getDependencyCollector(),
                getInstaller(),
                getDeployer(),
                getLocalRepositoryProvider(),
                getSyncContextFactory(),
                getRemoteRepositoryManager(),
                getRepositorySystemLifecycle(),
                getArtifactDecoratorFactories(),
                getRepositorySystemValidator());
    }

    @Override
    public RepositorySystem get() {
        return getRepositorySystem();
    }
}
