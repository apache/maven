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

    public ModelBuildingResult build( File pomFile, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException
    {
        return build( new FileModelSource( pomFile ), pomFile, request, modelResolver );
    }

    public ModelBuildingResult build( ModelSource modelSource, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException
    {
        return build( modelSource, null, request, modelResolver );
    }

    private ModelBuildingResult build( ModelSource modelSource, File pomFile, ModelBuildingRequest request,
                                       ModelResolver modelResolver )
        throws ModelBuildingException
    {
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        ProfileActivationContext profileActivationContext = getProfileActivationContext( request );

        List<Profile> activeExternalProfiles = getActiveExternalProfiles( request, profileActivationContext );

        Model model = readModel( modelSource, request );
        model.setPomFile( pomFile );

        List<Model> rawModels = new ArrayList<Model>();
        List<Model> resultModels = new ArrayList<Model>();

        for ( Model current = model; current != null; current = readParent( current, request, modelResolver ) )
        {
            Model resultModel = current;
            resultModels.add( resultModel );

            Model rawModel = ModelUtils.cloneModel( current );
            rawModels.add( rawModel );

            modelNormalizer.mergeDuplicates( resultModel, request );

            List<Profile> activeProjectProfiles = getActiveProjectProfiles( rawModel, profileActivationContext );

            List<Profile> activeProfiles = activeProjectProfiles;
            if ( current == model )
            {
                activeProfiles = new ArrayList<Profile>( activeProjectProfiles.size() + activeExternalProfiles.size() );
                activeProfiles.addAll( activeProjectProfiles );
                activeProfiles.addAll( activeExternalProfiles );
            }

            for ( Profile activeProfile : activeProfiles )
            {
                profileInjector.injectProfile( resultModel, activeProfile, request );
            }

            result.setActiveProfiles( rawModel, activeProfiles );

            configureResolver( modelResolver, resultModel );
        }

        Model superModel = getSuperModel();
        rawModels.add( superModel );
        resultModels.add( superModel );

        result.setRawModels( rawModels );

        assembleInheritance( resultModels, request );

        Model resultModel = resultModels.get( 0 );

        resultModel = interpolateModel( resultModel, request );
        resultModels.set( 0, resultModel );

        modelPathTranslator.alignToBaseDirectory( resultModel, resultModel.getProjectDirectory(), request );

        if ( request.isProcessPlugins() )
        {
            lifecycleBindingsInjector.injectLifecycleBindings( resultModel );
        }

        managementInjector.injectManagement( resultModel, request );

        if ( request.isProcessPlugins() )
        {
            pluginConfigurationExpander.expandPluginConfiguration( resultModel, request );
        }

        validateModel( resultModel, false, request );

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

    private Model readModel( ModelSource modelSource, ModelBuildingRequest request )
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
            throw new UnparseableModelException( "Failed to parse POM " + modelSource.getLocation(), e.getLineNumber(),
                                                 e.getColumnNumber(), e );
        }
        catch ( IOException e )
        {
            throw new UnparseableModelException( "Failed to read POM " + modelSource.getLocation(), -1, -1, e );
        }

        validateModel( model, true, request );

        return model;
    }

    private void validateModel( Model model, boolean raw, ModelBuildingRequest request )
        throws ModelBuildingException
    {
        ModelValidationResult result;

        if ( raw )
        {
            result = modelValidator.validateRawModel( model, request );
        }
        else
        {
            result = modelValidator.validateEffectiveModel( model, request );
        }

        if ( result.getMessageCount() > 0 )
        {
            throw new InvalidModelException( "Failed to validate POM " + toSourceHint( model ), result );
        }
    }

    private List<Profile> getActiveExternalProfiles( ModelBuildingRequest request, ProfileActivationContext context )
        throws ModelBuildingException
    {
        try
        {
            return profileSelector.getActiveProfiles( request.getProfiles(), context );
        }
        catch ( ProfileActivationException e )
        {
            throw new InvalidProfileException( "Failed to determine activation status of external profile "
                + e.getProfile(), e.getProfile(), e );
        }
    }

    private List<Profile> getActiveProjectProfiles( Model model, ProfileActivationContext context )
        throws ModelBuildingException
    {
        try
        {
            return profileSelector.getActiveProfiles( model.getProfiles(), context );
        }
        catch ( ProfileActivationException e )
        {
            throw new InvalidProfileException( "Failed to determine activation status of project profile "
                + e.getProfile() + " for POM " + toSourceHint( model ), e.getProfile(), e );
        }
    }

    private void configureResolver( ModelResolver modelResolver, Model model )
        throws ModelBuildingException
    {
        for ( Repository repository : model.getRepositories() )
        {
            try
            {
                modelResolver.addRepository( repository );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new InvalidModelException( "Failed to validate repository " + repository.getId() + " for POM "
                    + toSourceHint( model ), e );
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

    private Model interpolateModel( Model model, ModelBuildingRequest request )
        throws ModelBuildingException
    {
        try
        {
            Model result = modelInterpolator.interpolateModel( model, model.getProjectDirectory(), request );
            result.setPomFile( model.getPomFile() );
            return result;
        }
        catch ( ModelInterpolationException e )
        {
            throw new ModelBuildingException( "Failed to interpolate model " + toSourceHint( model ), e );
        }
    }

    private Model readParent( Model childModel, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException
    {
        Model parentModel;

        Parent parent = childModel.getParent();

        if ( parent != null )
        {
            parentModel = readParentLocally( childModel, request );

            if ( parentModel == null )
            {
                parentModel = readParentExternally( childModel, request, modelResolver );
            }
        }
        else
        {
            parentModel = null;
        }

        return parentModel;
    }

    private Model readParentLocally( Model childModel, ModelBuildingRequest request )
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

        Model candidateModel = readModel( new FileModelSource( pomFile ), request );
        candidateModel.setPomFile( pomFile );

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

    private Model readParentExternally( Model childModel, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException
    {
        Parent parent = childModel.getParent();

        ModelSource modelSource;
        try
        {
            modelSource = modelResolver.resolveModel( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
        }
        catch ( UnresolvableModelException e )
        {
            throw new UnresolvableParentException( "Failed to resolve parent POM " + toId( parent ) + " for POM "
                + toSourceHint( childModel ), e );
        }

        return readModel( modelSource, request );
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
