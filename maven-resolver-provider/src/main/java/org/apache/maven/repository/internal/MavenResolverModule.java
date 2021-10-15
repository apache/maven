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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Named;
import javax.inject.Singleton;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.apache.maven.model.building.BuildModelSourceTransformer;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSourceTransformer;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.plugin.DefaultPluginConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportingConverter;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;
import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.transform.BuildToRawPomXMLFilterListener;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.impl.guice.AetherModule;
import org.eclipse.aether.version.VersionScheme;

/**
 * MavenResolverModule
 */
public final class MavenResolverModule
    extends AbstractModule
{

    @Override
    protected void configure()
    {
        install( new AetherModule() );
        bind( VersionScheme.class ).toProvider( new DefaultVersionSchemeProvider() );
        bind( ArtifactDescriptorReader.class ).to( DefaultArtifactDescriptorReader.class ).in( Singleton.class );
        bind( VersionResolver.class ).to( DefaultVersionResolver.class ).in( Singleton.class );
        bind( VersionRangeResolver.class ).to( DefaultVersionRangeResolver.class ).in( Singleton.class );
        bind( MetadataGeneratorFactory.class ).annotatedWith( Names.named( "snapshot" ) )
            .to( SnapshotMetadataGeneratorFactory.class ).in( Singleton.class );

        bind( MetadataGeneratorFactory.class ).annotatedWith( Names.named( "versions" ) )
            .to( VersionsMetadataGeneratorFactory.class ).in( Singleton.class );

        // Model builder
        bind( ModelBuilder.class ).to( DefaultModelBuilder.class ).in( Singleton.class );
        bind( ModelValidator.class ).to( DefaultModelValidator.class ).in( Singleton.class );
        bind( SuperPomProvider.class ).to( DefaultSuperPomProvider.class ).in( Singleton.class );
        bind( InheritanceAssembler.class ).to( DefaultInheritanceAssembler.class ).in( Singleton.class );
        bind( ModelProcessor.class ).to( DefaultModelProcessor.class ).in( Singleton.class );
        bind( ModelInterpolator.class ).to( StringVisitorModelInterpolator.class ).in( Singleton.class );
        bind( ModelLocator.class ).to( DefaultModelLocator.class ).in( Singleton.class );
        bind( ModelReader.class ).to( DefaultModelReader.class ).in( Singleton.class );
        bind( ModelWriter.class ).to( DefaultModelWriter.class ).in( Singleton.class );
        bind( DependencyManagementImporter.class )
                .to( DefaultDependencyManagementImporter.class ).in( Singleton.class );
        bind( DependencyManagementInjector.class )
                .to( DefaultDependencyManagementInjector.class ).in( Singleton.class );
        bind( PluginManagementInjector.class ).to( DefaultPluginManagementInjector.class ).in( Singleton.class );
        bind( ModelNormalizer.class ).to( DefaultModelNormalizer.class ).in( Singleton.class );
        bind( PathTranslator.class ).to( DefaultPathTranslator.class ).in( Singleton.class );
        bind( UrlNormalizer.class ).to( DefaultUrlNormalizer.class ).in( Singleton.class );
        bind( ModelPathTranslator.class ).to( DefaultModelPathTranslator.class ).in( Singleton.class );
        bind( ModelUrlNormalizer.class ).to( DefaultModelUrlNormalizer.class ).in( Singleton.class );
        bind( PluginConfigurationExpander.class ).to( DefaultPluginConfigurationExpander.class ).in( Singleton.class );
        bind( ReportConfigurationExpander.class ).to( DefaultReportConfigurationExpander.class ).in( Singleton.class );
        bind( ReportingConverter.class ).to( DefaultReportingConverter.class ).in( Singleton.class );
        bind( ProfileSelector.class ).to( DefaultProfileSelector.class ).in( Singleton.class );
        bind( ProfileInjector.class ).to( DefaultProfileInjector.class ).in( Singleton.class );
        bind( Key.get( ProfileActivator.class, Names.named( "os" ) ) )
                .to( OperatingSystemProfileActivator.class ).in( Singleton.class );
        bind( Key.get( ProfileActivator.class, Names.named( "property" ) ) )
                .to( PropertyProfileActivator.class ).in( Singleton.class );
        bind( Key.get( ProfileActivator.class, Names.named( "jdk-version" ) ) )
                .to( JdkVersionProfileActivator.class ).in( Singleton.class );
        bind( Key.get( ProfileActivator.class, Names.named( "file" ) ) )
                .to( FileProfileActivator.class ).in( Singleton.class );

        bind( LifecycleBindingsInjector.class )
                .toInstance( new DefaultModelBuilderFactory.StubLifecycleBindingsInjector() );

        // ???
        bind( ModelSourceTransformer.class ).to( BuildModelSourceTransformer.class ).in( Singleton.class );
        bind( BuildToRawPomXMLFilterListener.class ).toProvider( Providers.of( null ) );
    }

    @Provides
    @Singleton
    Set<MetadataGeneratorFactory> provideMetadataGeneratorFactories(
        @Named( "snapshot" ) MetadataGeneratorFactory snapshot,
        @Named( "versions" ) MetadataGeneratorFactory versions )
    {
        Set<MetadataGeneratorFactory> factories = new HashSet<>( 2 );
        factories.add( snapshot );
        factories.add( versions );
        return Collections.unmodifiableSet( factories );
    }

    @Provides
    @Singleton
    List<ProfileActivator> provideProfileActivatorList(
            @Named( "os" ) ProfileActivator os,
            @Named( "property" ) ProfileActivator property,
            @Named( "jdk-version" ) ProfileActivator jdkVersion,
            @Named( "file" ) ProfileActivator file )
    {
        ArrayList<ProfileActivator> factories = new ArrayList<>( 4 );
        factories.add( os );
        factories.add( property );
        factories.add( jdkVersion );
        factories.add( file );
        return Collections.unmodifiableList( factories );
    }
}
