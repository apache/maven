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
package org.apache.maven.impl.standalone;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
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

/**
 * DI Bridge for Maven Resolver
 *
 * TODO: reuse mvn4 Supplier here
 */
@SuppressWarnings({"unused", "checkstyle:ParameterNumber"})
public class RepositorySystemSupplier {

    @Provides
    static MetadataResolver newMetadataResolver(
            RepositoryEventDispatcher repositoryEventDispatcher,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager,
            PathProcessor pathProcessor) {
        return new DefaultMetadataResolver(
                repositoryEventDispatcher,
                updateCheckManager,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                syncContextFactory,
                offlineController,
                remoteRepositoryFilterManager,
                pathProcessor);
    }

    @Provides
    static RepositoryEventDispatcher newRepositoryEventDispatcher(@Nullable Map<String, RepositoryListener> listeners) {
        return new DefaultRepositoryEventDispatcher(listeners != null ? listeners : Map.of());
    }

    @Provides
    static UpdateCheckManager newUpdateCheckManager(
            TrackingFileManager trackingFileManager,
            UpdatePolicyAnalyzer updatePolicyAnalyzer,
            PathProcessor pathProcessor) {
        return new DefaultUpdateCheckManager(trackingFileManager, updatePolicyAnalyzer, pathProcessor);
    }

    @Provides
    static TrackingFileManager newTrackingFileManager() {
        return new DefaultTrackingFileManager();
    }

    @Provides
    static UpdatePolicyAnalyzer newUpdatePolicyAnalyzer() {
        return new DefaultUpdatePolicyAnalyzer();
    }

    @Provides
    static RepositoryConnectorProvider newRepositoryConnectorProvider(
            Map<String, RepositoryConnectorFactory> connectorFactories,
            Map<String, PipelineRepositoryConnectorFactory> pipelineConnectorFactories) {
        return new DefaultRepositoryConnectorProvider(connectorFactories, pipelineConnectorFactories);
    }

    @Named("basic")
    @Provides
    static BasicRepositoryConnectorFactory newBasicRepositoryConnectorFactory(
            TransporterProvider transporterProvider,
            RepositoryLayoutProvider layoutProvider,
            ChecksumPolicyProvider checksumPolicyProvider,
            PathProcessor pathProcessor,
            ChecksumProcessor checksumProcessor,
            Map<String, ProvidedChecksumsSource> providedChecksumsSources) {
        return new BasicRepositoryConnectorFactory(
                transporterProvider,
                layoutProvider,
                checksumPolicyProvider,
                pathProcessor,
                checksumProcessor,
                providedChecksumsSources);
    }

    @Named(OfflinePipelineRepositoryConnectorFactory.NAME)
    @Provides
    static OfflinePipelineRepositoryConnectorFactory newOfflinePipelineConnectorFactory(
            OfflineController offlineController) {
        return new OfflinePipelineRepositoryConnectorFactory(offlineController);
    }

    @Named(FilteringPipelineRepositoryConnectorFactory.NAME)
    @Provides
    static FilteringPipelineRepositoryConnectorFactory newFilteringPipelineConnectorFactory(
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        return new FilteringPipelineRepositoryConnectorFactory(remoteRepositoryFilterManager);
    }

    @Provides
    static RepositoryLayoutProvider newRepositoryLayoutProvider(Map<String, RepositoryLayoutFactory> layoutFactories) {
        return new DefaultRepositoryLayoutProvider(layoutFactories);
    }

    @Provides
    @Named(Maven2RepositoryLayoutFactory.NAME)
    static Maven2RepositoryLayoutFactory newMaven2RepositoryLayoutFactory(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector,
            ArtifactPredicateFactory artifactPredicateFactory) {
        return new Maven2RepositoryLayoutFactory(checksumAlgorithmFactorySelector, artifactPredicateFactory);
    }

    @Provides
    static SyncContextFactory newSyncContextFactory(NamedLockFactoryAdapterFactory namedLockFactoryAdapterFactory) {
        return new DefaultSyncContextFactory(namedLockFactoryAdapterFactory);
    }

    @Provides
    static OfflineController newOfflineController() {
        return new DefaultOfflineController();
    }

    @Provides
    static RemoteRepositoryFilterManager newRemoteRepositoryFilterManager(
            Map<String, RemoteRepositoryFilterSource> sources) {
        return new DefaultRemoteRepositoryFilterManager(sources);
    }

    @Provides
    @Named(GroupIdRemoteRepositoryFilterSource.NAME)
    static GroupIdRemoteRepositoryFilterSource newGroupIdRemoteRepositoryFilterSource(
            RepositorySystemLifecycle repositorySystemLifecycle, PathProcessor pathProcessor) {
        return new GroupIdRemoteRepositoryFilterSource(repositorySystemLifecycle, pathProcessor);
    }

    @Provides
    @Named(PrefixesRemoteRepositoryFilterSource.NAME)
    static PrefixesRemoteRepositoryFilterSource newPrefixesRemoteRepositoryFilterSource(
            MetadataResolver metadataResolver,
            RemoteRepositoryManager remoteRepositoryManager,
            RepositoryLayoutProvider repositoryLayoutProvider) {
        return new PrefixesRemoteRepositoryFilterSource(
                () -> metadataResolver, () -> remoteRepositoryManager, repositoryLayoutProvider);
    }

    @Provides
    static PathProcessor newPathProcessor() {
        return new DefaultPathProcessor();
    }

    @Provides
    static List<ValidatorFactory> newValidatorFactories() {
        return List.of(new MavenValidatorFactory());
    }

    @Provides
    static RepositorySystemValidator newRepositorySystemValidator(List<ValidatorFactory> validatorFactories) {
        return new DefaultRepositorySystemValidator(validatorFactories);
    }

    @Provides
    static RepositorySystem newRepositorySystem(
            VersionResolver versionResolver,
            VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver,
            MetadataResolver metadataResolver,
            ArtifactDescriptorReader artifactDescriptorReader,
            DependencyCollector dependencyCollector,
            Installer installer,
            Deployer deployer,
            LocalRepositoryProvider localRepositoryProvider,
            SyncContextFactory syncContextFactory,
            RemoteRepositoryManager remoteRepositoryManager,
            RepositorySystemLifecycle repositorySystemLifecycle,
            @Nullable Map<String, ArtifactDecoratorFactory> artifactDecoratorFactories,
            RepositorySystemValidator repositorySystemValidator) {
        return new DefaultRepositorySystem(
                versionResolver,
                versionRangeResolver,
                artifactResolver,
                metadataResolver,
                artifactDescriptorReader,
                dependencyCollector,
                installer,
                deployer,
                localRepositoryProvider,
                syncContextFactory,
                remoteRepositoryManager,
                repositorySystemLifecycle,
                artifactDecoratorFactories != null ? artifactDecoratorFactories : Map.of(),
                repositorySystemValidator);
    }

    @Provides
    static RemoteRepositoryManager newRemoteRepositoryManager(
            UpdatePolicyAnalyzer updatePolicyAnalyzer, ChecksumPolicyProvider checksumPolicyProvider) {
        return new DefaultRemoteRepositoryManager(updatePolicyAnalyzer, checksumPolicyProvider);
    }

    @Provides
    static ChecksumPolicyProvider newChecksumPolicyProvider() {
        return new DefaultChecksumPolicyProvider();
    }

    @Provides
    static NamedLockFactoryAdapterFactory newNamedLockFactoryAdapterFactory(
            Map<String, NamedLockFactory> factories,
            Map<String, NameMapper> nameMappers,
            RepositorySystemLifecycle lifecycle) {
        return new NamedLockFactoryAdapterFactoryImpl(factories, nameMappers, lifecycle);
    }

    @Provides
    @Named(FileLockNamedLockFactory.NAME)
    static FileLockNamedLockFactory newFileLockNamedLockFactory() {
        return new FileLockNamedLockFactory();
    }

    @Provides
    @Named(LocalReadWriteLockNamedLockFactory.NAME)
    static LocalReadWriteLockNamedLockFactory newLocalReadWriteLockNamedLockFactory() {
        return new LocalReadWriteLockNamedLockFactory();
    }

    @Provides
    @Named(LocalSemaphoreNamedLockFactory.NAME)
    static LocalSemaphoreNamedLockFactory newLocalSemaphoreNamedLockFactory() {
        return new LocalSemaphoreNamedLockFactory();
    }

    @Provides
    @Named(NoopNamedLockFactory.NAME)
    static NoopNamedLockFactory newNoopNamedLockFactory() {
        return new NoopNamedLockFactory();
    }

    @Provides
    @Named(NameMappers.STATIC_NAME)
    static NameMapper staticNameMapper() {
        return NameMappers.staticNameMapper();
    }

    @Provides
    @Named(NameMappers.GAV_NAME)
    static NameMapper gavNameMapper() {
        return NameMappers.gavNameMapper();
    }

    @Provides
    @Named(NameMappers.GAECV_NAME)
    static NameMapper gaecvNameMapper() {
        return NameMappers.gaecvNameMapper();
    }

    @Provides
    @Named(NameMappers.DISCRIMINATING_NAME)
    static NameMapper discriminatingNameMapper() {
        return NameMappers.discriminatingNameMapper();
    }

    @Provides
    @Named(NameMappers.FILE_GAV_NAME)
    static NameMapper fileGavNameMapper() {
        return NameMappers.fileGavNameMapper();
    }

    @Provides
    @Named(NameMappers.FILE_GAECV_NAME)
    static NameMapper fileGaecvNameMapper() {
        return NameMappers.fileGaecvNameMapper();
    }

    @Provides
    @Named(NameMappers.FILE_HGAV_NAME)
    static NameMapper fileHashingGavNameMapper() {
        return NameMappers.fileHashingGavNameMapper();
    }

    @Provides
    @Named(NameMappers.FILE_HGAECV_NAME)
    static NameMapper fileHashingGaecvNameMapper() {
        return NameMappers.fileHashingGaecvNameMapper();
    }

    @Provides
    static RepositorySystemLifecycle newRepositorySystemLifecycle() {
        return new DefaultRepositorySystemLifecycle();
    }

    @Provides
    static ArtifactResolver newArtifactResolver(
            PathProcessor pathProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            VersionResolver versionResolver,
            UpdateCheckManager updateCheckManager,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController,
            Map<String, ArtifactResolverPostProcessor> artifactResolverPostProcessors,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        return new DefaultArtifactResolver(
                pathProcessor,
                repositoryEventDispatcher,
                versionResolver,
                updateCheckManager,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                syncContextFactory,
                offlineController,
                artifactResolverPostProcessors,
                remoteRepositoryFilterManager);
    }

    @Provides
    static DependencyCollector newDependencyCollector(Map<String, DependencyCollectorDelegate> delegates) {
        return new DefaultDependencyCollector(delegates);
    }

    @Provides
    @Named(BfDependencyCollector.NAME)
    static BfDependencyCollector newBfDependencyCollector(
            RemoteRepositoryManager remoteRepositoryManager,
            ArtifactDescriptorReader artifactDescriptorReader,
            VersionRangeResolver versionRangeResolver,
            @Nullable Map<String, ArtifactDecoratorFactory> artifactDecoratorFactories) {
        return new BfDependencyCollector(
                remoteRepositoryManager,
                artifactDescriptorReader,
                versionRangeResolver,
                artifactDecoratorFactories != null ? artifactDecoratorFactories : Map.of());
    }

    @Provides
    @Named(DfDependencyCollector.NAME)
    static DfDependencyCollector newDfDependencyCollector(
            RemoteRepositoryManager remoteRepositoryManager,
            ArtifactDescriptorReader artifactDescriptorReader,
            VersionRangeResolver versionRangeResolver,
            @Nullable Map<String, ArtifactDecoratorFactory> artifactDecoratorFactories) {
        return new DfDependencyCollector(
                remoteRepositoryManager,
                artifactDescriptorReader,
                versionRangeResolver,
                artifactDecoratorFactories != null ? artifactDecoratorFactories : Map.of());
    }

    @Provides
    static Installer newInstaller(
            PathProcessor pathProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            @Nullable Map<String, ArtifactGeneratorFactory> artifactFactories,
            Map<String, MetadataGeneratorFactory> metadataFactories,
            SyncContextFactory syncContextFactory) {
        return new DefaultInstaller(
                pathProcessor,
                repositoryEventDispatcher,
                artifactFactories != null ? artifactFactories : Map.of(),
                metadataFactories,
                Map.of(),
                syncContextFactory);
    }

    @Provides
    static Deployer newDeployer(
            PathProcessor pathProcessor,
            RepositoryEventDispatcher repositoryEventDispatcher,
            RepositoryConnectorProvider repositoryConnectorProvider,
            RemoteRepositoryManager remoteRepositoryManager,
            UpdateCheckManager updateCheckManager,
            @Nullable Map<String, ArtifactGeneratorFactory> artifactFactories,
            Map<String, MetadataGeneratorFactory> metadataFactories,
            SyncContextFactory syncContextFactory,
            OfflineController offlineController) {
        return new DefaultDeployer(
                pathProcessor,
                repositoryEventDispatcher,
                repositoryConnectorProvider,
                remoteRepositoryManager,
                updateCheckManager,
                artifactFactories != null ? artifactFactories : Map.of(),
                metadataFactories,
                Map.of(),
                syncContextFactory,
                offlineController);
    }

    @Provides
    static LocalRepositoryProvider newLocalRepositoryProvider(
            Map<String, LocalRepositoryManagerFactory> localRepositoryManagerFactories) {
        return new DefaultLocalRepositoryProvider(localRepositoryManagerFactories);
    }

    @Provides
    @Named(EnhancedLocalRepositoryManagerFactory.NAME)
    static EnhancedLocalRepositoryManagerFactory newEnhancedLocalRepositoryManagerFactory(
            LocalPathComposer localPathComposer,
            TrackingFileManager trackingFileManager,
            LocalPathPrefixComposerFactory localPathPrefixComposerFactory) {
        return new EnhancedLocalRepositoryManagerFactory(
                localPathComposer, trackingFileManager, localPathPrefixComposerFactory);
    }

    @Provides
    @Named(SimpleLocalRepositoryManagerFactory.NAME)
    static SimpleLocalRepositoryManagerFactory newSimpleLocalRepositoryManagerFactory(
            LocalPathComposer localPathComposer) {
        return new SimpleLocalRepositoryManagerFactory(localPathComposer);
    }

    @Provides
    static LocalPathComposer newLocalPathComposer() {
        return new DefaultLocalPathComposer();
    }

    @Provides
    static LocalPathPrefixComposerFactory newLocalPathPrefixComposerFactory() {
        return new DefaultLocalPathPrefixComposerFactory();
    }

    @Provides
    static TransporterProvider newTransportProvider(@Nullable Map<String, TransporterFactory> transporterFactories) {
        return new DefaultTransporterProvider(transporterFactories != null ? transporterFactories : Map.of());
    }

    @Provides
    static ChecksumProcessor newChecksumProcessor(PathProcessor pathProcessor) {
        return new DefaultChecksumProcessor(pathProcessor);
    }

    @Provides
    static ChecksumExtractor newChecksumExtractor(Map<String, ChecksumExtractorStrategy> strategies) {
        return new DefaultChecksumExtractor(strategies);
    }

    @Provides
    @Named(Nx2ChecksumExtractor.NAME)
    static Nx2ChecksumExtractor newNx2ChecksumExtractor() {
        return new Nx2ChecksumExtractor();
    }

    @Provides
    @Named(XChecksumExtractor.NAME)
    static XChecksumExtractor newXChecksumExtractor() {
        return new XChecksumExtractor();
    }

    @Provides
    @Named(TrustedToProvidedChecksumsSourceAdapter.NAME)
    static TrustedToProvidedChecksumsSourceAdapter newTrustedToProvidedChecksumsSourceAdapter(
            Map<String, TrustedChecksumsSource> trustedChecksumsSources) {
        return new TrustedToProvidedChecksumsSourceAdapter(trustedChecksumsSources);
    }

    @Provides
    @Named(SparseDirectoryTrustedChecksumsSource.NAME)
    static SparseDirectoryTrustedChecksumsSource newSparseDirectoryTrustedChecksumsSource(
            ChecksumProcessor checksumProcessor, LocalPathComposer localPathComposer) {
        return new SparseDirectoryTrustedChecksumsSource(checksumProcessor, localPathComposer);
    }

    @Provides
    @Named(SummaryFileTrustedChecksumsSource.NAME)
    static SummaryFileTrustedChecksumsSource newSummaryFileTrustedChecksumsSource(
            LocalPathComposer localPathComposer,
            RepositorySystemLifecycle repositorySystemLifecycle,
            PathProcessor pathProcessor) {
        return new SummaryFileTrustedChecksumsSource(localPathComposer, repositorySystemLifecycle, pathProcessor);
    }

    @Provides
    static ChecksumAlgorithmFactorySelector newChecksumAlgorithmFactorySelector(
            Map<String, ChecksumAlgorithmFactory> factories) {
        return new DefaultChecksumAlgorithmFactorySelector(factories);
    }

    @Provides
    @Named(Md5ChecksumAlgorithmFactory.NAME)
    static Md5ChecksumAlgorithmFactory newMd5ChecksumAlgorithmFactory() {
        return new Md5ChecksumAlgorithmFactory();
    }

    @Provides
    @Named(Sha1ChecksumAlgorithmFactory.NAME)
    static Sha1ChecksumAlgorithmFactory newSh1ChecksumAlgorithmFactory() {
        return new Sha1ChecksumAlgorithmFactory();
    }

    @Provides
    @Named(Sha256ChecksumAlgorithmFactory.NAME)
    static Sha256ChecksumAlgorithmFactory newSh256ChecksumAlgorithmFactory() {
        return new Sha256ChecksumAlgorithmFactory();
    }

    @Provides
    @Named(Sha512ChecksumAlgorithmFactory.NAME)
    static Sha512ChecksumAlgorithmFactory newSh512ChecksumAlgorithmFactory() {
        return new Sha512ChecksumAlgorithmFactory();
    }

    @Provides
    static ArtifactPredicateFactory newArtifactPredicateFactory(
            ChecksumAlgorithmFactorySelector checksumAlgorithmFactorySelector) {
        return new DefaultArtifactPredicateFactory(checksumAlgorithmFactorySelector);
    }
}
