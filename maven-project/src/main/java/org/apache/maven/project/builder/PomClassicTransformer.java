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
import org.apache.maven.shared.model.*;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Provides methods for transforming model properties into a domain model for the pom classic format and vice versa.
 */
public final class PomClassicTransformer
    implements ModelTransformer
{

    private final DomainModelFactory factory;

    public PomClassicTransformer(DomainModelFactory factory)
    {
        this.factory = factory;
    }

    private static Map<String, List<ModelProperty>> cache = new HashMap<String, List<ModelProperty>>();

    /**
     * @see ModelTransformer#transformToDomainModel(java.util.List, java.util.List)
     */
    public DomainModel transformToDomainModel( List<ModelProperty> properties, List<ModelEventListener> eventListeners)
        throws IOException
    {
        if ( properties == null )
        {
            throw new IllegalArgumentException( "properties: null" );
        }

        if( eventListeners == null )
        {
            eventListeners = new ArrayList<ModelEventListener>();
        }
        else
        {
            eventListeners = new ArrayList<ModelEventListener>(eventListeners);
        }

        List<ModelProperty> props = new ArrayList<ModelProperty>( properties );

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
                   // ModelContainer reverseSortedContainer = new ArtifactModelContainerFactory().create(
                   //     ModelTransformerContext.sort(dependencyContainer.getProperties(), ProjectUri.Build.Plugins.Plugin.xUri) );
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
                                  Arrays.asList( new ArtifactModelContainerFactory(), new PluginExecutionIdModelContainerFactory() ) );
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

        for(ModelEventListener listener : eventListeners)
        {
            ModelDataSource ds = new DefaultModelDataSource();
            ds.init( props, listener.getModelContainerFactories() );
            for(String uri : listener.getUris() )
            {
                listener.fire(ds.queryFor(uri));
            }
        }

        return factory.createDomainModel( props );
    }

    /**
     * @see ModelTransformer#transformToModelProperties(java.util.List)
     */
    public List<ModelProperty> transformToModelProperties(List<DomainModel> domainModels
    )
        throws IOException
    {
        if ( domainModels == null || domainModels.isEmpty() )
        {
            throw new IllegalArgumentException( "domainModels: null or empty" );
        }

        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        List<String> projectNames = new ArrayList<String>();
        StringBuilder projectUrl = new StringBuilder( 128 );
        StringBuilder siteUrl = new StringBuilder( 128 );
        StringBuilder scmUrl = new StringBuilder( 128 );
        StringBuilder scmConnectionUrl = new StringBuilder( 128 );
        StringBuilder scmDeveloperUrl = new StringBuilder( 128 );

        boolean containsBuildResources = false;
        boolean containsTestResources = false;
        boolean containsPluginRepositories = false;
        boolean containsLicenses = false;
        boolean containsDevelopers = false;
        boolean containsContributors = false;
        boolean containsMailingLists = false;
        boolean containsOrganization = false;
        boolean containsIssueManagement = false;
        boolean containsCiManagement = false;
        boolean containsDistRepo = false;
        boolean containsDistSnapRepo = false;
        boolean containsDistSite = false;

        int domainModelIndex = -1;

        for ( DomainModel domainModel : domainModels )
        {
            domainModelIndex++;

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
                ( (PomClassicDomainModel) domainModel ).getInputStream(), ProjectUri.baseUri, PomTransformer.URIS );

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
            if ( domainModelIndex > 0 )
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
            if ( domainModelIndex > 0 )
            {
                List<ModelProperty> removeProperties = new ArrayList<ModelProperty>();
                ModelDataSource source = new DefaultModelDataSource();
                source.init( tmp, Arrays.asList( new ArtifactModelContainerFactory(), new PluginExecutionIdModelContainerFactory() ) );
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
            if ( domainModelIndex > 0 )
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

            // Project URL Rule
            adjustUrl( projectUrl, tmp, ProjectUri.url, projectNames );
            // Site Rule
            adjustUrl( siteUrl, tmp, ProjectUri.DistributionManagement.Site.url, projectNames );
            // SCM Rule
            adjustUrl( scmUrl, tmp, ProjectUri.Scm.url, projectNames );
            // SCM Connection Rule
            adjustUrl( scmConnectionUrl, tmp, ProjectUri.Scm.connection, projectNames );
            // SCM Developer Rule
            adjustUrl( scmDeveloperUrl, tmp, ProjectUri.Scm.developerConnection, projectNames );

            // Project Name Rule: not inherited
            // Packaging Rule: not inherited
            // Profiles Rule: not inherited
            // Parent.relativePath Rule: not inherited
            // Prerequisites Rule: not inherited
            // DistributionManagent.Relocation Rule: not inherited
            if ( domainModelIndex > 0 )
            {
                for ( ModelProperty mp : tmp )
                {
                    String uri = mp.getUri();
                    if ( uri.equals( ProjectUri.name ) || uri.equals( ProjectUri.packaging )
                        || uri.startsWith( ProjectUri.Profiles.xUri )
                        || uri.startsWith( ProjectUri.Parent.relativePath )
                        || uri.startsWith( ProjectUri.Prerequisites.xUri )
                        || uri.startsWith( ProjectUri.DistributionManagement.Relocation.xUri ) )
                    {
                        clearedProperties.add( mp );
                    }
                }
            }

            // Remove Plugin Repository Inheritance Rule
            // License Rule: only inherited if not specified in child
            // Organization Rule: only inherited if not specified in child
            // Developers Rule: only inherited if not specified in child
            // Contributors Rule: only inherited if not specified in child
            // Mailing Lists Rule: only inherited if not specified in child
            // Build Resources Rule: only inherited if not specified in child
            // Build Test Resources Rule: only inherited if not specified in child
            // CI Management Rule: only inherited if not specified in child
            // Issue Management Rule: only inherited if not specified in child
            // Distribution Management Repository Rule: only inherited if not specified in child
            // Distribution Management Snapshot Repository Rule: only inherited if not specified in child
            // Distribution Management Site Rule: only inherited if not specified in child
            for ( ModelProperty mp : tmp )
            {
                String uri = mp.getUri();
                if ( ( containsBuildResources && uri.startsWith( ProjectUri.Build.Resources.xUri ) )
                    || ( containsTestResources && uri.startsWith( ProjectUri.Build.TestResources.xUri ) )
                    || ( containsPluginRepositories && uri.startsWith( ProjectUri.PluginRepositories.xUri ) )
                    || ( containsOrganization && uri.startsWith( ProjectUri.Organization.xUri ) )
                    || ( containsLicenses && uri.startsWith( ProjectUri.Licenses.xUri ) )
                    || ( containsDevelopers && uri.startsWith( ProjectUri.Developers.xUri ) )
                    || ( containsContributors && uri.startsWith( ProjectUri.Contributors.xUri ) )
                    || ( containsMailingLists && uri.startsWith( ProjectUri.MailingLists.xUri ) ) 
                    || ( containsCiManagement && uri.startsWith( ProjectUri.CiManagement.xUri ) )
                    || ( containsIssueManagement && uri.startsWith( ProjectUri.IssueManagement.xUri ) )
                    || ( containsDistRepo && uri.startsWith( ProjectUri.DistributionManagement.Repository.xUri ) )
                    || ( containsDistSnapRepo && uri.startsWith( ProjectUri.DistributionManagement.SnapshotRepository.xUri ) )
                    || ( containsDistSite && uri.startsWith( ProjectUri.DistributionManagement.Site.xUri ) ) )
                {
                    clearedProperties.add( mp );
                }
            }
            containsBuildResources |= hasProjectUri( ProjectUri.Build.Resources.xUri, tmp );
            containsTestResources |= hasProjectUri( ProjectUri.Build.TestResources.xUri, tmp );
            containsPluginRepositories |= hasProjectUri( ProjectUri.PluginRepositories.xUri, tmp );
            containsOrganization |= hasProjectUri( ProjectUri.Organization.xUri, tmp );
            containsLicenses |= hasProjectUri( ProjectUri.Licenses.xUri, tmp );
            containsDevelopers |= hasProjectUri( ProjectUri.Developers.xUri, tmp );
            containsContributors |= hasProjectUri( ProjectUri.Contributors.xUri, tmp );
            containsMailingLists |= hasProjectUri( ProjectUri.MailingLists.xUri, tmp );
            containsCiManagement |= hasProjectUri( ProjectUri.CiManagement.xUri, tmp );
            containsIssueManagement |= hasProjectUri( ProjectUri.IssueManagement.xUri, tmp );
            containsDistRepo |= hasProjectUri( ProjectUri.DistributionManagement.Repository.xUri, tmp );
            containsDistSnapRepo |= hasProjectUri( ProjectUri.DistributionManagement.SnapshotRepository.xUri, tmp );
            containsDistSite |= hasProjectUri( ProjectUri.DistributionManagement.Site.xUri, tmp );

            ModelProperty artifactId = getPropertyFor( ProjectUri.artifactId, tmp );
            if ( artifactId != null )
            {
                projectNames.add( 0, artifactId.getResolvedValue() );
            }

            tmp.removeAll( clearedProperties );
            modelProperties.addAll( tmp );
            modelProperties.removeAll( clearedProperties );
        }

        //Rule: Build plugin config overrides reporting plugin config
        ModelDataSource source = new DefaultModelDataSource();
        source.init( modelProperties, Arrays.asList( new ArtifactModelContainerFactory(), new IdModelContainerFactory() ) );

        List<ModelContainer> reportContainers = source.queryFor( ProjectUri.Reporting.Plugins.Plugin.xUri );
        for ( ModelContainer pluginContainer : source.queryFor( ProjectUri.Build.Plugins.Plugin.xUri ) )
        {
            ModelContainer transformedReportContainer = new ArtifactModelContainerFactory().create(
                    transformPlugin( pluginContainer.getProperties() ) );

            for(ModelContainer reportContainer : reportContainers) {
                ModelContainerAction action = transformedReportContainer.containerAction( reportContainer );
                if ( action.equals( ModelContainerAction.JOIN ) )
                {
                    source.join( transformedReportContainer, reportContainer );
                    break;
                }
            }
        }

        modelProperties = source.getModelProperties();
        return modelProperties;
    }

    /**
     * Adjusts an inherited URL to compensate for a child's relation/distance to the parent that defines the URL.
     * 
     * @param url The buffer for the adjusted URL, must not be {@code null}.
     * @param properties The model properties to update, must not be {@code null}.
     * @param uri The URI of the model property defining the URL to adjust, must not be {@code null}.
     * @param ids The artifact identifiers of the parent projects, starting with the least significant parent, must not
     *            be {@code null}.
     */
    private void adjustUrl( StringBuilder url, List<ModelProperty> properties, String uri, List<String> ids )
    {
        if ( url.length() == 0 )
        {
            ModelProperty property = getPropertyFor( uri, properties );
            if ( property != null )
            {
                url.append( property.getResolvedValue() );
                for ( String id : ids )
                {
                    if ( url.length() > 0 && url.charAt( url.length() - 1 ) != '/' )
                    {
                        url.append( '/' );
                    }
                    url.append( id );
                }
                int index = properties.indexOf( property );
                properties.set( index, new ModelProperty( uri, url.toString() ) );
            }
        }
    }

    public void interpolateModelProperties(List<ModelProperty> modelProperties,
                                           List<InterpolatorProperty> interpolatorProperties,
                                           DomainModel domainModel)
            throws IOException
    {
        ProjectBuilder.Interpolator.interpolateModelProperties( modelProperties, interpolatorProperties, (PomClassicDomainModel) domainModel);
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

    public List<ModelProperty> preprocessModelProperties(List<ModelProperty> modelProperties)
    {
        return new ArrayList<ModelProperty>(modelProperties);        
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

    private static List<ModelProperty> transformPluginManagement( List<ModelProperty> modelProperties )
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

    private static List<ModelProperty> transformPlugin( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> transformedProperties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.Build.Plugins.xUri ) )
            {   if(mp.getUri().startsWith(ProjectUri.Build.Plugins.Plugin.configuration)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.groupId)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.artifactId)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.version)
                    || mp.getUri().equals( ProjectUri.Build.Plugins.Plugin.xUri ) )
                {
                transformedProperties.add( new ModelProperty(
                    mp.getUri().replace( ProjectUri.Build.Plugins.xUri, ProjectUri.Reporting.Plugins.xUri ),
                    mp.getResolvedValue() ) );
                }

            }
        }
        return transformedProperties;
    }
}

