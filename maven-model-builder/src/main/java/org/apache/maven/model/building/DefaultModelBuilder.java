package org.apache.maven.model.building;

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

import static org.apache.maven.model.building.Result.error;
import static org.apache.maven.model.building.Result.newResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.building.Source;
import org.apache.maven.feature.Features;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.merge.ModelMerger;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.eclipse.sisu.Nullable;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelBuilder
    implements ModelBuilder
{
    @Inject
    private ModelProcessor modelProcessor;

    @Inject
    private ModelValidator modelValidator;

    @Inject
    private ModelNormalizer modelNormalizer;

    @Inject
    private ModelInterpolator modelInterpolator;

    @Inject
    private ModelPathTranslator modelPathTranslator;

    @Inject
    private ModelUrlNormalizer modelUrlNormalizer;

    @Inject
    private SuperPomProvider superPomProvider;

    @Inject
    private InheritanceAssembler inheritanceAssembler;

    @Inject
    private ProfileSelector profileSelector;

    @Inject
    private ProfileInjector profileInjector;

    @Inject
    private PluginManagementInjector pluginManagementInjector;

    @Inject
    private DependencyManagementInjector dependencyManagementInjector;

    @Inject
    private DependencyManagementImporter dependencyManagementImporter;

    @Inject
    @Nullable
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Inject
    private PluginConfigurationExpander pluginConfigurationExpander;

    @Inject
    private ReportConfigurationExpander reportConfigurationExpander;

    @Inject
    private ReportingConverter reportingConverter;

    private ModelMerger modelMerger = new FileToRawModelMerger();

    @Inject
    private ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;

    public DefaultModelBuilder setModelProcessor( ModelProcessor modelProcessor )
    {
        this.modelProcessor = modelProcessor;
        return this;
    }

    public DefaultModelBuilder setModelValidator( ModelValidator modelValidator )
    {
        this.modelValidator = modelValidator;
        return this;
    }

    public DefaultModelBuilder setModelNormalizer( ModelNormalizer modelNormalizer )
    {
        this.modelNormalizer = modelNormalizer;
        return this;
    }

    public DefaultModelBuilder setModelInterpolator( ModelInterpolator modelInterpolator )
    {
        this.modelInterpolator = modelInterpolator;
        return this;
    }

    public DefaultModelBuilder setModelPathTranslator( ModelPathTranslator modelPathTranslator )
    {
        this.modelPathTranslator = modelPathTranslator;
        return this;
    }

    public DefaultModelBuilder setModelUrlNormalizer( ModelUrlNormalizer modelUrlNormalizer )
    {
        this.modelUrlNormalizer = modelUrlNormalizer;
        return this;
    }

    public DefaultModelBuilder setSuperPomProvider( SuperPomProvider superPomProvider )
    {
        this.superPomProvider = superPomProvider;
        return this;
    }

    public DefaultModelBuilder setProfileSelector( ProfileSelector profileSelector )
    {
        this.profileSelector = profileSelector;
        return this;
    }

    public DefaultModelBuilder setProfileInjector( ProfileInjector profileInjector )
    {
        this.profileInjector = profileInjector;
        return this;
    }

    public DefaultModelBuilder setInheritanceAssembler( InheritanceAssembler inheritanceAssembler )
    {
        this.inheritanceAssembler = inheritanceAssembler;
        return this;
    }

    public DefaultModelBuilder setDependencyManagementImporter( DependencyManagementImporter depMgmtImporter )
    {
        this.dependencyManagementImporter = depMgmtImporter;
        return this;
    }

    public DefaultModelBuilder setDependencyManagementInjector( DependencyManagementInjector depMgmtInjector )
    {
        this.dependencyManagementInjector = depMgmtInjector;
        return this;
    }

    public DefaultModelBuilder setLifecycleBindingsInjector( LifecycleBindingsInjector lifecycleBindingsInjector )
    {
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
        return this;
    }

    public DefaultModelBuilder setPluginConfigurationExpander( PluginConfigurationExpander pluginConfigurationExpander )
    {
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        return this;
    }

    public DefaultModelBuilder setPluginManagementInjector( PluginManagementInjector pluginManagementInjector )
    {
        this.pluginManagementInjector = pluginManagementInjector;
        return this;
    }

    public DefaultModelBuilder setReportConfigurationExpander( ReportConfigurationExpander reportConfigurationExpander )
    {
        this.reportConfigurationExpander = reportConfigurationExpander;
        return this;
    }

    public DefaultModelBuilder setReportingConverter( ReportingConverter reportingConverter )
    {
        this.reportingConverter = reportingConverter;
        return this;
    }

    public DefaultModelBuilder setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator )
    {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        return this;
    }

    @Override
    public DefaultTransformerContextBuilder newTransformerContextBuilder()
    {
        return new DefaultTransformerContextBuilder();
    }

    @Override
    public ModelBuildingResult build( ModelBuildingRequest request )
        throws ModelBuildingException
    {
        return build( request, new LinkedHashSet<>() );
    }

    protected ModelBuildingResult build( ModelBuildingRequest request, Collection<String> importIds )
        throws ModelBuildingException
    {
        // phase 1
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector( result );

        // read and validate raw model
        Model fileModel = readFileModel( request, problems );

        request.setFileModel( fileModel );
        result.setFileModel( fileModel );

        activateFileModel( request, result, problems );

        if ( !request.isTwoPhaseBuilding() )
        {
            return build( request, result, importIds );
        }
        else if ( hasModelErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        return result;
    }

    private void activateFileModel( final ModelBuildingRequest request, final DefaultModelBuildingResult result,
                          DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        Model inputModel = request.getFileModel();
        problems.setRootModel( inputModel );

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext( request );

        problems.setSource( "(external profiles)" );
        List<Profile> activeExternalProfiles = profileSelector.getActiveProfiles( request.getProfiles(),
                                                                                  profileActivationContext, problems );

        result.setActiveExternalProfiles( activeExternalProfiles );

        if ( !activeExternalProfiles.isEmpty() )
        {
            Properties profileProps = new Properties();
            for ( Profile profile : activeExternalProfiles )
            {
                profileProps.putAll( profile.getProperties() );
            }
            profileProps.putAll( profileActivationContext.getUserProperties() );
            profileActivationContext.setUserProperties( profileProps );
        }

        profileActivationContext.setProjectProperties( inputModel.getProperties() );
        problems.setSource( inputModel );
        List<Profile> activePomProfiles = profileSelector.getActiveProfiles( inputModel.getProfiles(),
                                                                             profileActivationContext, problems );

        // model normalization
        problems.setSource( inputModel );
        modelNormalizer.mergeDuplicates( inputModel, request, problems );

        Map<String, Activation> interpolatedActivations = getProfileActivations( inputModel, false );
        injectProfileActivations( inputModel, interpolatedActivations );

        // profile injection
        for ( Profile activeProfile : activePomProfiles )
        {
            profileInjector.injectProfile( inputModel, activeProfile, request, problems );
        }

        for ( Profile activeProfile : activeExternalProfiles )
        {
            profileInjector.injectProfile( inputModel, activeProfile, request, problems );
        }
    }

    @SuppressWarnings( "checkstyle:methodlength" )
    private Model readEffectiveModel( final ModelBuildingRequest request, final DefaultModelBuildingResult result,
                          DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        Model inputModel =
            readRawModel( request, problems );

        problems.setRootModel( inputModel );

        ModelData resultData = new ModelData( request.getModelSource(), inputModel );
        ModelData superData = new ModelData( null, getSuperModel() );

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext( request );

        List<Profile> activeExternalProfiles = result.getActiveExternalProfiles();

        if ( !activeExternalProfiles.isEmpty() )
        {
            Properties profileProps = new Properties();
            for ( Profile profile : activeExternalProfiles )
            {
                profileProps.putAll( profile.getProperties() );
            }
            profileProps.putAll( profileActivationContext.getUserProperties() );
            profileActivationContext.setUserProperties( profileProps );
        }

        Collection<String> parentIds = new LinkedHashSet<>();

        List<Model> lineage = new ArrayList<>();

        for ( ModelData currentData = resultData; ; )
        {
            String modelId = currentData.getId();
            result.addModelId( modelId );

            Model rawModel = currentData.getModel();
            result.setRawModel( modelId, rawModel );

            profileActivationContext.setProjectProperties( rawModel.getProperties() );
            problems.setSource( rawModel );
            List<Profile> activePomProfiles = profileSelector.getActiveProfiles( rawModel.getProfiles(),
                                                                                 profileActivationContext, problems );
            result.setActivePomProfiles( modelId, activePomProfiles );

            Model tmpModel = rawModel.clone();

            problems.setSource( tmpModel );

            // model normalization
            modelNormalizer.mergeDuplicates( tmpModel, request, problems );

            profileActivationContext.setProjectProperties( tmpModel.getProperties() );

            Map<String, Activation> interpolatedActivations = getInterpolatedActivations( rawModel,
                                                                                          profileActivationContext,
                                                                                          problems );
            injectProfileActivations( tmpModel, interpolatedActivations );

            // profile injection
            for ( Profile activeProfile : result.getActivePomProfiles( modelId ) )
            {
                profileInjector.injectProfile( tmpModel, activeProfile, request, problems );
            }

            if ( currentData == resultData )
            {
                for ( Profile activeProfile : activeExternalProfiles )
                {
                    profileInjector.injectProfile( tmpModel, activeProfile, request, problems );
                }
                result.setEffectiveModel( tmpModel );
            }

            lineage.add( tmpModel );

            if ( currentData == superData )
            {
                break;
            }

            configureResolver( request.getModelResolver(), tmpModel, problems );

            ModelData parentData =
                readParent( currentData.getModel(), currentData.getSource(), request, result, problems );

            if ( parentData == null )
            {
                currentData = superData;
            }
            else if ( !parentIds.add( parentData.getId() ) )
            {
                StringBuilder message = new StringBuilder( "The parents form a cycle: " );
                for ( String parentId : parentIds )
                {
                    message.append( parentId ).append( " -> " );
                }
                message.append( parentData.getId() );

                problems.add( new ModelProblemCollectorRequest( ModelProblem.Severity.FATAL, ModelProblem.Version.BASE )
                    .setMessage( message.toString() ) );

                throw problems.newModelBuildingException();
            }
            else
            {
                currentData = parentData;
            }
        }

        problems.setSource( result.getRawModel() );
        checkPluginVersions( lineage, request, problems );

        // inheritance assembly
        assembleInheritance( lineage, request, problems );

        Model resultModel = lineage.get( 0 );

        // consider caching inherited model

        problems.setSource( resultModel );
        problems.setRootModel( resultModel );

        // model interpolation
        resultModel = interpolateModel( resultModel, request, problems );

        // url normalization
        modelUrlNormalizer.normalize( resultModel, request );

        result.setEffectiveModel( resultModel );

        // Now the fully interpolated model is available: reconfigure the resolver
        configureResolver( request.getModelResolver(), resultModel, problems, true );

        return resultModel;
    }

    private Map<String, Activation> getInterpolatedActivations( Model rawModel,
                                                                DefaultProfileActivationContext context,
                                                                DefaultModelProblemCollector problems )
    {
        Map<String, Activation> interpolatedActivations = getProfileActivations( rawModel, true );
        for ( Activation activation : interpolatedActivations.values() )
        {
            if ( activation.getFile() != null )
            {
                replaceWithInterpolatedValue( activation.getFile(), context, problems );
            }
        }
        return interpolatedActivations;
    }

    private void replaceWithInterpolatedValue( ActivationFile activationFile, ProfileActivationContext context,
                                               DefaultModelProblemCollector problems  )
    {
        try
        {
            if ( isNotEmpty( activationFile.getExists() ) )
            {
                String path = activationFile.getExists();
                String absolutePath = profileActivationFilePathInterpolator.interpolate( path, context );
                activationFile.setExists( absolutePath );
            }
            else if ( isNotEmpty( activationFile.getMissing() ) )
            {
                String path = activationFile.getMissing();
                String absolutePath = profileActivationFilePathInterpolator.interpolate( path, context );
                activationFile.setMissing( absolutePath );
            }
        }
        catch ( InterpolationException e )
        {
            String path = isNotEmpty(
                    activationFile.getExists() ) ? activationFile.getExists() : activationFile.getMissing();

            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage(
                    "Failed to interpolate file location " + path + ": " + e.getMessage() ).setLocation(
                    activationFile.getLocation( isNotEmpty( activationFile.getExists() ) ? "exists" : "missing"  ) )
                    .setException( e ) );
        }
    }

    private static boolean isNotEmpty( String string )
    {
        return string != null && !string.isEmpty();
    }

    @Override
    public ModelBuildingResult build( final ModelBuildingRequest request, final ModelBuildingResult result )
        throws ModelBuildingException
    {
        return build( request, result, new LinkedHashSet<>() );
    }

    private ModelBuildingResult build( final ModelBuildingRequest request, final ModelBuildingResult phaseOneResult,
                                       Collection<String> imports )
        throws ModelBuildingException
    {
        DefaultModelBuildingResult result = asDefaultModelBuildingResult( phaseOneResult );

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector( result );

        // phase 2
        Model resultModel = readEffectiveModel( request, result, problems );
        problems.setSource( resultModel );
        problems.setRootModel( resultModel );

        // model path translation
        modelPathTranslator.alignToBaseDirectory( resultModel, resultModel.getProjectDirectory(), request );

        // plugin management injection
        pluginManagementInjector.injectManagement( resultModel, request, problems );

        fireEvent( resultModel, request, problems, ModelBuildingEventCatapult.BUILD_EXTENSIONS_ASSEMBLED );

        if ( request.isProcessPlugins() )
        {
            if ( lifecycleBindingsInjector == null )
            {
                throw new IllegalStateException( "lifecycle bindings injector is missing" );
            }

            // lifecycle bindings injection
            lifecycleBindingsInjector.injectLifecycleBindings( resultModel, request, problems );
        }

        // dependency management import
        importDependencyManagement( resultModel, request, problems, imports );

        // dependency management injection
        dependencyManagementInjector.injectManagement( resultModel, request, problems );

        modelNormalizer.injectDefaultValues( resultModel, request, problems );

        if ( request.isProcessPlugins() )
        {
            // reports configuration
            reportConfigurationExpander.expandPluginConfiguration( resultModel, request, problems );

            // reports conversion to decoupled site plugin
            reportingConverter.convertReporting( resultModel, request, problems );

            // plugins configuration
            pluginConfigurationExpander.expandPluginConfiguration( resultModel, request, problems );
        }

        // effective model validation
        modelValidator.validateEffectiveModel( resultModel, request, problems );

        if ( hasModelErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        return result;
    }

    private DefaultModelBuildingResult asDefaultModelBuildingResult( ModelBuildingResult phaseOneResult )
    {
        if ( phaseOneResult instanceof DefaultModelBuildingResult )
        {
            return (DefaultModelBuildingResult) phaseOneResult;
        }
        else
        {
            return new DefaultModelBuildingResult( phaseOneResult );
        }
    }

    @Override
    public Result<? extends Model> buildRawModel( File pomFile, int validationLevel, boolean locationTracking )
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest().setValidationLevel( validationLevel )
            .setLocationTracking( locationTracking );
        final DefaultModelProblemCollector collector =
            new DefaultModelProblemCollector( new DefaultModelBuildingResult() );
        try
        {
            return newResult( readFileModel( request, collector ), collector.getProblems() );
        }
        catch ( ModelBuildingException e )
        {
            return error( collector.getProblems() );
        }
    }

    private Model readFileModel( ModelBuildingRequest request,
                                 DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        ModelSource modelSource = request.getModelSource();
        Model model = fromCache( request.getModelCache(), modelSource, ModelCacheTag.FILE );
        if ( model == null )
        {
            model = doReadFileModel( modelSource, request, problems );

            intoCache( request.getModelCache(), modelSource, ModelCacheTag.FILE, model );
        }

        if ( modelSource instanceof FileModelSource )
        {
            if ( request.getTransformerContextBuilder() instanceof DefaultTransformerContextBuilder )
            {
                DefaultTransformerContextBuilder contextBuilder =
                        (DefaultTransformerContextBuilder) request.getTransformerContextBuilder();
                contextBuilder.putSource( getGroupId( model ), model.getArtifactId(), modelSource );
            }
        }

        return model;
    }

    @SuppressWarnings( "checkstyle:methodlength" )
    private Model doReadFileModel( ModelSource modelSource, ModelBuildingRequest request,
                                 DefaultModelProblemCollector problems )
            throws ModelBuildingException
    {
        Model model;
        problems.setSource( modelSource.getLocation() );
        try
        {
            boolean strict = request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;

            Map<String, Object> options = new HashMap<>( 3 );
            options.put( ModelProcessor.IS_STRICT, strict );
            options.put( ModelProcessor.SOURCE, modelSource );

            InputSource source;
            if ( request.isLocationTracking() )
            {
                source = (InputSource) options.computeIfAbsent( ModelProcessor.INPUT_SOURCE, k -> new InputSource() );
            }
            else
            {
                source = null;
            }

            try
            {
                model = modelProcessor.read( modelSource.getInputStream(), options );
            }
            catch ( ModelParseException e )
            {
                if ( !strict )
                {
                    throw e;
                }

                options.put( ModelProcessor.IS_STRICT, Boolean.FALSE );

                try
                {
                    model = modelProcessor.read( modelSource.getInputStream(), options );
                }
                catch ( ModelParseException ne )
                {
                    // still unreadable even in non-strict mode, rethrow original error
                    throw e;
                }

                Severity severity = ( modelSource instanceof FileModelSource ) ? Severity.ERROR : Severity.WARNING;
                problems.add( new ModelProblemCollectorRequest( severity, Version.V20 )
                    .setMessage( "Malformed POM " + modelSource.getLocation() + ": " + e.getMessage() )
                    .setException( e ) );
            }

            if ( source != null )
            {
                source.setModelId( ModelProblemUtils.toId( model ) );
                source.setLocation( modelSource.getLocation() );
            }
        }
        catch ( ModelParseException e )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE )
                .setMessage( "Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage() )
                .setException( e ) );
            throw problems.newModelBuildingException();
        }
        catch ( IOException e )
        {
            String msg = e.getMessage();
            if ( msg == null || msg.length() <= 0 )
            {
                // NOTE: There's java.nio.charset.MalformedInputException and sun.io.MalformedInputException
                if ( e.getClass().getName().endsWith( "MalformedInputException" ) )
                {
                    msg = "Some input bytes do not match the file encoding.";
                }
                else
                {
                    msg = e.getClass().getSimpleName();
                }
            }
            problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE )
                .setMessage( "Non-readable POM " + modelSource.getLocation() + ": " + msg ).setException( e ) );
            throw problems.newModelBuildingException();
        }

        if ( modelSource instanceof FileModelSource )
        {
            model.setPomFile( ( (FileModelSource) modelSource ).getFile() );
        }
        problems.setSource( model );

        modelValidator.validateFileModel( model, request, problems );

        if ( hasFatalErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        return model;
    }

    private Model readRawModel( ModelBuildingRequest request, DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        ModelSource modelSource = request.getModelSource();

        ModelData cachedData = fromCache( request.getModelCache(), modelSource, ModelCacheTag.RAW );
        if ( cachedData != null )
        {
            return cachedData.getModel();
        }

        Model rawModel;
        if ( Features.buildConsumer( request.getUserProperties() ).isActive()
            && modelSource instanceof FileModelSource )
        {
            rawModel = readFileModel( request, problems );
            File pomFile = ( (FileModelSource) modelSource ).getFile();

            TransformerContext context = null;
            if ( request.getTransformerContextBuilder() != null )
            {
                context = request.getTransformerContextBuilder().initialize( request, problems );
            }

            try
            {
                // must implement TransformContext, but should use request to access system properties/modelcache
                Model transformedFileModel = modelProcessor.read( pomFile,
                   Collections.singletonMap( ModelReader.TRANSFORMER_CONTEXT, context ) );

                // rawModel with locationTrackers, required for proper feedback during validations

                // Apply enriched data
                modelMerger.merge( rawModel, transformedFileModel, false, null );
            }
            catch ( IOException e )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V40 ).setException( e ) );
            }
        }
        else if ( request.getFileModel() == null )
        {
            rawModel = readFileModel( request, problems );
        }
        else
        {
            rawModel = request.getFileModel().clone();
        }

        modelValidator.validateRawModel( rawModel, request, problems );

        if ( hasFatalErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        String groupId = getGroupId( rawModel );
        String artifactId = rawModel.getArtifactId();
        String version = getVersion( rawModel );

        ModelData modelData = new ModelData( modelSource, rawModel, groupId, artifactId, version );
        intoCache( request.getModelCache(), modelSource, ModelCacheTag.RAW, modelData );

        return rawModel;
    }

    private String getGroupId( Model model )
    {
        String groupId = model.getGroupId();
        if ( groupId == null && model.getParent() != null )
        {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    private String getVersion( Model model )
    {
        String version = model.getVersion();
        if ( version == null && model.getParent() != null )
        {
            version = model.getParent().getVersion();
        }
        return version;
    }

    private DefaultProfileActivationContext getProfileActivationContext( ModelBuildingRequest request )
    {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        context.setActiveProfileIds( request.getActiveProfileIds() );
        context.setInactiveProfileIds( request.getInactiveProfileIds() );
        context.setSystemProperties( request.getSystemProperties() );
        context.setUserProperties( request.getUserProperties() );
        context.setProjectDirectory( ( request.getPomFile() != null ) ? request.getPomFile().getParentFile() : null );

        return context;
    }

    private void configureResolver( ModelResolver modelResolver, Model model, DefaultModelProblemCollector problems )
    {
        configureResolver( modelResolver, model, problems, false );
    }

    private void configureResolver( ModelResolver modelResolver, Model model, DefaultModelProblemCollector problems,
                                    boolean replaceRepositories )
    {
        if ( modelResolver == null )
        {
            return;
        }

        problems.setSource( model );

        List<Repository> repositories = model.getRepositories();

        for ( Repository repository : repositories )
        {
            try
            {
                modelResolver.addRepository( repository, replaceRepositories );
            }
            catch ( InvalidRepositoryException e )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Invalid repository " + repository.getId() + ": " + e.getMessage() )
                    .setLocation( repository.getLocation( "" ) ).setException( e ) );
            }
        }
    }

    private void checkPluginVersions( List<Model> lineage, ModelBuildingRequest request,
                                      ModelProblemCollector problems )
    {
        if ( request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            return;
        }

        Map<String, Plugin> plugins = new HashMap<>();
        Map<String, String> versions = new HashMap<>();
        Map<String, String> managedVersions = new HashMap<>();

        for ( int i = lineage.size() - 1; i >= 0; i-- )
        {
            Model model = lineage.get( i );
            Build build = model.getBuild();
            if ( build != null )
            {
                for ( Plugin plugin : build.getPlugins() )
                {
                    String key = plugin.getKey();
                    if ( versions.get( key ) == null )
                    {
                        versions.put( key, plugin.getVersion() );
                        plugins.put( key, plugin );
                    }
                }
                PluginManagement mgmt = build.getPluginManagement();
                if ( mgmt != null )
                {
                    for ( Plugin plugin : mgmt.getPlugins() )
                    {
                        String key = plugin.getKey();
                        managedVersions.computeIfAbsent( key, k -> plugin.getVersion() );
                    }
                }
            }
        }

        for ( String key : versions.keySet() )
        {
            if ( versions.get( key ) == null && managedVersions.get( key ) == null )
            {
                InputLocation location = plugins.get( key ).getLocation( "" );
                problems
                    .add( new ModelProblemCollectorRequest( Severity.WARNING, Version.V20 )
                        .setMessage( "'build.plugins.plugin.version' for " + key + " is missing." )
                        .setLocation( location ) );
            }
        }
    }

    private void assembleInheritance( List<Model> lineage, ModelBuildingRequest request,
                                      ModelProblemCollector problems )
    {
        for ( int i = lineage.size() - 2; i >= 0; i-- )
        {
            Model parent = lineage.get( i + 1 );
            Model child = lineage.get( i );
            inheritanceAssembler.assembleModelInheritance( child, parent, request, problems );
        }
    }

    private Map<String, Activation> getProfileActivations( Model model, boolean clone )
    {
        Map<String, Activation> activations = new HashMap<>();
        for ( Profile profile : model.getProfiles() )
        {
            Activation activation = profile.getActivation();

            if ( activation == null )
            {
                continue;
            }

            if ( clone )
            {
                activation = activation.clone();
            }

            activations.put( profile.getId(), activation );
        }

        return activations;
    }

    private void injectProfileActivations( Model model, Map<String, Activation> activations )
    {
        for ( Profile profile : model.getProfiles() )
        {
            Activation activation = profile.getActivation();

            if ( activation == null )
            {
                continue;
            }

            // restore activation
            profile.setActivation( activations.get( profile.getId() ) );
        }
    }

    private Model interpolateModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        // save profile activations before interpolation, since they are evaluated with limited scope
        Map<String, Activation> originalActivations = getProfileActivations( model, true );

        Model interpolatedModel =
            modelInterpolator.interpolateModel( model, model.getProjectDirectory(), request, problems );
        if ( interpolatedModel.getParent() != null )
        {
            StringSearchInterpolator ssi = new StringSearchInterpolator();
            ssi.addValueSource( new MapBasedValueSource( request.getUserProperties() ) );

            ssi.addValueSource( new MapBasedValueSource( model.getProperties() ) );

            ssi.addValueSource( new MapBasedValueSource( request.getSystemProperties() ) );

            try
            {
                String interpolated = ssi.interpolate( interpolatedModel.getParent().getVersion() );
                interpolatedModel.getParent().setVersion( interpolated );
            }
            catch ( Exception e )
            {
                ModelProblemCollectorRequest mpcr =
                    new ModelProblemCollectorRequest( Severity.ERROR,
                                                      Version.BASE ).setMessage( "Failed to interpolate field: "
                                                          + interpolatedModel.getParent().getVersion()
                                                          + " on class: " ).setException( e );
                problems.add( mpcr );
            }


        }
        interpolatedModel.setPomFile( model.getPomFile() );

        // restore profiles with file activation to their value before full interpolation
        injectProfileActivations( model, originalActivations );

        return interpolatedModel;
    }

    private ModelData readParent( Model childModel, Source childSource, ModelBuildingRequest request,
                                  ModelBuildingResult result, DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        ModelData parentData = null;

        Parent parent = childModel.getParent();
        if ( parent != null )
        {
            parentData = readParentLocally( childModel, childSource, request, result, problems );
            if ( parentData == null )
            {
                parentData = readParentExternally( childModel, request, result, problems );
            }

            Model parentModel = parentData.getModel();
            if ( !"pom".equals( parentModel.getPackaging() ) )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint( parentModel )
                                     + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"" )
                    .setLocation( parentModel.getLocation( "packaging" ) ) );
            }
        }

        return parentData;
    }

    private ModelData readParentLocally( Model childModel, Source childSource, ModelBuildingRequest request,
                                         ModelBuildingResult result, DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        final Parent parent = childModel.getParent();
        final ModelSource candidateSource;
        final Model candidateModel;
        final WorkspaceModelResolver resolver = request.getWorkspaceModelResolver();
        if ( resolver == null )
        {
            candidateSource = getParentPomFile( childModel, childSource );

            if ( candidateSource == null )
            {
                return null;
            }

            ModelBuildingRequest candidateBuildRequest = new DefaultModelBuildingRequest( request )
                .setModelSource( candidateSource );

            candidateModel = readRawModel( candidateBuildRequest, problems );
        }
        else
        {
            try
            {
                candidateModel =
                    resolver.resolveRawModel( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
            }
            catch ( UnresolvableModelException e )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE ) //
                    .setMessage( e.getMessage() ).setLocation( parent.getLocation( "" ) ).setException( e ) );
                throw problems.newModelBuildingException();
            }
            if ( candidateModel == null )
            {
                return null;
            }
            candidateSource = new FileModelSource( candidateModel.getPomFile() );
        }

        //
        // TODO jvz Why isn't all this checking the job of the duty of the workspace resolver, we know that we
        // have a model that is suitable, yet more checks are done here and the one for the version is problematic
        // before because with parents as ranges it will never work in this scenario.
        //

        String groupId = getGroupId( candidateModel );
        String artifactId = candidateModel.getArtifactId();

        if ( groupId == null || !groupId.equals( parent.getGroupId() ) || artifactId == null
            || !artifactId.equals( parent.getArtifactId() ) )
        {
            StringBuilder buffer = new StringBuilder( 256 );
            buffer.append( "'parent.relativePath'" );
            if ( childModel != problems.getRootModel() )
            {
                buffer.append( " of POM " ).append( ModelProblemUtils.toSourceHint( childModel ) );
            }
            buffer.append( " points at " ).append( groupId ).append( ':' ).append( artifactId );
            buffer.append( " instead of " ).append( parent.getGroupId() ).append( ':' );
            buffer.append( parent.getArtifactId() ).append( ", please verify your project structure" );

            problems.setSource( childModel );
            problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.BASE )
                .setMessage( buffer.toString() ).setLocation( parent.getLocation( "" ) ) );
            return null;
        }

        String version = getVersion( candidateModel );
        if ( version != null && parent.getVersion() != null && !version.equals( parent.getVersion() ) )
        {
            try
            {
                VersionRange parentRange = VersionRange.createFromVersionSpec( parent.getVersion() );
                if ( !parentRange.hasRestrictions() )
                {
                    // the parent version is not a range, we have version skew, drop back to resolution from repo
                    return null;
                }
                if ( !parentRange.containsVersion( new DefaultArtifactVersion( version ) ) )
                {
                    // version skew drop back to resolution from the repository
                    return null;
                }

                // Validate versions aren't inherited when using parent ranges the same way as when read externally.
                if ( childModel.getVersion() == null )
                {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                        .setMessage( "Version must be a constant" ).setLocation( childModel.getLocation( "" ) ) );

                }
                else
                {
                    if ( childModel.getVersion().contains( "${" ) )
                    {
                        // Message below is checked for in the MNG-2199 core IT.
                        problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                            .setMessage( "Version must be a constant" )
                            .setLocation( childModel.getLocation( "version" ) ) );

                    }
                }

                // MNG-2199: What else to check here ?
            }
            catch ( InvalidVersionSpecificationException e )
            {
                // invalid version range, so drop back to resolution from the repository
                return null;
            }
        }

        //
        // Here we just need to know that a version is fine to use but this validation we can do in our workspace
        // resolver.
        //

        /*
         * if ( version == null || !version.equals( parent.getVersion() ) ) { return null; }
         */

        return new ModelData( candidateSource, candidateModel, groupId, artifactId, version );
    }

    private ModelSource getParentPomFile( Model childModel, Source source )
    {
        if ( !( source instanceof ModelSource2 ) )
        {
            return null;
        }

        String parentPath = childModel.getParent().getRelativePath();

        if ( parentPath == null || parentPath.length() <= 0 )
        {
            return null;
        }

        return ( (ModelSource2) source ).getRelatedSource( parentPath );
    }

    private ModelData readParentExternally( Model childModel, ModelBuildingRequest request,
                                            ModelBuildingResult result, DefaultModelProblemCollector problems )
        throws ModelBuildingException
    {
        problems.setSource( childModel );

        Parent parent = childModel.getParent();

        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();

        ModelResolver modelResolver = request.getModelResolver();
        Objects.requireNonNull( modelResolver,
                                String.format( "request.modelResolver cannot be null (parent POM %s and POM %s)",
                                               ModelProblemUtils.toId( groupId, artifactId, version ),
                                               ModelProblemUtils.toSourceHint( childModel ) ) );

        ModelSource modelSource;
        try
        {
            modelSource = modelResolver.resolveModel( parent );
        }
        catch ( UnresolvableModelException e )
        {
            // Message below is checked for in the MNG-2199 core IT.
            StringBuilder buffer = new StringBuilder( 256 );
            buffer.append( "Non-resolvable parent POM" );
            if ( !containsCoordinates( e.getMessage(), groupId, artifactId, version ) )
            {
                buffer.append( ' ' ).append( ModelProblemUtils.toId( groupId, artifactId, version ) );
            }
            if ( childModel != problems.getRootModel() )
            {
                buffer.append( " for " ).append( ModelProblemUtils.toId( childModel ) );
            }
            buffer.append( ": " ).append( e.getMessage() );
            if ( childModel.getProjectDirectory() != null )
            {
                if ( parent.getRelativePath() == null || parent.getRelativePath().length() <= 0 )
                {
                    buffer.append( " and 'parent.relativePath' points at no local POM" );
                }
                else
                {
                    buffer.append( " and 'parent.relativePath' points at wrong local POM" );
                }
            }

            problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE )
                .setMessage( buffer.toString() ).setLocation( parent.getLocation( "" ) ).setException( e ) );
            throw problems.newModelBuildingException();
        }

        int validationLevel = Math.min( request.getValidationLevel(), ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 );
        ModelBuildingRequest lenientRequest = new DefaultModelBuildingRequest( request )
                .setValidationLevel( validationLevel )
                .setFileModel( null )
                .setModelSource( modelSource );

        Model parentModel = readRawModel( lenientRequest, problems );

        if ( !parent.getVersion().equals( version ) )
        {
            if ( childModel.getVersion() == null )
            {
                // Message below is checked for in the MNG-2199 core IT.
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                    .setMessage( "Version must be a constant" ).setLocation( childModel.getLocation( "" ) ) );

            }
            else
            {
                if ( childModel.getVersion().contains( "${" ) )
                {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                        .setMessage( "Version must be a constant" )
                        .setLocation( childModel.getLocation( "version" ) ) );

                }
            }

            // MNG-2199: What else to check here ?
        }

        return new ModelData( modelSource, parentModel, parent.getGroupId(), parent.getArtifactId(),
                              parent.getVersion() );
    }

    private Model getSuperModel()
    {
        return superPomProvider.getSuperModel( "4.0.0" ).clone();
    }

    private void importDependencyManagement( Model model, ModelBuildingRequest request,
                                             DefaultModelProblemCollector problems, Collection<String> importIds )
    {
        DependencyManagement depMgmt = model.getDependencyManagement();

        if ( depMgmt == null )
        {
            return;
        }

        String importing = model.getGroupId() + ':' + model.getArtifactId() + ':' + model.getVersion();

        importIds.add( importing );

        List<DependencyManagement> importMgmts = null;

        for ( Iterator<Dependency> it = depMgmt.getDependencies().iterator(); it.hasNext(); )
        {
            Dependency dependency = it.next();

            if ( !"pom".equals( dependency.getType() ) || !"import".equals( dependency.getScope() ) )
            {
                continue;
            }

            it.remove();

            DependencyManagement importMgmt = loadDependencyManagement( model, request, problems,
                                                                        dependency, importIds );

            if ( importMgmt != null )
            {
                if ( importMgmts == null )
                {
                    importMgmts = new ArrayList<>();
                }

                importMgmts.add( importMgmt );
            }
        }

        importIds.remove( importing );

        dependencyManagementImporter.importManagement( model, importMgmts, request, problems );
    }

    private DependencyManagement loadDependencyManagement( Model model, ModelBuildingRequest request,
                                                           DefaultModelProblemCollector problems,
                                                           Dependency dependency,
                                                           Collection<String> importIds )
    {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        if ( groupId == null || groupId.length() <= 0 )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "'dependencyManagement.dependencies.dependency.groupId' for "
                                     + dependency.getManagementKey() + " is missing." )
                    .setLocation( dependency.getLocation( "" ) ) );
            return null;
        }
        if ( artifactId == null || artifactId.length() <= 0 )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "'dependencyManagement.dependencies.dependency.artifactId' for "
                                     + dependency.getManagementKey() + " is missing." )
                    .setLocation( dependency.getLocation( "" ) ) );
            return null;
        }
        if ( version == null || version.length() <= 0 )
        {
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( "'dependencyManagement.dependencies.dependency.version' for "
                                     + dependency.getManagementKey() + " is missing." )
                    .setLocation( dependency.getLocation( "" ) ) );
            return null;
        }

        String imported = groupId + ':' + artifactId + ':' + version;

        if ( importIds.contains( imported ) )
        {
            String message = "The dependencies of type=pom and with scope=import form a cycle: ";
            for ( String modelId : importIds )
            {
                message += modelId + " -> ";
            }
            message += imported;
            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage( message ) );

            return null;
        }

        DependencyManagement importMgmt = fromCache( request.getModelCache(), groupId, artifactId, version,
                                                    ModelCacheTag.IMPORT );
        if ( importMgmt == null )
        {
            importMgmt = doLoadDependencyManagement( model, request, problems, dependency,
                                                     groupId, artifactId, version, importIds );
            if ( importMgmt != null )
            {
                intoCache( request.getModelCache(), groupId, artifactId, version, ModelCacheTag.IMPORT, importMgmt );
            }
        }

        return importMgmt;
    }

    @SuppressWarnings( "checkstyle:parameternumber" )
    private DependencyManagement doLoadDependencyManagement( Model model, ModelBuildingRequest request,
                                                             DefaultModelProblemCollector problems,
                                                             Dependency dependency,
                                                             String groupId,
                                                             String artifactId,
                                                             String version,
                                                             Collection<String> importIds )
    {
        DependencyManagement importMgmt;
        final WorkspaceModelResolver workspaceResolver = request.getWorkspaceModelResolver();
        final ModelResolver modelResolver = request.getModelResolver();
        if ( workspaceResolver == null && modelResolver == null )
        {
            throw new NullPointerException( String.format(
                "request.workspaceModelResolver and request.modelResolver cannot be null (parent POM %s and POM %s)",
                ModelProblemUtils.toId( groupId, artifactId, version ),
                ModelProblemUtils.toSourceHint( model ) ) );
        }

        Model importModel = null;
        if ( workspaceResolver != null )
        {
            try
            {
                importModel = workspaceResolver.resolveEffectiveModel( groupId, artifactId, version );
            }
            catch ( UnresolvableModelException e )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE )
                    .setMessage( e.getMessage() ).setException( e ) );
                return null;
            }
        }

        // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
        if ( importModel == null )
        {
            final ModelSource importSource;
            try
            {
                importSource = modelResolver.resolveModel( dependency );
            }
            catch ( UnresolvableModelException e )
            {
                StringBuilder buffer = new StringBuilder( 256 );
                buffer.append( "Non-resolvable import POM" );
                if ( !containsCoordinates( e.getMessage(), groupId, artifactId, version ) )
                {
                    buffer.append( ' ' ).append( ModelProblemUtils.toId( groupId, artifactId, version ) );
                }
                buffer.append( ": " ).append( e.getMessage() );

                problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage( buffer.toString() ).setLocation( dependency.getLocation( "" ) )
                    .setException( e ) );
                return null;
            }

            final ModelBuildingResult importResult;
            try
            {
                ModelBuildingRequest importRequest = new DefaultModelBuildingRequest();
                importRequest.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
                importRequest.setModelCache( request.getModelCache() );
                importRequest.setSystemProperties( request.getSystemProperties() );
                importRequest.setUserProperties( request.getUserProperties() );
                importRequest.setLocationTracking( request.isLocationTracking() );

                importRequest.setModelSource( importSource );
                importRequest.setModelResolver( modelResolver.newCopy() );

                importResult = build( importRequest, importIds );
            }
            catch ( ModelBuildingException e )
            {
                problems.addAll( e.getProblems() );
                return null;
            }

            problems.addAll( importResult.getProblems() );

            importModel = importResult.getEffectiveModel();
        }

        importMgmt = importModel.getDependencyManagement();

        if ( importMgmt == null )
        {
            importMgmt = new DependencyManagement();
        }
        return importMgmt;
    }

    private <T> void intoCache( ModelCache modelCache, String groupId, String artifactId, String version,
                               ModelCacheTag<T> tag, T data )
    {
        if ( modelCache != null )
        {
            modelCache.put( groupId, artifactId, version, tag, data );
        }
    }

    private <T> void intoCache( ModelCache modelCache, Source source, ModelCacheTag<T> tag, T data )
    {
        if ( modelCache != null )
        {
            modelCache.put( source, tag, data );
        }
    }

    private static <T> T fromCache( ModelCache modelCache, String groupId, String artifactId, String version,
                            ModelCacheTag<T> tag )
    {
        if ( modelCache != null )
        {
            return modelCache.get( groupId, artifactId, version, tag );
        }
        return null;
    }

    private static <T> T fromCache( ModelCache modelCache, Source source, ModelCacheTag<T> tag )
    {
        if ( modelCache != null )
        {
            return modelCache.get( source, tag );
        }
        return null;
    }

    private void fireEvent( Model model, ModelBuildingRequest request, ModelProblemCollector problems,
                            ModelBuildingEventCatapult catapult )
        throws ModelBuildingException
    {
        ModelBuildingListener listener = request.getModelBuildingListener();

        if ( listener != null )
        {
            ModelBuildingEvent event = new DefaultModelBuildingEvent( model, request, problems );

            catapult.fire( listener, event );
        }
    }

    private boolean containsCoordinates( String message, String groupId, String artifactId, String version )
    {
        return message != null && ( groupId == null || message.contains( groupId ) )
            && ( artifactId == null || message.contains( artifactId ) )
            && ( version == null || message.contains( version ) );
    }

    protected boolean hasModelErrors( ModelProblemCollectorExt problems )
    {
        if ( problems instanceof DefaultModelProblemCollector )
        {
            return ( (DefaultModelProblemCollector) problems ).hasErrors();
        }
        else
        {
            // the default execution path only knows the DefaultModelProblemCollector,
            // only reason it's not in signature is because it's package private
            throw new IllegalStateException();
        }
    }

    protected boolean hasFatalErrors( ModelProblemCollectorExt problems )
    {
        if ( problems instanceof DefaultModelProblemCollector )
        {
            return ( (DefaultModelProblemCollector) problems ).hasFatalErrors();
        }
        else
        {
            // the default execution path only knows the DefaultModelProblemCollector,
            // only reason it's not in signature is because it's package private
            throw new IllegalStateException();
        }
    }

    /**
     * Builds up the transformer context.
     * After the buildplan is ready, the build()-method returns the immutable context useful during distribution.
     * This is an inner class, as it must be able to call readRawModel()
     *
     * @author Robert Scholte
     * @since 4.0.0
     */
    private class DefaultTransformerContextBuilder implements TransformerContextBuilder
    {
        private final DefaultTransformerContext context = new DefaultTransformerContext();

        private final Map<DefaultTransformerContext.GAKey, Set<Source>> mappedSources
                = new ConcurrentHashMap<>( 64 );

        /**
         * If an interface could be extracted, DefaultModelProblemCollector should be ModelProblemCollectorExt
         *
         * @param request
         * @param collector
         * @return
         */
        @Override
        public TransformerContext initialize( ModelBuildingRequest request, ModelProblemCollector collector )
        {
            // We must assume the TransformerContext was created using this.newTransformerContextBuilder()
            DefaultModelProblemCollector problems = (DefaultModelProblemCollector) collector;
            return new TransformerContext()
            {
                @Override
                public String getUserProperty( String key )
                {
                    return context.userProperties.computeIfAbsent( key,
                                                           k -> request.getUserProperties().getProperty( key ) );
                }

                @Override
                public Model getRawModel( String gId, String aId )
                {
                    return context.modelByGA.computeIfAbsent( new DefaultTransformerContext.GAKey( gId, aId ),
                                                              k -> new DefaultTransformerContext.Holder() )
                            .computeIfAbsent( () -> findRawModel( gId, aId ) );
                }

                @Override
                public Model getRawModel( Path path )
                {
                    return context.modelByPath.computeIfAbsent( path,
                                                                k -> new DefaultTransformerContext.Holder() )
                            .computeIfAbsent( () -> findRawModel( path ) );
                }

                private Model findRawModel( String groupId, String artifactId )
                {
                    Source source = getSource( groupId, artifactId );
                    if ( source != null )
                    {
                        try
                        {
                            ModelBuildingRequest gaBuildingRequest = new DefaultModelBuildingRequest( request )
                                .setModelSource( (ModelSource) source );
                            return readRawModel( gaBuildingRequest, problems );
                        }
                        catch ( ModelBuildingException e )
                        {
                            // gathered with problem collector
                        }
                    }
                    return null;
                }

                private Model findRawModel( Path p )
                {
                    if ( !Files.isRegularFile( p ) )
                    {
                        throw new IllegalArgumentException( "Not a regular file: " + p );
                    }

                    DefaultModelBuildingRequest req = new DefaultModelBuildingRequest( request )
                                    .setPomFile( p.toFile() )
                                    .setModelSource( new FileModelSource( p.toFile() ) );

                    try
                    {
                        return readRawModel( req, problems );
                    }
                    catch ( ModelBuildingException e )
                    {
                        // gathered with problem collector
                    }
                    return null;
                }
            };
        }

        @Override
        public TransformerContext build()
        {
            return context;
        }

        public Source getSource( String groupId, String artifactId )
        {
            Set<Source> sources = mappedSources.get( new DefaultTransformerContext.GAKey( groupId, artifactId ) );
            if ( sources == null )
            {
                return null;
            }
            return sources.stream().reduce( ( a, b ) ->
            {
                throw new IllegalStateException( String.format( "No unique Source for %s:%s: %s and %s",
                                                                groupId, artifactId,
                                                                a.getLocation(), b.getLocation() ) );
            } ).orElse( null );
        }

        public void putSource( String groupId, String artifactId, Source source )
        {
            mappedSources.computeIfAbsent( new DefaultTransformerContext.GAKey( groupId, artifactId ),
                    k -> new HashSet<>() ).add( source );
        }

    }
}
