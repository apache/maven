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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
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
import org.apache.maven.model.finalization.ModelFinalizer;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.validation.ModelValidator;
import org.apache.maven.model.versioning.ModelVersions;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

import static org.apache.maven.model.building.Result.error;
import static org.apache.maven.model.building.Result.newResult;

/**
 * @author Benjamin Bentmann
 */
@Component( role = ModelBuilder.class )
public class DefaultModelBuilder
    implements ModelBuilder
{

    @Requirement
    private ModelProcessor modelProcessor;

    @Requirement
    private ModelValidator modelValidator;

    @Requirement
    private ModelNormalizer modelNormalizer;

    @Requirement
    private ModelInterpolator modelInterpolator;

    @Requirement
    private ModelPathTranslator modelPathTranslator;

    @Requirement
    private ModelUrlNormalizer modelUrlNormalizer;

    @Requirement
    private SuperPomProvider superPomProvider;

    @Requirement
    private InheritanceAssembler inheritanceAssembler;

    @Requirement
    private ProfileSelector profileSelector;

    @Requirement
    private ProfileInjector profileInjector;

    @Requirement
    private PluginManagementInjector pluginManagementInjector;

    @Requirement
    private DependencyManagementInjector dependencyManagementInjector;

    @Requirement
    private DependencyManagementImporter dependencyManagementImporter;

    @Requirement( optional = true )
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Requirement
    private PluginConfigurationExpander pluginConfigurationExpander;

    @Requirement
    private ReportConfigurationExpander reportConfigurationExpander;

    @Requirement
    private ReportingConverter reportingConverter;

    @Requirement( optional = true )
    private List<ModelFinalizer> modelFinalizers;

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

    public DefaultModelBuilder setDependencyManagementImporter( DependencyManagementImporter depMngmntImporter )
    {
        this.dependencyManagementImporter = depMngmntImporter;
        return this;
    }

    public DefaultModelBuilder setDependencyManagementInjector( DependencyManagementInjector depMngmntInjector )
    {
        this.dependencyManagementInjector = depMngmntInjector;
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

    /**
     * @since 3.4
     */
    public DefaultModelBuilder setModelFinalizers( List<ModelFinalizer> value )
    {
        this.modelFinalizers = value;
        return this;
    }

    @Override
    public ModelBuildingResult build( ModelBuildingRequest request )
        throws ModelBuildingException
    {
        // phase 1
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector( result );

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext( request );

        problems.setSource( "(external profiles)" );
        List<Profile> activeExternalProfiles =
            profileSelector.getActiveProfiles( request.getProfiles(), profileActivationContext, problems );

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

        // read and validate raw model
        Model inputModel = request.getRawModel();
        if ( inputModel == null )
        {
            inputModel = readModel( request.getModelSource(), request.getPomFile(), request, problems );
        }

        problems.setRootModel( inputModel );

        ModelData resultData = new ModelData( request.getModelSource(), inputModel );
        ModelData superData = new ModelData( null, getSuperModel( inputModel.getModelVersion() ) );

        Collection<String> parentIds = new LinkedHashSet<>();
        List<ModelData> lineage = new ArrayList<>();

        for ( ModelData currentData = resultData; currentData != null; )
        {
            lineage.add( currentData );

            Model rawModel = currentData.getModel();
            currentData.setRawModel( rawModel );

            Model tmpModel = rawModel.clone();
            currentData.setModel( tmpModel );

            problems.setSource( tmpModel );

            // model normalization
            modelNormalizer.mergeDuplicates( tmpModel, request, problems );

            profileActivationContext.setProjectProperties( tmpModel.getProperties() );

            List<Profile> activePomProfiles =
                profileSelector.getActiveProfiles( rawModel.getProfiles(), profileActivationContext, problems );

            currentData.setActiveProfiles( activePomProfiles );

            Map<String, Activation> interpolatedActivations = getProfileActivations( rawModel, false );
            injectProfileActivations( tmpModel, interpolatedActivations );

            // profile injection
            for ( Profile activeProfile : activePomProfiles )
            {
                profileInjector.injectProfile( tmpModel, activeProfile, request, problems );
            }

            if ( currentData == resultData )
            {
                for ( Profile activeProfile : activeExternalProfiles )
                {
                    profileInjector.injectProfile( tmpModel, activeProfile, request, problems );
                }
            }

            if ( currentData == superData )
            {
                break;
            }

            configureResolver( request.getModelResolver(), tmpModel, problems );

            ModelData parentData = readParent( tmpModel, currentData.getSource(), request, problems );

            if ( parentData == null )
            {
                currentData = superData;
            }
            else if ( currentData == resultData )
            { // First iteration - add initial id after version resolution.
                currentData.setGroupId( currentData.getRawModel().getGroupId() == null ? parentData.getGroupId()
                                : currentData.getRawModel().getGroupId() );

                currentData.setVersion( currentData.getRawModel().getVersion() == null ? parentData.getVersion()
                                : currentData.getRawModel().getVersion() );

                currentData.setArtifactId( currentData.getRawModel().getArtifactId() );
                parentIds.add( currentData.getId() );
                // Reset - only needed for 'getId'.
                currentData.setGroupId( null );
                currentData.setArtifactId( null );
                currentData.setVersion( null );
                currentData = parentData;
            }
            else if ( !parentIds.add( parentData.getId() ) )
            {
                String message = "The parents form a cycle: ";
                for ( String modelId : parentIds )
                {
                    message += modelId + " -> ";
                }
                message += parentData.getId();

                problems.add( new ModelProblemCollectorRequest( ModelProblem.Severity.FATAL, ModelProblem.Version.BASE )
                    .setMessage( message ) );

                throw problems.newModelBuildingException();
            }
            else
            {
                currentData = parentData;
            }
        }

        problems.setSource( inputModel );
        checkModelVersions( lineage, request, problems );
        checkPluginVersions( lineage, request, problems );

        // [MNG-4052] import scope dependencies prefer to download pom rather than find it in the current project
        // [MNG-5971] Imported dependencies should be available to inheritance processing
        //
        // This first phase of model building is used for building models holding just enough information to map
        // groupId:artifactId:version to pom files and to provide modules to build. For this, inheritance and
        // interpolation needs to be performed. A temporary model is built in phase 1 applying inheritance and
        // interpolation to fill in those values but is not returned. The rest of the model building takes place in
        // phase 2.
        final DefaultModelProblemCollector intermediateProblems = new DefaultModelProblemCollector( result );
        final List<Model> intermediateLineage = new ArrayList<>( lineage.size() );
        for ( final ModelData modelData : lineage )
        {
            intermediateLineage.add( modelData.getModel().clone() );
        }
        assembleInheritance( intermediateLineage, request, intermediateProblems );

        Model intermediateModel = intermediateLineage.get( 0 );
        intermediateModel = interpolateModel( intermediateModel, request, intermediateProblems );

        Model resultModel = resultData.getModel();

        resultModel.setGroupId( intermediateModel.getGroupId() );
        resultModel.setArtifactId( intermediateModel.getArtifactId() );
        resultModel.setVersion( intermediateModel.getVersion() );

        problems.setSource( resultModel );
        problems.setRootModel( resultModel );

        resultData.setGroupId( resultModel.getGroupId() );
        resultData.setArtifactId( resultModel.getArtifactId() );
        resultData.setVersion( resultModel.getVersion() );

        result.setEffectiveModel( resultModel );

        for ( ModelData currentData : lineage )
        {
            String modelId = ( currentData != superData ) ? currentData.getId() : "";

            result.addModelId( modelId );
            result.setActivePomProfiles( modelId, currentData.getActiveProfiles() );
            result.setRawModel( modelId, currentData.getRawModel() );
            result.setEffectiveModel( modelId, currentData.getModel() );
        }

        if ( !request.isTwoPhaseBuilding() )
        {
            build( request, result );
        }

        return result;
    }

    @Override
    public ModelBuildingResult build( ModelBuildingRequest request, ModelBuildingResult result )
        throws ModelBuildingException
    {
        // phase 2
        Model resultModel = result.getEffectiveModel();

        // Reset to on-disk values to not suppress any warnings from phase 1.
        resultModel.setGroupId( result.getRawModel().getGroupId() );
        resultModel.setArtifactId( result.getRawModel().getArtifactId() );
        resultModel.setVersion( result.getRawModel().getVersion() );

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector( result );
        problems.setSource( resultModel );
        problems.setRootModel( resultModel );

        final List<Model> lineage = new ArrayList<>( result.getModelIds().size() );

        for ( final String modelId : result.getModelIds() )
        {
            lineage.add( result.getEffectiveModel( modelId ) );
        }

        if ( ModelVersions.supportsDependencyManagementImportInheritanceProcessing( resultModel ) )
        {
            // [MNG-5971] Imported dependencies should be available to inheritance processing
            processImports( lineage, request, problems );
        }

        problems.setSource( resultModel );

        // inheritance assembly
        assembleInheritance( lineage, request, problems );

        resultModel = interpolateModel( resultModel, request, problems );

        // url normalization
        modelUrlNormalizer.normalize( resultModel, request );

        // Now the fully interpolated model is available: reconfigure the resolver
        configureResolver( request.getModelResolver(), resultModel, problems, true );

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

        if ( !ModelVersions.supportsDependencyManagementImportInheritanceProcessing( resultModel ) )
        {
            this.importDependencyManagement( resultModel, "import", request, problems, new HashSet<String>() );
        }

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

        if ( this.modelFinalizers != null )
        {
            for ( final ModelFinalizer modelFinalizer : this.modelFinalizers )
            {
                modelFinalizer.finalizeModel( resultModel, request, problems );
            }
        }

        // effective model validation
        modelValidator.validateEffectiveModel( resultModel, request, problems );

        if ( hasModelErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        return result;
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
            return newResult( readModel( null, pomFile, request, collector ), collector.getProblems() );
        }
        catch ( ModelBuildingException e )
        {
            return error( collector.getProblems() );
        }
    }

    private Model readModel( ModelSource modelSource, File pomFile, ModelBuildingRequest request,
                             DefaultModelProblemCollector problems )
                                 throws ModelBuildingException
    {
        Model model;

        if ( modelSource == null )
        {
            if ( pomFile != null )
            {
                modelSource = new FileModelSource( pomFile );
            }
            else
            {
                throw new NullPointerException( "neither pomFile nor modelSource can be null" );
            }
        }

        problems.setSource( modelSource.getLocation() );
        try
        {
            boolean strict = request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;
            InputSource source = request.isLocationTracking() ? new InputSource() : null;

            Map<String, Object> options = new HashMap<>();
            options.put( ModelProcessor.IS_STRICT, strict );
            options.put( ModelProcessor.INPUT_SOURCE, source );
            options.put( ModelProcessor.SOURCE, modelSource );

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

                if ( pomFile != null )
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.V20 )
                        .setMessage( "Malformed POM " + modelSource.getLocation() + ": " + e.getMessage() )
                        .setException( e ) );

                }
                else
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.V20 )
                        .setMessage( "Malformed POM " + modelSource.getLocation() + ": " + e.getMessage() )
                        .setException( e ) );

                }
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

        model.setPomFile( pomFile );

        problems.setSource( model );

        modelValidator.validateRawModel( model, request, problems );

        if ( hasFatalErrors( problems ) )
        {
            throw problems.newModelBuildingException();
        }

        return model;
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
        if ( modelResolver != null )
        {
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
    }

    private void checkPluginVersions( List<ModelData> lineage, ModelBuildingRequest request,
                                      ModelProblemCollector problems )
    {
        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            Map<String, Plugin> plugins = new HashMap<>();
            Map<String, String> versions = new HashMap<>();
            Map<String, String> managedVersions = new HashMap<>();

            for ( int i = lineage.size() - 1; i >= 0; i-- )
            {
                Model model = lineage.get( i ).getModel();
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
                    PluginManagement mngt = build.getPluginManagement();
                    if ( mngt != null )
                    {
                        for ( Plugin plugin : mngt.getPlugins() )
                        {
                            String key = plugin.getKey();
                            if ( managedVersions.get( key ) == null )
                            {
                                managedVersions.put( key, plugin.getVersion() );
                            }
                        }
                    }
                }
            }

            for ( String key : versions.keySet() )
            {
                if ( versions.get( key ) == null && managedVersions.get( key ) == null )
                {
                    InputLocation location = plugins.get( key ).getLocation( "" );
                    problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.V20 )
                        .setMessage( "'build.plugins.plugin.version' for " + key + " is missing." )
                        .setLocation( location ) );

                }
            }
        }
    }

    private void checkModelVersions( final List<ModelData> lineage, final ModelBuildingRequest request,
                                     final ModelProblemCollector problems )
    {
        if ( request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 )
        {
            final Set<String> modelVersions = new HashSet<>( lineage.size() );

            for ( int i = lineage.size() - 1; i >= 0; i-- )
            {
                final Model model = lineage.get( i ).getModel();

                if ( model.getModelVersion() != null )
                {
                    final boolean initial = modelVersions.isEmpty();

                    if ( modelVersions.add( model.getModelVersion() ) && !initial )
                    {
                        problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.V31 )
                            .setMessage( String.format(
                                    "Cannot inherit from parent '%s' with different model version '%s'."
                                        + " Expected model version '%s'.",
                                    model.getId(), model.getModelVersion(), modelVersions.iterator().next() ) ) );

                    }
                }
            }
        }
    }

    private void processImports( final List<Model> lineage, final ModelBuildingRequest request,
                                 final DefaultModelProblemCollector problems )
    {
        // [MNG-5971] Imported dependencies should be available to inheritance processing
        // It's not possible to support all ${project.xyz} properties in dependency management import declarations
        // because import processing is performed before the final inheritance processing is performed. So the set of
        // ${project.xyz} properties supported in dependency management import declarations is limited.

        final List<Model> intermediateLineage = new ArrayList<>( lineage.size() );

        for ( int i = 0, s0 = lineage.size(); i < s0; i++ )
        {
            intermediateLineage.add( lineage.get( i ).clone() );
        }

        for ( int i = intermediateLineage.size() - 2; i >= 0; i-- )
        {
            final Model parent = intermediateLineage.get( i + 1 );
            final Model child = intermediateLineage.get( i );

            if ( child.getGroupId() == null )
            {
                // Support ${project.groupId} in dependency management import declarations.
                child.setGroupId( parent.getGroupId() );
            }
            if ( child.getVersion() == null )
            {
                // Support ${project.version} in dependency management import declarations.
                child.setVersion( parent.getVersion() );
            }

            final Properties properties = new Properties();
            properties.putAll( parent.getProperties() );
            properties.putAll( child.getProperties() );
            child.setProperties( properties );

            final List<Repository> repositories = new ArrayList<>();
            repositories.addAll( child.getRepositories() );

            for ( final Repository parentRepository : parent.getRepositories() )
            {
                if ( !repositories.contains( parentRepository ) )
                {
                    repositories.add( parentRepository );
                }
            }

            child.setRepositories( repositories );
        }

        final Properties effectiveProperties = intermediateLineage.get( 0 ).getProperties();

        final DefaultModelProblemCollector intermediateProblems =
            new DefaultModelProblemCollector( new DefaultModelBuildingResult() );

        // Interpolates the intermediate model.
        // MNG-6079: Uses the effective properties of the result model to support property overriding.
        for ( int i = 0, s0 = intermediateLineage.size(); i < s0; i++ )
        {
            final Model model = intermediateLineage.get( i );
            model.setProperties( effectiveProperties );
            intermediateProblems.setSource( model );
            this.interpolateModel( model, request, intermediateProblems );
        }

        // Exchanges 'import' scope dependencies in the original lineage with possibly interpolated values.
        for ( int i = 0, s0 = lineage.size(); i < s0; i++ )
        {
            final Model model = lineage.get( i );

            if ( model.getDependencyManagement() != null )
            {
                for ( int j = 0, s1 = model.getDependencyManagement().getDependencies().size(); j < s1; j++ )
                {
                    final Dependency dependency = model.getDependencyManagement().getDependencies().get( j );

                    if ( "import".equals( dependency.getScope() ) && "pom".equals( dependency.getType() ) )
                    {
                        final Dependency interpolated =
                            intermediateLineage.get( i ).getDependencyManagement().getDependencies().get( j );

                        model.getDependencyManagement().getDependencies().set( j, interpolated );
                    }
                }
            }
        }

        // [MNG-4488] [regression] Parent POMs resolved from repository are validated in strict mode
        ModelBuildingRequest lenientRequest = request;
        if ( request.getValidationLevel() > ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            lenientRequest = new FilterModelBuildingRequest( request )
            {

                @Override
                public int getValidationLevel( )
                {
                    return ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;
                }

            };
        }

        // Imports dependencies into the original model using the repositories of the intermediate model.
        for ( int i = 0, s0 = lineage.size(); i < s0; i++ )
        {
            final Model model = lineage.get( i );
            this.configureResolver( lenientRequest.getModelResolver(), intermediateLineage.get( i ), problems, true );
            this.importDependencyManagement( model, "import", lenientRequest, problems, new HashSet<String>() );
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
                ModelProblemCollectorRequest mpcr = new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                    .setMessage(
                        "Failed to interpolate field: " + interpolatedModel.getParent().getVersion() + " on class: " )
                    .setException( e );
                problems.add( mpcr );
            }

        }
        interpolatedModel.setPomFile( model.getPomFile() );

        // restore profiles with file activation to their value before full interpolation
        injectProfileActivations( model, originalActivations );

        return interpolatedModel;
    }

    private ModelData readParent( Model childModel, ModelSource childSource, ModelBuildingRequest request,
                                  DefaultModelProblemCollector problems )
                                      throws ModelBuildingException
    {
        ModelData parentData;

        Parent parent = childModel.getParent();

        if ( parent != null )
        {
            String groupId = parent.getGroupId();
            String artifactId = parent.getArtifactId();
            String version = parent.getVersion();

            parentData = getCache( request.getModelCache(), groupId, artifactId, version, ModelCacheTag.RAW );

            if ( parentData == null )
            {
                parentData = readParentLocally( childModel, childSource, request, problems );

                if ( parentData == null )
                {
                    parentData = readParentExternally( childModel, request, problems );
                }

                putCache( request.getModelCache(), groupId, artifactId, version, ModelCacheTag.RAW, parentData );
            }
            else
            {
                /*
                 * NOTE: This is a sanity check of the cache hit. If the cached parent POM was locally resolved, the
                 * child's <relativePath> should point at that parent, too. If it doesn't, we ignore the cache and
                 * resolve externally, to mimic the behavior if the cache didn't exist in the first place. Otherwise,
                 * the cache would obscure a bad POM.
                 */

                File pomFile = parentData.getModel().getPomFile();
                if ( pomFile != null )
                {
                    ModelSource expectedParentSource = getParentPomFile( childModel, childSource );

                    if ( expectedParentSource instanceof ModelSource2
                        && !pomFile.toURI().equals( ( (ModelSource2) expectedParentSource ).getLocationURI() ) )
                    {
                        parentData = readParentExternally( childModel, request, problems );
                    }
                }
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
        else
        {
            parentData = null;
        }

        return parentData;
    }

    private ModelData readParentLocally( Model childModel, ModelSource childSource, ModelBuildingRequest request,
                                         DefaultModelProblemCollector problems )
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

            File pomFile = null;
            if ( candidateSource instanceof FileModelSource )
            {
                pomFile = ( (FileModelSource) candidateSource ).getPomFile();
            }

            candidateModel = readModel( candidateSource, pomFile, request, problems );
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
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.BASE )
                    .setMessage( e.getMessage().toString() ).setLocation( parent.getLocation( "" ) )
                    .setException( e ) );

                throw problems.newModelBuildingException();
            }
            if ( candidateModel == null )
            {
                return null;
            }
            candidateSource = new FileModelSource( candidateModel.getPomFile() );
        }

        //
        // TODO:jvz Why isn't all this checking the job of the duty of the workspace resolver, we know that we
        // have a model that is suitable, yet more checks are done here and the one for the version is problematic
        // before because with parents as ranges it will never work in this scenario.
        //
        String groupId = candidateModel.getGroupId();
        if ( groupId == null && candidateModel.getParent() != null )
        {
            groupId = candidateModel.getParent().getGroupId();
        }
        String artifactId = candidateModel.getArtifactId();
        String version = candidateModel.getVersion();
        if ( version == null && candidateModel.getParent() != null )
        {
            version = candidateModel.getParent().getVersion();
        }

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
                    problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                        .setMessage( "Version must be a constant" ).setLocation( childModel.getLocation( "" ) ) );

                }
                else
                {
                    if ( childModel.getVersion().contains( "${" ) )
                    {
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
        ModelData parentData = new ModelData( candidateSource, candidateModel, groupId, artifactId, version );

        return parentData;
    }

    private ModelSource getParentPomFile( Model childModel, ModelSource source )
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
                                            DefaultModelProblemCollector problems )
                                                throws ModelBuildingException
    {
        problems.setSource( childModel );

        Parent parent = childModel.getParent().clone();

        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();

        ModelResolver modelResolver = request.getModelResolver();

        Validate.notNull( modelResolver, "request.modelResolver cannot be null (parent POM %s and POM %s)",
            ModelProblemUtils.toId( groupId, artifactId, version ), ModelProblemUtils.toSourceHint( childModel ) );

        ModelSource modelSource;
        try
        {
            modelSource = modelResolver.resolveModel( parent );
        }
        catch ( UnresolvableModelException e )
        {
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

        ModelBuildingRequest lenientRequest = request;
        if ( request.getValidationLevel() > ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            lenientRequest = new FilterModelBuildingRequest( request )
            {

                @Override
                public int getValidationLevel( )
                {
                    return ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;
                }

            };
        }

        Model parentModel = readModel( modelSource, null, lenientRequest, problems );

        if ( !parent.getVersion().equals( version ) )
        {
            if ( childModel.getVersion() == null )
            {
                problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                    .setMessage( "Version must be a constant" ).setLocation( childModel.getLocation( "" ) ) );

            }
            else
            {
                if ( childModel.getVersion().contains( "${" ) )
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.FATAL, Version.V31 )
                        .setMessage( "Version must be a constant" )
                        .setLocation( childModel.getLocation( "version" ) ) );

                }
            }

            // MNG-2199: What else to check here ?
        }

        ModelData parentData =
            new ModelData( modelSource, parentModel, parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );

        return parentData;
    }

    private Model getSuperModel( final String version )
    {
        return superPomProvider.getSuperModel( version ).clone();
    }

    private void importDependencyManagement( Model model, String scope, ModelBuildingRequest request,
                                             DefaultModelProblemCollector problems, Collection<String> importIds )
    {
        DependencyManagement depMngt = model.getDependencyManagement();

        if ( depMngt != null )
        {
            problems.setSource( model );

            String importing = model.getGroupId() + ':' + model.getArtifactId() + ':' + model.getVersion();

            importIds.add( importing );

            final WorkspaceModelResolver workspaceResolver = request.getWorkspaceModelResolver();
            final ModelResolver modelResolver = request.getModelResolver();

            List<DependencyManagement> importMngts = null;

            for ( Iterator<Dependency> it = depMngt.getDependencies().iterator(); it.hasNext(); )
            {
                Dependency dependency = it.next();

                if ( !"pom".equals( dependency.getType() ) || !scope.equals( dependency.getScope() ) )
                {
                    continue;
                }

                it.remove();

                String groupId = dependency.getGroupId();
                String artifactId = dependency.getArtifactId();
                String version = dependency.getVersion();

                if ( groupId == null || groupId.length() <= 0 )
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                        .setMessage( "'dependencyManagement.dependencies.dependency.groupId' for "
                            + dependency.getManagementKey() + " is missing." )
                        .setLocation( dependency.getLocation( "" ) ) );

                    continue;
                }
                if ( artifactId == null || artifactId.length() <= 0 )
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                        .setMessage( "'dependencyManagement.dependencies.dependency.artifactId' for "
                            + dependency.getManagementKey() + " is missing." )
                        .setLocation( dependency.getLocation( "" ) ) );

                    continue;
                }
                if ( version == null || version.length() <= 0 )
                {
                    problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                        .setMessage( "'dependencyManagement.dependencies.dependency.version' for "
                            + dependency.getManagementKey() + " is missing." )
                        .setLocation( dependency.getLocation( "" ) ) );

                    continue;
                }

                String imported = groupId + ':' + artifactId + ':' + version;

                if ( importIds.contains( imported ) )
                {
                    String message = "The dependencies of type=pom and scope=" + scope + " form a cycle: ";
                    for ( String modelId : importIds )
                    {
                        message += modelId + " -> ";
                    }
                    message += imported;
                    problems
                        .add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage( message ) );

                    continue;
                }

                DependencyManagement importMngt =
                    getCache( request.getModelCache(), groupId, artifactId, version, ModelCacheTag.IMPORT );

                if ( importMngt == null )
                {
                    if ( workspaceResolver == null && modelResolver == null )
                    {
                        throw new NullPointerException( String.format(
                            "request.workspaceModelResolver and request.modelResolver cannot be null"
                                + " (parent POM %s and POM %s)",
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

                            continue;
                        }
                    }

                    if ( importModel == null )
                    {
                        // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
                        importModel = this.buildImportModelFromRepository( model, request, dependency, importIds,
                                                                           problems );

                        if ( importModel == null )
                        {
                            continue;
                        }
                    }

                    importMngt = importModel.getDependencyManagement() != null
                                    ? importModel.getDependencyManagement().clone() : new DependencyManagement();

                    if ( ModelVersions.supportsDependencyManagementImportExclusions( model ) )
                    {
                        if ( !dependency.getExclusions().isEmpty() )
                        {
                            for ( final Exclusion exclusion : dependency.getExclusions() )
                            {
                                if ( exclusion.getGroupId() != null && exclusion.getArtifactId() != null )
                                {
                                    for ( final Iterator<Dependency> dependencies =
                                        importMngt.getDependencies().iterator(); dependencies.hasNext(); )
                                    {
                                        final Dependency candidate = dependencies.next();

                                        if ( ( exclusion.getGroupId().equals( "*" )
                                               || exclusion.getGroupId().equals( candidate.getGroupId() ) )
                                                 && ( exclusion.getArtifactId().equals( "*" )
                                                      || exclusion.getArtifactId().
                                                     equals( candidate.getArtifactId() ) ) )
                                        {
                                            // Dependency excluded from import.
                                            dependencies.remove();
                                        }
                                    }
                                }
                            }

                            for ( final Dependency includedDependency : importMngt.getDependencies() )
                            {
                                includedDependency.getExclusions().addAll( dependency.getExclusions() );
                            }
                        }
                        else
                        {
                            // Only dependency managements without exclusion processing applied can be cached.
                            putCache( request.getModelCache(), groupId, artifactId, version, ModelCacheTag.IMPORT,
                                      importMngt );

                        }
                    }
                }

                if ( importMngts == null )
                {
                    importMngts = new ArrayList<>();
                }

                importMngts.add( importMngt );
            }

            importIds.remove( importing );

            dependencyManagementImporter.importManagement( model, importMngts, request, problems );
        }
    }

    private Model buildImportModelFromRepository( final Model model,
                                                  final ModelBuildingRequest targetModelBuildingRequest,
                                                  final Dependency dependency, final Collection<String> importIds,
                                                  final DefaultModelProblemCollector problems )
    {
        try
        {
            final String imported = String.format( "%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(),
                                                   dependency.getVersion() );

            final Dependency resolvedDependency = dependency.clone();

            final ModelSource importSource =
                ModelVersions.supportsDependencyManagementImportVersionRanges( model )
                    ? targetModelBuildingRequest.getModelResolver().resolveModel( resolvedDependency )
                    : targetModelBuildingRequest.getModelResolver().resolveModel(
                        resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(),
                        resolvedDependency.getVersion() );

            final String resolvedId =
                String.format( "%s:%s:%s", resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(),
                               resolvedDependency.getVersion() );

            if ( !imported.equals( resolvedId ) && importIds.contains( resolvedId ) )
            {
                // A version range has been resolved to a cycle.
                String message = "The dependencies of type=pom and scope=" + dependency.getScope() + " form a cycle: ";
                for ( String modelId : importIds )
                {
                    message += modelId + " -> ";
                }
                message += resolvedId;
                problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage( message ) );
            }
            else
            {
                final ModelBuildingRequest importRequest = new DefaultModelBuildingRequest();
                importRequest.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
                importRequest.setModelCache( targetModelBuildingRequest.getModelCache() );
                importRequest.setSystemProperties( targetModelBuildingRequest.getSystemProperties() );
                importRequest.setUserProperties( targetModelBuildingRequest.getUserProperties() );
                importRequest.setLocationTracking( targetModelBuildingRequest.isLocationTracking() );
                importRequest.setModelSource( importSource );
                importRequest.setModelResolver( targetModelBuildingRequest.getModelResolver().newCopy() );

                final ModelBuildingResult importResult = build( importRequest );
                problems.addAll( importResult.getProblems() );

                Model importModel = importResult.getEffectiveModel();

                if ( importModel.getDistributionManagement() != null
                         && importModel.getDistributionManagement().getRelocation() != null
                         && ModelVersions.supportsDependencyManagementImportRelocations( model ) )
                {
                    final Dependency relocated = dependency.clone();
                    relocated.setGroupId( importModel.getDistributionManagement().getRelocation().getGroupId() );
                    relocated.setArtifactId( importModel.getDistributionManagement().getRelocation().getArtifactId() );
                    relocated.setVersion( importModel.getDistributionManagement().getRelocation().getVersion() );

                    String message =
                        String.format( "The dependency of type='%s' and scope='%s' has been relocated to '%s:%s:%s'",
                                       dependency.getType(), dependency.getScope(), relocated.getGroupId(),
                                       relocated.getArtifactId(), relocated.getVersion() );

                    if ( importModel.getDistributionManagement().getRelocation().getMessage() != null )
                    {
                        message += ". " + importModel.getDistributionManagement().getRelocation().getMessage();
                    }

                    problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.BASE )
                        .setMessage( message )
                        .setLocation( importModel.getDistributionManagement().getRelocation().getLocation( "" ) ) );

                    importModel = this.buildImportModelFromRepository( model, targetModelBuildingRequest, relocated,
                                                                       importIds, problems );

                }

                return importModel;
            }
        }
        catch ( final UnresolvableModelException e )
        {
            final StringBuilder buffer = new StringBuilder( 256 );
            buffer.append( "Non-resolvable " + dependency.getScope() + " POM" );

            if ( !containsCoordinates( e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(),
                                       dependency.getVersion() ) )
            {
                buffer.append( ' ' ).append( ModelProblemUtils.toId( dependency.getGroupId(),
                                                                     dependency.getArtifactId(),
                                                                     dependency.getVersion() ) );

            }

            buffer.append( ": " ).append( e.getMessage() );

            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                .setMessage( buffer.toString() ).setLocation( dependency.getLocation( "" ) ).setException( e ) );

        }
        catch ( final ModelBuildingException e )
        {
            final StringBuilder buffer = new StringBuilder( 256 );
            buffer.append( "Failure building " + dependency.getScope() + " POM" );

            if ( !containsCoordinates( e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(),
                                       dependency.getVersion() ) )
            {
                buffer.append( ' ' ).append( ModelProblemUtils.toId( dependency.getGroupId(),
                                                                     dependency.getArtifactId(),
                                                                     dependency.getVersion() ) );

            }

            buffer.append( ": " ).append( e.getMessage() );

            problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                .setMessage( buffer.toString() ).setLocation( dependency.getLocation( "" ) ).setException( e ) );

            problems.addAll( e.getProblems() );
        }

        return null;
    }

    private <T> void putCache( ModelCache modelCache, String groupId, String artifactId, String version,
                               ModelCacheTag<T> tag, T data )
    {
        if ( modelCache != null )
        {
            modelCache.put( groupId, artifactId, version, tag.getName(), tag.intoCache( data ) );
        }
    }

    private <T> T getCache( ModelCache modelCache, String groupId, String artifactId, String version,
                            ModelCacheTag<T> tag )
    {
        if ( modelCache != null )
        {
            Object data = modelCache.get( groupId, artifactId, version, tag.getName() );
            if ( data != null )
            {
                return tag.fromCache( tag.getType().cast( data ) );
            }
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

}
