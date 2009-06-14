package org.apache.maven.model;

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolationException;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.management.ManagementInjector;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationException;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelectionResult;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.validation.ModelValidationResult;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Benjamin Bentmann
 */
@Component( role = ModelBuilder.class )
public class DefaultModelBuilder
    implements ModelBuilder
{

    private Model superModel;

    @Requirement
    private ModelReader modelReader;

    @Requirement
    private ModelValidator modelValidator;

    @Requirement
    private ModelNormalizer modelNormalizer;

    @Requirement
    private ModelInterpolator modelInterpolator;

    @Requirement
    private ModelPathTranslator modelPathTranslator;

    @Requirement
    private InheritanceAssembler inheritanceAssembler;

    @Requirement
    private ProfileSelector profileSelector;

    @Requirement
    private ProfileInjector profileInjector;

    @Requirement
    private ManagementInjector managementInjector;

    @Requirement
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Requirement
    private PluginConfigurationExpander pluginConfigurationExpander;

    public ModelBuildingResult build( File pomFile, ModelBuildingRequest request )
        throws ModelBuildingException
    {
        return build( new FileModelSource( pomFile ), pomFile, request );
    }

    public ModelBuildingResult build( ModelSource modelSource, ModelBuildingRequest request )
        throws ModelBuildingException
    {
        return build( modelSource, null, request );
    }

    private ModelBuildingResult build( ModelSource modelSource, File pomFile, ModelBuildingRequest request )
        throws ModelBuildingException
    {
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        List<ModelProblem> problems = new ArrayList<ModelProblem>();

        ProfileActivationContext profileActivationContext = getProfileActivationContext( request );

        List<Profile> activeExternalProfiles = getActiveExternalProfiles( request, profileActivationContext, problems );

        result.setActiveExternalProfiles( activeExternalProfiles );

        Model model = readModel( modelSource, pomFile, request, problems );

        List<Model> rawModels = new ArrayList<Model>();
        List<Model> resultModels = new ArrayList<Model>();

        for ( Model current = model; current != null; current = readParent( current, request, problems ) )
        {
            Model resultModel = current;
            resultModels.add( resultModel );

            Model rawModel = ModelUtils.cloneModel( current );
            rawModels.add( rawModel );

            modelNormalizer.mergeDuplicates( resultModel, request );

            List<Profile> activePomProfiles = getActivePomProfiles( rawModel, profileActivationContext, problems );

            result.setActivePomProfiles( rawModel, activePomProfiles );

            for ( Profile activeProfile : activePomProfiles )
            {
                profileInjector.injectProfile( resultModel, activeProfile, request );
            }

            if ( current == model )
            {
                for ( Profile activeProfile : activeExternalProfiles )
                {
                    profileInjector.injectProfile( resultModel, activeProfile, request );
                }
            }

            configureResolver( request.getModelResolver(), resultModel, problems );
        }

        Model superModel = getSuperModel();
        rawModels.add( superModel );
        resultModels.add( superModel );

        result.setRawModels( rawModels );

        assembleInheritance( resultModels, request );

        Model resultModel = resultModels.get( 0 );

        resultModel = interpolateModel( resultModel, request, problems );
        resultModels.set( 0, resultModel );

        modelPathTranslator.alignToBaseDirectory( resultModel, resultModel.getProjectDirectory(), request );

        if ( request.isProcessPlugins() )
        {
            lifecycleBindingsInjector.injectLifecycleBindings( resultModel );
        }

        managementInjector.injectManagement( resultModel, request );

        modelNormalizer.injectDefaultValues( resultModel, request );

        if ( request.isProcessPlugins() )
        {
            pluginConfigurationExpander.expandPluginConfiguration( resultModel, request );
        }

        ModelValidationResult validationResult = modelValidator.validateEffectiveModel( resultModel, request );
        addProblems( resultModel, validationResult, problems );

        if ( !problems.isEmpty() )
        {
            throw new ModelBuildingException( problems );
        }

        result.setEffectiveModel( resultModel );

        return result;
    }

    private ProfileActivationContext getProfileActivationContext( ModelBuildingRequest request )
    {
        ProfileActivationContext context = new DefaultProfileActivationContext();
        context.setActiveProfileIds( request.getActiveProfileIds() );
        context.setInactiveProfileIds( request.getInactiveProfileIds() );
        context.setExecutionProperties( request.getExecutionProperties() );
        return context;
    }

    private Model readModel( ModelSource modelSource, File pomFile, ModelBuildingRequest request,
                             List<ModelProblem> problems )
        throws ModelBuildingException
    {
        Model model;

        try
        {
            Map<String, Object> options =
                Collections.<String, Object> singletonMap( ModelReader.IS_STRICT,
                                                           Boolean.valueOf( !request.istLenientValidation() ) );
            model = modelReader.read( modelSource.getInputStream(), options );
        }
        catch ( ModelParseException e )
        {
            problems.add( new ModelProblem( "Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage(),
                                            modelSource.getLocation(), e ) );
            throw new ModelBuildingException( problems );
        }
        catch ( IOException e )
        {
            problems.add( new ModelProblem( "Non-readable POM " + modelSource.getLocation() + ": " + e.getMessage(),
                                            modelSource.getLocation(), e ) );
            throw new ModelBuildingException( problems );
        }

        model.setPomFile( pomFile );

        ModelValidationResult validationResult = modelValidator.validateRawModel( model, request );
        addProblems( model, validationResult, problems );

        return model;
    }

    private void addProblems( Model model, ModelValidationResult result, List<ModelProblem> problems )
    {
        if ( result.getMessageCount() > 0 )
        {
            String source = toSourceHint( model );

            for ( int i = 0; i < result.getMessageCount(); i++ )
            {
                problems.add( new ModelProblem( "Invalid POM " + source + ": " + result.getMessage( i ), source ) );
            }
        }
    }

    private List<Profile> getActiveExternalProfiles( ModelBuildingRequest request, ProfileActivationContext context,
                                                     List<ModelProblem> problems )
    {
        ProfileSelectionResult result = profileSelector.getActiveProfiles( request.getProfiles(), context );

        for ( ProfileActivationException e : result.getActivationExceptions() )
        {
            problems.add( new ModelProblem( "Invalid activation condition for external profile "
                + e.getProfile().getId() + ": " + e.getMessage(), "(external profiles)", e ) );
        }

        return result.getActiveProfiles();
    }

    private List<Profile> getActivePomProfiles( Model model, ProfileActivationContext context,
                                                List<ModelProblem> problems )
    {
        ProfileSelectionResult result = profileSelector.getActiveProfiles( model.getProfiles(), context );

        for ( ProfileActivationException e : result.getActivationExceptions() )
        {
            problems.add( new ModelProblem( "Invalid activation condition for project profile "
                + e.getProfile().getId() + " in POM " + toSourceHint( model ) + ": " + e.getMessage(),
                                            toSourceHint( model ), e ) );
        }

        return result.getActiveProfiles();
    }

    private void configureResolver( ModelResolver modelResolver, Model model, List<ModelProblem> problems )
    {
        if ( modelResolver == null )
        {
            return;
        }

        for ( Repository repository : model.getRepositories() )
        {
            try
            {
                modelResolver.addRepository( repository );
            }
            catch ( InvalidRepositoryException e )
            {
                problems.add( new ModelProblem( "Invalid repository " + repository.getId() + " in POM "
                    + toSourceHint( model ) + ": " + e.getMessage(), toSourceHint( model ), e ) );
            }
        }
    }

    private void assembleInheritance( List<Model> models, ModelBuildingRequest request )
    {
        for ( int i = models.size() - 2; i >= 0; i-- )
        {
            Model parent = models.get( i + 1 );
            Model child = models.get( i );
            inheritanceAssembler.assembleModelInheritance( child, parent, request );
        }
    }

    private Model interpolateModel( Model model, ModelBuildingRequest request, List<ModelProblem> problems )
    {
        try
        {
            Model result = modelInterpolator.interpolateModel( model, model.getProjectDirectory(), request );
            result.setPomFile( model.getPomFile() );
            return result;
        }
        catch ( ModelInterpolationException e )
        {
            problems.add( new ModelProblem( "Invalid expression in POM " + toSourceHint( model ) + ": "
                + e.getMessage(), toSourceHint( model ), e ) );

            return model;
        }
    }

    private Model readParent( Model childModel, ModelBuildingRequest request, List<ModelProblem> problems )
        throws ModelBuildingException
    {
        Model parentModel;

        Parent parent = childModel.getParent();

        if ( parent != null )
        {
            parentModel = readParentLocally( childModel, request, problems );

            if ( parentModel == null )
            {
                parentModel = readParentExternally( childModel, request, problems );
            }
        }
        else
        {
            parentModel = null;
        }

        return parentModel;
    }

    private Model readParentLocally( Model childModel, ModelBuildingRequest request, List<ModelProblem> problems )
        throws ModelBuildingException
    {
        File projectDirectory = childModel.getProjectDirectory();
        if ( projectDirectory == null )
        {
            return null;
        }

        Parent parent = childModel.getParent();

        File pomFile = new File( new File( projectDirectory, parent.getRelativePath() ).toURI().normalize() );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }
        if ( !pomFile.isFile() )
        {
            return null;
        }

        Model candidateModel = readModel( new FileModelSource( pomFile ), pomFile, request, problems );

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

        if ( groupId == null || !groupId.equals( parent.getGroupId() ) )
        {
            return null;
        }
        if ( artifactId == null || !artifactId.equals( parent.getArtifactId() ) )
        {
            return null;
        }
        if ( version == null || !version.equals( parent.getVersion() ) )
        {
            return null;
        }

        return candidateModel;
    }

    private Model readParentExternally( Model childModel, ModelBuildingRequest request, List<ModelProblem> problems )
        throws ModelBuildingException
    {
        Parent parent = childModel.getParent();

        ModelResolver modelResolver = request.getModelResolver();

        if ( modelResolver == null )
        {
            problems.add( new ModelProblem( "Non-resolvable parent POM " + toId( parent ) + " for POM "
                + toSourceHint( childModel ) + ": " + "No model resolver provided", toSourceHint( childModel ) ) );
            throw new ModelBuildingException( problems );
        }

        ModelSource modelSource;
        try
        {
            modelSource = modelResolver.resolveModel( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
        }
        catch ( UnresolvableModelException e )
        {
            problems.add( new ModelProblem( "Non-resolvable parent POM " + toId( parent ) + " for POM "
                + toSourceHint( childModel ) + ": " + e.getMessage(), toSourceHint( childModel ), e ) );
            throw new ModelBuildingException( problems );
        }

        return readModel( modelSource, null, request, problems );
    }

    private Model getSuperModel()
    {
        if ( superModel == null )
        {
            InputStream is = getClass().getResourceAsStream( "/org/apache/maven/project/pom-4.0.0.xml" );
            try
            {
                superModel = modelReader.read( is, null );
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( "The super POM is damaged"
                    + ", please verify the integrity of your Maven installation", e );
            }
        }

        return ModelUtils.cloneModel( superModel );
    }

    private String toSourceHint( Model model )
    {
        StringBuilder buffer = new StringBuilder( 128 );

        buffer.append( toId( model ) );

        File pomFile = model.getPomFile();
        if ( pomFile != null )
        {
            buffer.append( " (" ).append( pomFile ).append( ")" );
        }

        return buffer.toString();
    }

    private String toId( Model model )
    {
        StringBuilder buffer = new StringBuilder( 64 );

        if ( model.getGroupId() != null )
        {
            buffer.append( model.getGroupId() );
        }
        else if ( model.getParent() != null && model.getParent().getGroupId() != null )
        {
            buffer.append( model.getParent().getGroupId() );
        }
        else
        {
            buffer.append( "[unknown-group-id]" );
        }

        buffer.append( ':' );

        if ( model.getArtifactId() != null )
        {
            buffer.append( model.getArtifactId() );
        }
        else
        {
            buffer.append( "[unknown-artifact-id]" );
        }

        buffer.append( ':' );

        if ( model.getVersion() != null )
        {
            buffer.append( model.getVersion() );
        }
        else if ( model.getParent() != null && model.getParent().getVersion() != null )
        {
            buffer.append( model.getParent().getVersion() );
        }
        else
        {
            buffer.append( "[unknown-version]" );
        }

        return buffer.toString();
    }

    private String toId( Parent parent )
    {
        StringBuilder buffer = new StringBuilder( 64 );

        buffer.append( parent.getGroupId() );
        buffer.append( ':' );
        buffer.append( parent.getArtifactId() );
        buffer.append( ':' );
        buffer.append( parent.getVersion() );

        return buffer.toString();
    }

}
