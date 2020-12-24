package org.apache.maven.model.interpolation;

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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * StringVisitorModelInterpolator
 *
 * @since 3.6.2
 */
@Named
@Singleton
public class StringVisitorModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    interface InnerInterpolator
    {
        String interpolate( String value );
    }

    @Override
    public Model interpolateModel( Model model, File projectDir, ModelBuildingRequest config,
                                   ModelProblemCollector problems )
    {
        List<? extends ValueSource> valueSources = createValueSources( model, projectDir, config, problems );
        List<? extends InterpolationPostProcessor> postProcessors =
            createPostProcessors( model, projectDir, config );

        InnerInterpolator innerInterpolator = createInterpolator( valueSources, postProcessors, problems );

        new ModelVisitor( innerInterpolator ).visit( model );

        return model;
    }

    private InnerInterpolator createInterpolator( List<? extends ValueSource> valueSources,
                                                  List<? extends InterpolationPostProcessor> postProcessors,
                                                  final ModelProblemCollector problems )
    {
        final Map<String, String> cache = new HashMap<>();
        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );
        for ( ValueSource vs : valueSources )
        {
            interpolator.addValueSource( vs );
        }
        for ( InterpolationPostProcessor postProcessor : postProcessors )
        {
            interpolator.addPostProcessor( postProcessor );
        }
        final RecursionInterceptor recursionInterceptor = createRecursionInterceptor();
        return value ->
        {
            if ( value != null && value.contains( "${" ) )
            {
                String c = cache.get( value );
                if ( c == null )
                {
                    try
                    {
                        c = interpolator.interpolate( value, recursionInterceptor );
                    }
                    catch ( InterpolationException e )
                    {
                        problems.add( new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE )
                                .setMessage( e.getMessage() ).setException( e ) );
                    }
                    cache.put( value, c );
                }
                return c;
            }
            return value;
        };
    }

    @SuppressWarnings( "StringEquality" )
    private static final class ModelVisitor
    {
        private final InnerInterpolator interpolator;

        ModelVisitor( InnerInterpolator interpolator )
        {
            this.interpolator = interpolator;
        }

        void visit( Model model )
        {
            if ( model != null )
            {
                visit( (ModelBase) model );
                // ModelVersion
                String orgModelVersion = model.getModelVersion();
                String intModelVersion = interpolate( orgModelVersion );
                if ( orgModelVersion != intModelVersion )
                {
                    model.setModelVersion( intModelVersion );
                }
                visit( model.getParent() );
                // GroupId
                String orgGroupId = model.getGroupId();
                String intGroupId = interpolate( orgGroupId );
                if ( orgGroupId != intGroupId )
                {
                    model.setGroupId( intGroupId );
                }
                // ArtifactId
                String orgArtifactId = model.getArtifactId();
                String intArtifactId = interpolate( orgArtifactId );
                if ( orgArtifactId != intArtifactId )
                {
                    model.setArtifactId( intArtifactId );
                }
                // Version
                String orgVersion = model.getVersion();
                String intVersion = interpolate( orgVersion );
                if ( orgVersion != intVersion )
                {
                    model.setVersion( intVersion );
                }

                // Packaging
                String orgPackaging = model.getPackaging();
                String intPackaging = interpolate( orgPackaging );
                if ( orgPackaging != intPackaging )
                {
                    model.setPackaging( intPackaging );
                }
                // Name
                String orgName = model.getName();
                String intName = interpolate( orgName );
                if ( orgName != intName )
                {
                    model.setName( intName );
                }
                // Description
                String orgDescription = model.getDescription();
                String intDescription = interpolate( orgDescription );
                if ( orgDescription != intDescription )
                {
                    model.setDescription( intDescription );
                }
                // Url
                String orgUrl = model.getUrl();
                String intUrl = interpolate( orgUrl );
                if ( orgUrl != intUrl )
                {
                    model.setUrl( intUrl );
                }
                // ChildProjectUrlInheritAppendPath
                String orgChildProjectUrlInheritAppendPath = model.getChildProjectUrlInheritAppendPath();
                String intChildProjectUrlInheritAppendPath = interpolate( orgChildProjectUrlInheritAppendPath );
                if ( orgChildProjectUrlInheritAppendPath != intChildProjectUrlInheritAppendPath )
                {
                    model.setChildProjectUrlInheritAppendPath( intChildProjectUrlInheritAppendPath );
                }
                // InceptionYear
                String orgInceptionYear = model.getInceptionYear();
                String intInceptionYear = interpolate( orgInceptionYear );
                if ( orgInceptionYear != intInceptionYear )
                {
                    model.setInceptionYear( intInceptionYear );
                }
                visit( model.getOrganization() );
                for ( License license : model.getLicenses() )
                {
                    visit( license );
                }
                for ( Developer developer : model.getDevelopers() )
                {
                    visit( developer );
                }
                for ( Contributor contributor : model.getContributors() )
                {
                    visit( contributor );
                }
                for ( MailingList mailingList : model.getMailingLists() )
                {
                    visit( mailingList );
                }
                visit( model.getPrerequisites() );
                visit( model.getScm() );
                visit( model.getIssueManagement() );
                visit( model.getCiManagement() );
                visit( model.getBuild() );
                for ( Profile profile : model.getProfiles() )
                {
                    visit( profile );
                }

            }
        }

        private void visit( Parent parent )
        {
            if ( parent != null )
            {
                String org, val;
                // GroupId
                org = parent.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    parent.setGroupId( val );
                }
                // ArtifactId
                org = parent.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    parent.setArtifactId( val );
                }
                // Version
                org = parent.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    parent.setVersion( val );
                }
                // RelativePath
                org = parent.getRelativePath();
                val = interpolate( org );
                if ( org != val )
                {
                    parent.setRelativePath( val );
                }
            }
        }

        private void visit( Organization organization )
        {
            if ( organization != null )
            {
                String org, val;
                // Name
                org = organization.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    organization.setName( val );
                }
                // Url
                org = organization.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    organization.setUrl( val );
                }
            }
        }

        private void visit( License license )
        {
            if ( license != null )
            {
                String org, val;
                // Name
                org = license.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    license.setName( val );
                }
                // Url
                org = license.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    license.setUrl( val );
                }
                // Distribution
                org = license.getDistribution();
                val = interpolate( org );
                if ( org != val )
                {
                    license.setDistribution( val );
                }
                // Comments
                org = license.getComments();
                val = interpolate( org );
                if ( org != val )
                {
                    license.setComments( val );
                }
            }
        }

        private void visit( Developer developer )
        {
            if ( developer != null )
            {
                String org, val;
                // Contributor
                visit( (Contributor) developer );
                // Distribution
                org = developer.getId();
                val = interpolate( org );
                if ( org != val )
                {
                    developer.setId( val );
                }
            }
        }

        private void visit( Contributor contributor )
        {
            if ( contributor != null )
            {
                String org, val;
                // Name
                org = contributor.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    contributor.setName( val );
                }
                // Email
                org = contributor.getEmail();
                val = interpolate( org );
                if ( org != val )
                {
                    contributor.setEmail( val );
                }
                // Url
                org = contributor.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    contributor.setUrl( val );
                }
                // Organization
                org = contributor.getOrganization();
                val = interpolate( org );
                if ( org != val )
                {
                    contributor.setOrganization( val );
                }
                // OrganizationUrl
                org = contributor.getOrganizationUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    contributor.setOrganizationUrl( val );
                }
                // Roles
                visit( contributor.getRoles() );
            }
        }

        private void visit( MailingList mailingList )
        {
            if ( mailingList != null )
            {
                String org, val;
                // Name
                org = mailingList.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    mailingList.setName( val );
                }
                // Subscribe
                org = mailingList.getSubscribe();
                val = interpolate( org );
                if ( org != val )
                {
                    mailingList.setSubscribe( val );
                }
                // Unsubscribe
                org = mailingList.getUnsubscribe();
                val = interpolate( org );
                if ( org != val )
                {
                    mailingList.setUnsubscribe( val );
                }
                // Post
                org = mailingList.getPost();
                val = interpolate( org );
                if ( org != val )
                {
                    mailingList.setPost( val );
                }
                // Archive
                org = mailingList.getArchive();
                val = interpolate( org );
                if ( org != val )
                {
                    mailingList.setArchive( val );
                }
            }
        }

        private void visit( Prerequisites prerequisites )
        {
            if ( prerequisites != null )
            {
                String org, val;
                // Maven
                org = prerequisites.getMaven();
                val = interpolate( org );
                if ( org != val )
                {
                    prerequisites.setMaven( val );
                }
            }
        }

        private void visit( Scm scm )
        {
            if ( scm != null )
            {
                String org, val;
                // Connection
                org = scm.getConnection();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setConnection( val );
                }
                // DeveloperConnection
                org = scm.getDeveloperConnection();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setDeveloperConnection( val );
                }
                // Tag
                org = scm.getTag();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setTag( val );
                }
                // Url
                org = scm.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setUrl( val );
                }
                // ChildScmConnectionInheritAppendPath
                org = scm.getChildScmConnectionInheritAppendPath();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setChildScmConnectionInheritAppendPath( val );
                }
                // ChildScmDeveloperConnectionInheritAppendPath
                org = scm.getChildScmDeveloperConnectionInheritAppendPath();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setChildScmDeveloperConnectionInheritAppendPath( val );
                }
                // ChildScmUrlInheritAppendPath
                org = scm.getChildScmUrlInheritAppendPath();
                val = interpolate( org );
                if ( org != val )
                {
                    scm.setChildScmUrlInheritAppendPath( val );
                }
            }
        }

        private void visit( IssueManagement issueManagement )
        {
            if ( issueManagement != null )
            {
                String org, val;
                // System
                org = issueManagement.getSystem();
                val = interpolate( org );
                if ( org != val )
                {
                    issueManagement.setSystem( val );
                }
                // Url
                org = issueManagement.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    issueManagement.setUrl( val );
                }
            }
        }

        private void visit( CiManagement ciManagement )
        {
            if ( ciManagement != null )
            {
                String org, val;
                // System
                org = ciManagement.getSystem();
                val = interpolate( org );
                if ( org != val )
                {
                    ciManagement.setSystem( val );
                }
                // Url
                org = ciManagement.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    ciManagement.setUrl( val );
                }
                // Notifiers
                for ( Notifier notifier : ciManagement.getNotifiers() )
                {
                    visit( notifier );
                }
            }
        }

        private void visit( Notifier notifier )
        {
            if ( notifier != null )
            {
                String org, val;
                // Type
                org = notifier.getType();
                val = interpolate( org );
                if ( org != val )
                {
                    notifier.setType( val );
                }
                // Configuration
                visit( notifier.getConfiguration() );
            }
        }

        private void visit( BuildBase build )
        {
            if ( build != null )
            {
                String org, val;
                // Plugins
                for ( Plugin plugin : build.getPlugins() )
                {
                    visit( plugin );
                }
                // PluginManagement
                visit( build.getPluginManagement() );
                // DefaultGoal
                org = build.getDefaultGoal();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setDefaultGoal( val );
                }
                // Resources
                for ( Resource resource : build.getResources() )
                {
                    visit( resource );
                }
                // TestResources
                for ( Resource resource : build.getTestResources() )
                {
                    visit( resource );
                }
                // Directory
                org = build.getDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setDirectory( val );
                }
                // FinalName
                org = build.getFinalName();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setFinalName( val );
                }
                // Filters
                visit( build.getFilters() );
            }
        }

        private void visit( PluginManagement pluginManagement )
        {
            if ( pluginManagement != null )
            {
                for ( Plugin plugin : pluginManagement.getPlugins() )
                {
                    visit( plugin );
                }
            }
        }

        private void visit( Build build )
        {
            if ( build != null )
            {
                String org, val;
                // BuildBase
                visit( (BuildBase) build );
                // SourceDirectory
                org = build.getSourceDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setSourceDirectory( val );
                }
                // ScriptSourceDirectory
                org = build.getScriptSourceDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setScriptSourceDirectory( val );
                }
                // TestSourceDirectory
                org = build.getTestSourceDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setTestSourceDirectory( val );
                }
                // OutputDirectory
                org = build.getOutputDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setOutputDirectory( val );
                }
                // TestOutputDirectory
                org = build.getTestOutputDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    build.setTestOutputDirectory( val );
                }
                // Extensions
                for ( Extension extension : build.getExtensions() )
                {
                    visit( extension );
                }
            }
        }

        private void visit( Resource resource )
        {
            if ( resource != null )
            {
                String org, val;
                // Includes
                visit( resource.getIncludes() );
                // Excludes
                visit( resource.getExcludes() );
                // Directory
                org = resource.getDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    resource.setDirectory( val );
                }
                // TargetPath
                org = resource.getTargetPath();
                val = interpolate( org );
                if ( org != val )
                {
                    resource.setTargetPath( val );
                }
                // Filtering
                org = resource.getFiltering();
                val = interpolate( org );
                if ( org != val )
                {
                    resource.setFiltering( val );
                }
            }
        }

        private void visit( Plugin plugin )
        {
            if ( plugin != null )
            {
                String org, val;
                // Inherited
                org = plugin.getInherited();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setInherited( val );
                }
                // Configuration
                visit( (Xpp3Dom) plugin.getConfiguration() );
                // GroupId
                org = plugin.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setGroupId( val );
                }
                // ArtifactId
                org = plugin.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setArtifactId( val );
                }
                // Version
                org = plugin.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setVersion( val );
                }
                // Extensions
                org = plugin.getExtensions();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setExtensions( val );
                }
                // Executions
                for ( PluginExecution execution : plugin.getExecutions() )
                {
                    visit( execution );
                }
                // Dependencies
                for ( Dependency dependency : plugin.getDependencies() )
                {
                    visit( dependency );
                }
            }
        }

        private void visit( PluginExecution execution )
        {
            if ( execution != null )
            {
                String org, val;
                // Inherited
                org = execution.getInherited();
                val = interpolate( org );
                if ( org != val )
                {
                    execution.setInherited( val );
                }
                // Configuration
                visit( (Xpp3Dom) execution.getConfiguration() );
                // Id
                org = execution.getId();
                val = interpolate( org );
                if ( org != val )
                {
                    execution.setId( val );
                }
                // Phase
                org = execution.getPhase();
                val = interpolate( org );
                if ( org != val )
                {
                    execution.setPhase( val );
                }
                // Goals
                visit( execution.getGoals() );
            }
        }

        private void visit( Xpp3Dom dom )
        {
            if ( dom != null )
            {
                String org, val;
                // Content
                org = dom.getValue();
                val = interpolate( org );
                if ( org != val )
                {
                    dom.setValue( val );
                }
                // Attributes
                for ( String attr : dom.getAttributeNames() )
                {
                    org = dom.getAttribute( attr );
                    val = interpolate( org );
                    if ( org != val )
                    {
                        dom.setAttribute( attr, val );
                    }
                }
                // Children
                for ( int i = 0, l = dom.getChildCount(); i < l; i++ )
                {
                    visit( dom.getChild( i ) );
                }
            }
        }

        private void visit( Extension extension )
        {
            if ( extension != null )
            {
                String org, val;
                // GroupId
                org = extension.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    extension.setGroupId( val );
                }
                // ArtifactId
                org = extension.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    extension.setArtifactId( val );
                }
                // Version
                org = extension.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    extension.setVersion( val );
                }
            }
        }

        private void visit( Profile profile )
        {
            if ( profile != null )
            {
                String org, val;
                // ModelBase
                visit( (ModelBase) profile );
                // Id
                org = profile.getId();
                val = interpolate( org );
                if ( org != val )
                {
                    profile.setId( val );
                }
                // Activation
                visit( profile.getActivation() );
                // Build
                visit( profile.getBuild() );
            }
        }

        private void visit( Activation activation )
        {
            if ( activation != null )
            {
                String org, val;
                // Jdk
                org = activation.getJdk();
                val = interpolate( org );
                if ( org != val )
                {
                    activation.setJdk( val );
                }
                // OS
                visit( activation.getOs() );
                // Property
                visit( activation.getProperty() );
                // File
                visit( activation.getFile() );
            }
        }

        private void visit( ActivationOS activationOS )
        {
            if ( activationOS != null )
            {
                String org, val;
                // Name
                org = activationOS.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    activationOS.setName( val );
                }
                // Family
                org = activationOS.getFamily();
                val = interpolate( org );
                if ( org != val )
                {
                    activationOS.setFamily( val );
                }
                // Arch
                org = activationOS.getArch();
                val = interpolate( org );
                if ( org != val )
                {
                    activationOS.setArch( val );
                }
                // Version
                org = activationOS.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    activationOS.setVersion( val );
                }
            }
        }

        private void visit( ActivationProperty activationProperty )
        {
            if ( activationProperty != null )
            {
                String org, val;
                // Name
                org = activationProperty.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    activationProperty.setName( val );
                }
                // Value
                org = activationProperty.getValue();
                val = interpolate( org );
                if ( org != val )
                {
                    activationProperty.setValue( val );
                }
            }
        }

        private void visit( ActivationFile activationFile )
        {
            if ( activationFile != null )
            {
                String org, val;
                // Missing
                org = activationFile.getMissing();
                val = interpolate( org );
                if ( org != val )
                {
                    activationFile.setMissing( val );
                }
                // Exists
                org = activationFile.getExists();
                val = interpolate( org );
                if ( org != val )
                {
                    activationFile.setExists( val );
                }
            }
        }

        private void visit( ModelBase modelBase )
        {
            if ( modelBase != null )
            {
                visit( modelBase.getModules() );
                visit( modelBase.getDistributionManagement() );
                visit( modelBase.getProperties() );
                visit( modelBase.getDependencyManagement() );
                for ( Dependency dependency : modelBase.getDependencies() )
                {
                    visit( dependency );
                }
                for ( Repository repository : modelBase.getRepositories() )
                {
                    visit( repository );
                }
                for ( Repository repository : modelBase.getPluginRepositories() )
                {
                    visit( repository );
                }
                visit( modelBase.getReporting() );
            }
        }

        private void visit( DistributionManagement distributionManagement )
        {
            if ( distributionManagement != null )
            {
                String org, val;
                // Repository
                visit( distributionManagement.getRepository() );
                // SnapshotRepository
                visit( distributionManagement.getSnapshotRepository() );
                // Site
                visit( distributionManagement.getSite() );
                // DownloadUrl
                org = distributionManagement.getDownloadUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    distributionManagement.setDownloadUrl( val );
                }
                // Relocation
                visit( distributionManagement.getRelocation() );
            }
        }

        private void visit( Site site )
        {
            if ( site != null )
            {
                String org, val;
                // Id
                org = site.getId();
                val = interpolate( org );
                if ( org != val )
                {
                    site.setId( val );
                }
                // Name
                org = site.getName();
                val = interpolate( org );
                if ( org != val )
                {
                    site.setName( val );
                }
                // Url
                org = site.getUrl();
                val = interpolate( org );
                if ( org != val )
                {
                    site.setUrl( val );
                }
                // ChildSiteUrlInheritAppendPath
                org = site.getChildSiteUrlInheritAppendPath();
                val = interpolate( org );
                if ( org != val )
                {
                    site.setChildSiteUrlInheritAppendPath( val );
                }
            }
        }

        private void visit( Relocation relocation )
        {
            if ( relocation != null )
            {
                String org, val;
                // GroupId
                org = relocation.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    relocation.setGroupId( val );
                }
                // ArtifactId
                org = relocation.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    relocation.setArtifactId( val );
                }
                // Version
                org = relocation.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    relocation.setVersion( val );
                }
                // Message
                org = relocation.getMessage();
                val = interpolate( org );
                if ( org != val )
                {
                    relocation.setMessage( val );
                }
            }
        }

        private void visit( DependencyManagement dependencyManagement )
        {
            if ( dependencyManagement != null )
            {
                // Dependencies
                for ( Dependency dependency : dependencyManagement.getDependencies() )
                {
                    visit( dependency );
                }
            }
        }

        private void visit( Repository repository )
        {
            if ( repository != null )
            {
                visit( (RepositoryBase) repository );
                visit( repository.getReleases() );
                visit( repository.getSnapshots() );
            }
        }

        private void visit( RepositoryBase repositoryBase )
        {
            if ( repositoryBase != null )
            {
                // Id
                String orgId = repositoryBase.getId();
                String intId = interpolate( orgId );
                if ( orgId != intId )
                {
                    repositoryBase.setId( intId );
                }
                // Name
                String orgName = repositoryBase.getName();
                String intName = interpolate( orgName );
                if ( orgName != intName )
                {
                    repositoryBase.setName( intName );
                }
                // Url
                String orgUrl = repositoryBase.getUrl();
                String intUrl = interpolate( orgUrl );
                if ( orgUrl != intUrl )
                {
                    repositoryBase.setUrl( intUrl );
                }
                // Layout
                String orgLayout = repositoryBase.getLayout();
                String intLayout = interpolate( orgLayout );
                if ( orgLayout != intLayout )
                {
                    repositoryBase.setLayout( intLayout );
                }
            }
        }

        private void visit( RepositoryPolicy repositoryPolicy )
        {
            if ( repositoryPolicy != null )
            {
                // Enabled
                String orgEnabled = repositoryPolicy.getEnabled();
                String intEnabled = interpolate( orgEnabled );
                if ( orgEnabled != intEnabled )
                {
                    repositoryPolicy.setEnabled( intEnabled );
                }
                // UpdatePolicy
                String orgUpdatePolicy = repositoryPolicy.getUpdatePolicy();
                String intUpdatePolicy = interpolate( orgUpdatePolicy );
                if ( orgUpdatePolicy != intUpdatePolicy )
                {
                    repositoryPolicy.setUpdatePolicy( intUpdatePolicy );
                }
                // ChecksumPolicy
                String orgChecksumPolicy = repositoryPolicy.getChecksumPolicy();
                String intChecksumPolicy = interpolate( orgChecksumPolicy );
                if ( orgChecksumPolicy != intChecksumPolicy )
                {
                    repositoryPolicy.setChecksumPolicy( intChecksumPolicy );
                }
            }
        }

        private void visit( Dependency dependency )
        {
            if ( dependency != null )
            {
                String org, val;
                // GroupId
                org = dependency.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setGroupId( val );
                    dependency.clearManagementKey();
                }
                // ArtifactId
                org = dependency.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setArtifactId( val );
                    dependency.clearManagementKey();
                }
                // Version
                org = dependency.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setVersion( val );
                }
                // Type
                org = dependency.getType();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setType( val );
                    dependency.clearManagementKey();
                }
                // Classifier
                org = dependency.getClassifier();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setClassifier( val );
                    dependency.clearManagementKey();
                }
                // Scope
                org = dependency.getScope();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setScope( val );
                }
                // SystemPath
                org = dependency.getSystemPath();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setSystemPath( val );
                }
                // Exclusions
                for ( Exclusion exclusion : dependency.getExclusions() )
                {
                    visit( exclusion );
                }
                // Optional
                org = dependency.getOptional();
                val = interpolate( org );
                if ( org != val )
                {
                    dependency.setOptional( val );
                }
            }
        }

        private void visit( Exclusion exclusion )
        {
            if ( exclusion != null )
            {
                String org, val;
                // GroupId
                org = exclusion.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    exclusion.setGroupId( val );
                }
                // ArtifactId
                org = exclusion.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    exclusion.setArtifactId( val );
                }
            }
        }

        private void visit( Reporting reporting )
        {
            if ( reporting != null )
            {
                String org, val;
                // ExcludeDefaults
                org = reporting.getExcludeDefaults();
                val = interpolate( org );
                if ( org != val )
                {
                    reporting.setExcludeDefaults( val );
                }
                // OutputDirectory
                org = reporting.getOutputDirectory();
                val = interpolate( org );
                if ( org != val )
                {
                    reporting.setOutputDirectory( val );
                }
                // Plugins
                for ( ReportPlugin plugin : reporting.getPlugins() )
                {
                    visit( plugin );
                }
            }
        }

        private void visit( ReportPlugin plugin )
        {
            if ( plugin != null )
            {
                String org, val;
                // Inherited
                org = plugin.getInherited();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setInherited( val );
                }
                // Configuration
                visit( (Xpp3Dom) plugin.getConfiguration() );
                // GroupId
                org = plugin.getGroupId();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setGroupId( val );
                }
                // ArtifactId
                org = plugin.getArtifactId();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setArtifactId( val );
                }
                // Version
                org = plugin.getVersion();
                val = interpolate( org );
                if ( org != val )
                {
                    plugin.setVersion( val );
                }
                // ReportSets
                for ( ReportSet reportSet : plugin.getReportSets() )
                {
                    visit( reportSet );
                }
            }
        }

        private void visit( ReportSet reportSet )
        {
            if ( reportSet != null )
            {
                String org, val;
                // Inherited
                org = reportSet.getInherited();
                val = interpolate( org );
                if ( org != val )
                {
                    reportSet.setInherited( val );
                }
                // Configuration
                visit( (Xpp3Dom) reportSet.getConfiguration() );
                // Id
                org = reportSet.getId();
                val = interpolate( org );
                if ( org != val )
                {
                    reportSet.setId( val );
                }
                // Reports
                visit( reportSet.getReports() );
            }
        }

        private void visit( Properties properties )
        {
            if ( properties != null )
            {
                for ( Map.Entry<Object, Object> entry : properties.entrySet() )
                {
                    Object v = entry.getValue();
                    if ( v instanceof String )
                    {
                        String value = (String) v;
                        String inter = interpolate( value );
                        if ( value != inter && inter != null )
                        {
                            entry.setValue( inter );
                        }
                    }
                }
            }
        }

        private void visit( List<String> list )
        {
            if ( list != null )
            {
                ListIterator<String> it = list.listIterator();
                while ( it.hasNext() )
                {
                    String value = it.next();
                    String inter = interpolate( value );
                    if ( value != inter )
                    {
                        it.set( inter );
                    }
                }
            }
        }

        private String interpolate( String value )
        {
            return interpolator.interpolate( value );
        }

    }
}
