package org.apache.maven.project.builder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods for transforming model properties into a domain model for the pom classic format and vice versa.
 */
public final class PomClassicTransformer
    implements ModelTransformer
{

    /**
     * The URIs this tranformer supports
     */
    private static Set<String> uris = new HashSet<String>( Arrays.asList( ProjectUri.Build.Extensions.xUri,
                                                                          ProjectUri.Build.PluginManagement.Plugins.xUri,
                                                                          ProjectUri.Build.PluginManagement.Plugins.Plugin.configuration,
                                                                          ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.xUri,
                                                                          ProjectUri.Build.PluginManagement.Plugins.Plugin.Dependencies.Dependency.Exclusions.xUri,
                                                                          ProjectUri.Build.PluginManagement.Plugins.Plugin.Executions.xUri,

                                                                          ProjectUri.Build.Plugins.xUri,
                                                                          ProjectUri.Build.Plugins.Plugin.configuration,
                                                                          ProjectUri.Build.Plugins.Plugin.Dependencies.xUri,
                                                                          ProjectUri.Build.Plugins.Plugin.Executions.xUri,
                                                                          ProjectUri.Build.Resources.xUri,
                                                                          ProjectUri.Build.Resources.Resource.includes,
                                                                          ProjectUri.Build.Resources.Resource.excludes,
                                                                          ProjectUri.Build.TestResources.xUri,

                                                                          ProjectUri.CiManagement.Notifiers.xUri,

                                                                          ProjectUri.Contributors.xUri,

                                                                          ProjectUri.Dependencies.xUri,
                                                                          ProjectUri.Dependencies.Dependency.Exclusions.xUri,

                                                                          ProjectUri.DependencyManagement.Dependencies.xUri,
                                                                          ProjectUri.DependencyManagement.Dependencies.Dependency.Exclusions.xUri,

                                                                          ProjectUri.Developers.xUri,
                                                                          ProjectUri.Developers.Developer.roles,
                                                                          ProjectUri.Licenses.xUri,
                                                                          ProjectUri.MailingLists.xUri,
                                                                          ProjectUri.Modules.xUri,
                                                                          ProjectUri.PluginRepositories.xUri,

                                                                          ProjectUri.Profiles.xUri,
                                                                          ProjectUri.Profiles.Profile.Build.Plugins.xUri,
                                                                          ProjectUri.Profiles.Profile.Build.Plugins.Plugin.Dependencies.xUri,
                                                                          ProjectUri.Profiles.Profile.Build.Resources.xUri,
                                                                          ProjectUri.Profiles.Profile.Build.TestResources.xUri,
                                                                          ProjectUri.Profiles.Profile.Dependencies.xUri,
                                                                          ProjectUri.Profiles.Profile.Dependencies.Dependency.Exclusions.xUri,
                                                                          ProjectUri.Profiles.Profile.DependencyManagement.Dependencies.xUri,
                                                                          ProjectUri.Profiles.Profile.PluginRepositories.xUri,
                                                                          ProjectUri.Profiles.Profile.Reporting.Plugins.xUri,
                                                                          ProjectUri.Profiles.Profile.Repositories.xUri,

                                                                          ProjectUri.Reporting.Plugins.xUri,
                                                                          ProjectUri.Reporting.Plugins.Plugin.ReportSets.xUri,

                                                                          ProjectUri.Repositories.xUri,

                                                                          "http://apache.org/maven/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/exclusions#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/build/pluginManagement/plugins/plugin/executions#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/build/pluginManagement/plugins#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/build/plugins/plugin/dependencies/dependency/exclusions#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/dependencyManagement/dependencies/dependency/exclusions#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/reporting/plugins/plugin/reportSets#collection",
                                                                          "http://apache.org/maven/project/profiles/profile/build/plugins/plugin/executions#collection" ) );

    private static Map<String, List<ModelProperty>> cache = new HashMap<String, List<ModelProperty>>();

    private Collection<Profile> profiles;

    //private static List<DomainModel> cache = new ArrayList<DomainModel>();

    /**
     * Default constructor
     */
    public PomClassicTransformer( Collection<Profile> profiles )
    {
        this.profiles = profiles;
    }

    /**
     * @see ModelTransformer#transformToDomainModel(java.util.List)
     */
    public DomainModel transformToDomainModel( List<ModelProperty> properties )
        throws IOException
    {
        if ( properties == null )
        {
            throw new IllegalArgumentException( "properties: null" );
        }

        List<ModelProperty> props = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : properties )
        { //TODO: Resolved values
            if ( mp.getResolvedValue() != null && ( mp.getResolvedValue().contains( "=" ) || mp.getResolvedValue().contains( "<" ) ) )
            {
                props.add( new ModelProperty( mp.getUri(), "<![CDATA[" + mp.getResolvedValue() + "]]>" ) );
            }
            else
            {
                props.add( mp );
            }
        }

        //dependency management
        ModelDataSource source = new DefaultModelDataSource();
        source.init( props, Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );

        for ( ModelContainer dependencyContainer : source.queryFor( ProjectUri.Dependencies.Dependency.xUri ) )
        {
            for ( ModelContainer managementContainer : source.queryFor(
                ProjectUri.DependencyManagement.Dependencies.Dependency.xUri ) )
            {
                managementContainer = new ArtifactModelContainerFactory().create(
                    transformDependencyManagement( managementContainer.getProperties() ) );
                ModelContainerAction action = dependencyContainer.containerAction( managementContainer );
                if ( action.equals( ModelContainerAction.JOIN ) || action.equals( ModelContainerAction.DELETE ) )
                {
                    source.join( dependencyContainer, managementContainer );
                }
            }
        }

        for ( ModelContainer dependencyContainer : source.queryFor( ProjectUri.Build.Plugins.Plugin.xUri ) )
        {
            for ( ModelContainer managementContainer : source.queryFor(
                ProjectUri.Build.PluginManagement.Plugins.Plugin.xUri ) )
            {
                managementContainer = new ArtifactModelContainerFactory().create(
                    transformPluginManagement( managementContainer.getProperties() ) );

                //Remove duplicate executions tags
                boolean hasExecutionsTag = false;
                for ( ModelProperty mp : dependencyContainer.getProperties() )
                {
                    if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.xUri ) )
                    {
                        hasExecutionsTag = true;
                        break;
                    }
                }
                List<ModelProperty> pList = new ArrayList<ModelProperty>();
                if ( !hasExecutionsTag )
                {
                    pList = managementContainer.getProperties();
                }
                else
                {
                    for ( ModelProperty mp : managementContainer.getProperties() )
                    {
                        if ( !mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.xUri ) )
                        {
                            pList.add( mp );
                        }
                    }
                }
                managementContainer = new ArtifactModelContainerFactory().create( pList );

                ModelContainerAction action = dependencyContainer.containerAction( managementContainer );
                if ( action.equals( ModelContainerAction.JOIN ) || action.equals( ModelContainerAction.DELETE ) )
                {
                    source.join( dependencyContainer, managementContainer );
                }
            }
        }

        props = source.getModelProperties();

        //Rule: Do not join plugin executions without ids
        Set<ModelProperty> removeProperties = new HashSet<ModelProperty>();
        ModelDataSource dataSource = new DefaultModelDataSource();
        dataSource.init( props, Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );
        List<ModelContainer> containers = dataSource.queryFor( ProjectUri.Build.Plugins.Plugin.xUri );
        for ( ModelContainer pluginContainer : containers )
        {
            ModelDataSource executionSource = new DefaultModelDataSource();
            executionSource.init( pluginContainer.getProperties(),
                                  Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );
            List<ModelContainer> executionContainers =
                executionSource.queryFor( ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri );
            if ( executionContainers.size() < 2 )
            {
                continue;
            }

            boolean hasAtLeastOneWithoutId = true;
            for ( ModelContainer executionContainer : executionContainers )
            {
                if ( hasAtLeastOneWithoutId )
                {
                    hasAtLeastOneWithoutId = hasExecutionId( executionContainer );
                }
                if ( !hasAtLeastOneWithoutId && !hasExecutionId( executionContainer ) &&
                    executionContainers.indexOf( executionContainer ) > 0 )
                {
                    removeProperties.addAll( executionContainer.getProperties() );
                }
            }
        }
        props.removeAll( removeProperties );
        String xml = null;
        try
        {
            xml = ModelMarshaller.unmarshalModelPropertiesToXml( props, ProjectUri.baseUri );
            return new PomClassicDomainModel( new MavenXpp3Reader().read( new StringReader( xml ) ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e + ":\r\n" + xml );
        }
    }

    /**
     * @see ModelTransformer#transformToModelProperties(java.util.List, java.util.List)
     */
    public List<ModelProperty> transformToModelProperties( List<DomainModel> domainModels, 
                                                           List<InterpolatorProperty> interpolatorProperties)
        throws IOException
    {
        if ( domainModels == null || domainModels.isEmpty() )
        {
            throw new IllegalArgumentException( "domainModels: null or empty" );
        }

        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        List<String> projectNames = new ArrayList<String>();
        StringBuffer scmUrl = new StringBuffer();
        StringBuffer scmConnectionUrl = new StringBuffer();
        StringBuffer scmDeveloperUrl = new StringBuffer();

        boolean containsBuildResources = false;
        boolean containsTestResources = false;
        boolean containsPluginRepositories = false;

        for ( DomainModel domainModel : domainModels )
        {
            if ( !( domainModel instanceof PomClassicDomainModel ) )
            {
                throw new IllegalArgumentException( "domainModels: Invalid domain model" );
            }

            PomClassicDomainModel pomDomainModel = (PomClassicDomainModel) domainModel;
            if ( cache.containsKey( pomDomainModel.getId() ) )
            {
                System.out.println( "Found in cache: ID = " + pomDomainModel.getId() );
                modelProperties.addAll( cache.get( pomDomainModel.getId() ) );
                continue;
            }

            List<ModelProperty> tmp = ModelMarshaller.marshallXmlToModelProperties(
                ( (PomClassicDomainModel) domainModel ).getInputStream(), ProjectUri.baseUri, uris );

            List clearedProperties = new ArrayList<ModelProperty>();

            //Missing Version Rule
            if ( getPropertyFor( ProjectUri.version, tmp ) == null )
            {
                ModelProperty parentVersion = getPropertyFor( ProjectUri.Parent.version, tmp );
                if ( parentVersion != null )
                {
                    tmp.add( new ModelProperty( ProjectUri.version, parentVersion.getResolvedValue() ) );
                }
            }

            //Modules Not Inherited Rule
            if ( domainModels.indexOf( domainModel ) != 0 )
            {
                ModelProperty modulesProperty = getPropertyFor( ProjectUri.Modules.xUri, tmp );
                if ( modulesProperty != null )
                {
                    tmp.remove( modulesProperty );
                    tmp.removeAll( getPropertiesFor( ProjectUri.Modules.module, tmp ) );
                }
            }

            //Missing groupId, use parent one Rule
            if ( getPropertyFor( ProjectUri.groupId, tmp ) == null )
            {
                ModelProperty parentGroupId = getPropertyFor( ProjectUri.Parent.groupId, tmp );
                if ( parentGroupId != null )
                {
                    tmp.add( new ModelProperty( ProjectUri.groupId, parentGroupId.getResolvedValue() ) );
                }

            }

            //Not inherited plugin execution rule
            if ( domainModels.indexOf( domainModel ) > 0 )
            {
                List<ModelProperty> removeProperties = new ArrayList<ModelProperty>();
                ModelDataSource source = new DefaultModelDataSource();
                source.init( tmp, Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );
                List<ModelContainer> containers =
                    source.queryFor( ProjectUri.Build.Plugins.Plugin.Executions.Execution.xUri );
                for ( ModelContainer container : containers )
                {
                    for ( ModelProperty mp : container.getProperties() )
                    {
                        if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.Execution.inherited ) &&
                            mp.getResolvedValue() != null && mp.getResolvedValue().equals( "false" ) )
                        {
                            removeProperties.addAll( container.getProperties() );
                            for ( int j = tmp.indexOf( mp ); j >= 0; j-- )
                            {
                                if ( tmp.get( j ).getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.xUri ) )
                                {
                                    removeProperties.add( tmp.get( j ) );
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                tmp.removeAll( removeProperties );
            }
            //Not inherited plugin rule
            if ( domainModels.indexOf( domainModel ) > 0 )
            {
                List<ModelProperty> removeProperties = new ArrayList<ModelProperty>();
                ModelDataSource source = new DefaultModelDataSource();
                source.init( tmp, Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );
                List<ModelContainer> containers = source.queryFor( ProjectUri.Build.Plugins.Plugin.xUri );
                for ( ModelContainer container : containers )
                {
                    for ( ModelProperty mp : container.getProperties() )
                    {
                        if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.inherited ) && mp.getResolvedValue() != null &&
                            mp.getResolvedValue().equals( "false" ) )
                        {
                            removeProperties.addAll( container.getProperties() );
                            for ( int j = tmp.indexOf( mp ); j >= 0; j-- )
                            {
                                if ( tmp.get( j ).getUri().equals( ProjectUri.Build.Plugins.Plugin.xUri ) )
                                {
                                    removeProperties.add( tmp.get( j ) );
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                tmp.removeAll( removeProperties );
            }

            //SCM Rule
            ModelProperty scmUrlProperty = getPropertyFor( ProjectUri.Scm.url, tmp );
            if ( scmUrl.length() == 0 && scmUrlProperty != null )
            {
                scmUrl.append( scmUrlProperty.getResolvedValue() );
                for ( String projectName : projectNames )
                {
                    scmUrl.append( "/" ).append( projectName );
                }
                int index = tmp.indexOf( scmUrlProperty );
                tmp.remove( index );
                tmp.add( index, new ModelProperty( ProjectUri.Scm.url, scmUrl.toString() ) );
            }

            //SCM Connection Rule
            scmUrlProperty = getPropertyFor( ProjectUri.Scm.connection, tmp );
            if ( scmConnectionUrl.length() == 0 && scmUrlProperty != null )
            {
                scmConnectionUrl.append( scmUrlProperty.getResolvedValue() );
                for ( String projectName : projectNames )
                {
                    scmConnectionUrl.append( "/" ).append( projectName );
                }
                int index = tmp.indexOf( scmUrlProperty );
                tmp.remove( index );
                tmp.add( index, new ModelProperty( ProjectUri.Scm.connection, scmConnectionUrl.toString() ) );
            }
            //SCM Developer Rule
            scmUrlProperty = getPropertyFor( ProjectUri.Scm.developerConnection, tmp );
            if ( scmDeveloperUrl.length() == 0 && scmUrlProperty != null )
            {
                scmDeveloperUrl.append( scmUrlProperty.getResolvedValue() );
                for ( String projectName : projectNames )
                {
                    scmDeveloperUrl.append( "/" ).append( projectName );
                }
                int index = tmp.indexOf( scmUrlProperty );
                tmp.remove( index );
                tmp.add( index, new ModelProperty( ProjectUri.Scm.developerConnection, scmDeveloperUrl.toString() ) );
            }

            //Project Name Inheritance Rule
            //Packaging Inheritance Rule
            //Profiles not inherited rule
            for ( ModelProperty mp : tmp )
            {
                String uri = mp.getUri();
                if ( domainModels.indexOf( domainModel ) > 0 && ( uri.equals( ProjectUri.name ) ||
                    uri.equals( ProjectUri.packaging ) || uri.startsWith( ProjectUri.Profiles.xUri ) ) )
                {
                    clearedProperties.add( mp );
                }
            }

            //Remove Plugin Repository Inheritance Rule
            //Build Resources Inheritence Rule
            //Build Test Resources Inheritance Rule
            //Only inherit IF: the above is contained in super pom (domainModels.size() -1) && the child doesn't has it's own respective field
            if ( domainModels.indexOf( domainModel ) == 0 )
            {
                containsBuildResources = hasProjectUri( ProjectUri.Build.Resources.xUri, tmp );
                containsTestResources = hasProjectUri( ProjectUri.Build.TestResources.xUri, tmp );
                containsPluginRepositories = hasProjectUri( ProjectUri.PluginRepositories.xUri, tmp );
            }
            for ( ModelProperty mp : tmp )
            {
                if ( domainModels.indexOf( domainModel ) > 0 )
                {
                    String uri = mp.getUri();
                    boolean isNotSuperPom = domainModels.indexOf( domainModel ) != ( domainModels.size() - 1 );
                    if ( isNotSuperPom )
                    {
                        if ( uri.startsWith( ProjectUri.Build.Resources.xUri ) ||
                            uri.startsWith( ProjectUri.Build.TestResources.xUri ) ||
                            uri.startsWith( ProjectUri.PluginRepositories.xUri ) )
                        {
                            clearedProperties.add( mp );
                        }
                    }
                    else
                    {
                        if ( containsBuildResources && uri.startsWith( ProjectUri.Build.Resources.xUri ) )
                        {
                            clearedProperties.add( mp );
                        }
                        else if ( containsTestResources && uri.startsWith( ProjectUri.Build.TestResources.xUri ) )
                        {
                            clearedProperties.add( mp );
                        }
                        else if ( containsPluginRepositories && uri.startsWith( ProjectUri.PluginRepositories.xUri ) )
                        {
                            clearedProperties.add( mp );
                        }
                    }
                }
            }

            ModelProperty artifactId = getPropertyFor( ProjectUri.artifactId, tmp );
            if ( artifactId != null )
            {
                projectNames.add( 0, artifactId.getResolvedValue() );
            }

            tmp.removeAll( clearedProperties );
            modelProperties.addAll( tmp );
            modelProperties.removeAll( clearedProperties );
        }

        interpolateModelProperties( modelProperties, interpolatorProperties, ((PomClassicDomainModel) domainModels.get(0)) );
        return modelProperties;
    }

    public static String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
            throws IOException
    {
        List<ModelProperty> modelProperties =
            ModelMarshaller.marshallXmlToModelProperties( new ByteArrayInputStream(xml.getBytes()), ProjectUri.baseUri, uris );

        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put( "project.", "pom.");

        List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>(interpolatorProperties);
        ips.addAll(ModelTransformerContext.createInterpolatorProperties(modelProperties, ProjectUri.baseUri, aliases,
                        false, false));

        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().startsWith(ProjectUri.properties) && mp.getValue() != null )
            {
                String uri = mp.getUri();
                ips.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1,
                        uri.length() ) + "}", mp.getValue() ) );
            }
        }

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips );
        return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
    }

    public static String interpolateModelAsString(Model model, List<InterpolatorProperty> interpolatorProperties, File projectDirectory)
            throws IOException
    {
        PomClassicDomainModel domainModel = new PomClassicDomainModel( model );
        domainModel.setProjectDirectory( projectDirectory );
        List<ModelProperty> modelProperties =
                ModelMarshaller.marshallXmlToModelProperties( domainModel.getInputStream(), ProjectUri.baseUri, uris );
        interpolateModelProperties( modelProperties, interpolatorProperties, domainModel);

        return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
    }

    public static Model interpolateModel(Model model, List<InterpolatorProperty> interpolatorProperties, File projectDirectory)
        throws IOException
    {
        String pomXml = interpolateModelAsString( model, interpolatorProperties, projectDirectory );
        PomClassicDomainModel domainModel = new PomClassicDomainModel( new ByteArrayInputStream( pomXml.getBytes() ));
        return domainModel.getModel();
    }

    private static void interpolateModelProperties(List<ModelProperty> modelProperties,
                                                   List<InterpolatorProperty> interpolatorProperties,
                                                   PomClassicDomainModel domainModel)
    {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put( "project.", "pom.");

        List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>(interpolatorProperties);
        ips.addAll(ModelTransformerContext.createInterpolatorProperties(modelProperties, ProjectUri.baseUri, aliases,
                        false, false));

        if(domainModel.isPomInBuild())
        {
            ips.add(new InterpolatorProperty("${project.basedir}", domainModel.getProjectDirectory().getAbsolutePath()));
            ips.add(new InterpolatorProperty("${basedir}", domainModel.getProjectDirectory().getAbsolutePath()));
            ips.add(new InterpolatorProperty("${pom.basedir}", domainModel.getProjectDirectory().getAbsolutePath()));

        }

        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().startsWith(ProjectUri.properties) && mp.getValue() != null )
            {
                String uri = mp.getUri();
                ips.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1,
                        uri.length() ) + "}", mp.getValue() ) );
            }
        }

        ModelTransformerContext.interpolateModelProperties( modelProperties, ips );
    }

    private static boolean hasExecutionId( ModelContainer executionContainer )
    {
        for ( ModelProperty mp : executionContainer.getProperties() )
        {
            if ( mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.Executions.Execution.id ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the base uri of all model properties: http://apache.org/maven/project/
     *
     * @return Returns the base uri of all model properties: http://apache.org/maven/project/
     */
    public String getBaseUri()
    {
        return ProjectUri.baseUri;
    }

    private static boolean hasProjectUri( String projectUri, List<ModelProperty> modelProperties )
    {
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().equals( projectUri ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all model properties containing the specified uri from the specified properties list.
     *
     * @param uri        the uri to use in finding the returned model properties
     * @param properties the model properties list to search
     * @return all model properties containing the specified uri from the specified properties list
     */
    private static List<ModelProperty> getPropertiesFor( String uri, List<ModelProperty> properties )
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : properties )
        {
            if ( uri.equals( mp.getUri() ) )
            {
                modelProperties.add( mp );
            }
        }
        return modelProperties;
    }


    /**
     * Returns the first model property containing the specified uri from the specified properties list.
     *
     * @param uri        the uri to use in finding the returned model property
     * @param properties the model properties list to search
     * @return the first model property containing the specified uri from the specified properties list.
     */
    private static ModelProperty getPropertyFor( String uri, List<ModelProperty> properties )
    {
        for ( ModelProperty mp : properties )
        {
            if ( uri.equals( mp.getUri() ) )
            {
                return mp;
            }
        }
        return null;
    }

    private static List<ModelProperty> transformDependencyManagement( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> transformedProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.DependencyManagement.xUri ) )
            {
                transformedProperties.add( new ModelProperty(
                    mp.getUri().replace( ProjectUri.DependencyManagement.xUri, ProjectUri.xUri ), mp.getResolvedValue() ) );
            }
        }
        return transformedProperties;
    }

    public static List<ModelProperty> transformPluginManagement( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> transformedProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.Build.PluginManagement.xUri ) )
            {
                transformedProperties.add( new ModelProperty(
                    mp.getUri().replace( ProjectUri.Build.PluginManagement.xUri, ProjectUri.Build.xUri ),
                    mp.getResolvedValue() ) );
            }
        }
        return transformedProperties;
    }
}

