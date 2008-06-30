package org.apache.maven.extension;

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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.reactor.MissingModuleException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DefaultBuildExtensionScanner
    implements BuildExtensionScanner, LogEnabled
{

    private Logger logger;

    private ExtensionManager extensionManager;

    private MavenProjectBuilder projectBuilder;

    private ModelLineageBuilder modelLineageBuilder;

    private ModelInterpolator modelInterpolator;

    // cached.
    private MavenProject basicSuperProject;

    public DefaultBuildExtensionScanner()
    {
    }

    protected DefaultBuildExtensionScanner( ExtensionManager extensionManager,
                                            MavenProjectBuilder projectBuilder,
                                            ModelLineageBuilder modelLineageBuilder,
                                            ModelInterpolator modelInterpolator,
                                            Logger logger )
    {
        this.extensionManager = extensionManager;
        this.projectBuilder = projectBuilder;
        this.modelLineageBuilder = modelLineageBuilder;
        this.modelInterpolator = modelInterpolator;
        this.logger = logger;
    }

    public void scanForBuildExtensions( List files,
                                        MavenExecutionRequest request,
                                        boolean ignoreMissingModules )
        throws ExtensionScanningException, MissingModuleException
    {
        List visited = new ArrayList();

        List internalFiles = new ArrayList();

        internalFiles.addAll(files);

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            File pom = (File) it.next();

            scanInternal( pom, request, visited, internalFiles, ignoreMissingModules );
        }
    }

    public void scanForBuildExtensions( File pom,
                                        MavenExecutionRequest request,
                                        boolean ignoreMissingModules )
        throws ExtensionScanningException, MissingModuleException
    {
        List internalFiles = new ArrayList();

        internalFiles.add( pom );

        scanInternal( pom, request, new ArrayList(), internalFiles, ignoreMissingModules );
    }

    private void scanInternal( File pom,
                               MavenExecutionRequest request,
                               List visitedModelIds,
                               List reactorFiles,
                               boolean ignoreMissingModules )
        throws ExtensionScanningException, MissingModuleException
    {

        try
        {
            List originalRemoteRepositories = getInitialRemoteRepositories( request.getProjectBuildingConfiguration() );

            getLogger().debug( "Pre-scanning POM lineage of: " + pom + " for build extensions." );

            ModelLineage lineage = buildModelLineage( pom, request.getProjectBuildingConfiguration(), originalRemoteRepositories );

            Map inheritedInterpolationValues = new HashMap();

            List inheritedRemoteRepositories = new ArrayList();

            inheritedRemoteRepositories.addAll( originalRemoteRepositories );

            Set managedPluginsWithExtensionsFlag = new HashSet();

            for ( ModelLineageIterator lineageIterator = lineage.reversedLineageIterator(); lineageIterator.hasNext(); )
            {
                Model model = (Model) lineageIterator.next();
                File modelPom = lineageIterator.getPOMFile();

                List remoteRepos = lineageIterator.getArtifactRepositories();
                if ( ( remoteRepos != null ) && !remoteRepos.isEmpty() )
                {
                    inheritedRemoteRepositories.addAll( remoteRepos );
                }

                String key = createKey( model );

                ProjectBuilderConfiguration config = request.getProjectBuildingConfiguration();
                Properties execProps = new Properties();
                if ( config.getExecutionProperties() != null )
                {
                    execProps.putAll( config.getExecutionProperties() );
                }

                if ( inheritedInterpolationValues != null )
                {
                    execProps.putAll( inheritedInterpolationValues );
                }
                else
                {
                    inheritedInterpolationValues = new HashMap();
                }

                config.setExecutionProperties( execProps );

                model = modelInterpolator.interpolate( model, modelPom.getParentFile(), config, getLogger().isDebugEnabled() );

                grabManagedPluginsWithExtensionsFlagTurnedOn( model, managedPluginsWithExtensionsFlag );

                Properties modelProps = model.getProperties();
                if ( modelProps != null )
                {
                    inheritedInterpolationValues.putAll( modelProps );
                }

                if ( visitedModelIds.contains( key ) )
                {
                    getLogger().debug( "Already visited: " + key + "; continuing." );
                    continue;
                }

                visitedModelIds.add( key );

                getLogger().debug(
                                   "Checking: " + model.getId() + " for extensions. (It has "
                                       + model.getModules().size() + " modules.)" );

                checkModelBuildForExtensions( model, request, inheritedRemoteRepositories, managedPluginsWithExtensionsFlag );

                if ( !reactorFiles.contains( modelPom ) )
                {
                    getLogger().debug(
                                       "POM: " + modelPom
                                           + " is not in the current reactor. Its modules will not be scanned." );
                }
                else if ( request.isRecursive() )
                {
                    checkModulesForExtensions( modelPom,
                                               model,
                                               request,
                                               originalRemoteRepositories,
                                               visitedModelIds,
                                               reactorFiles,
                                               ignoreMissingModules );
                }
            }
        }
        catch ( ModelInterpolationException e )
        {
            throw new ExtensionScanningException( "Failed to interpolate model from: " + pom
                + " prior to scanning for extensions.", pom, e );
        }
    }

    private void grabManagedPluginsWithExtensionsFlagTurnedOn( Model model,
                                                               Set managedPluginsWithExtensionsFlag )
    {
        Build build = model.getBuild();
        if ( build != null )
        {
            PluginManagement pluginManagement = build.getPluginManagement();
            if ( pluginManagement != null )
            {
                List plugins = pluginManagement.getPlugins();
                if ( ( plugins != null ) && !plugins.isEmpty() )
                {
                    for ( Iterator it = plugins.iterator(); it.hasNext(); )
                    {
                        Plugin plugin = (Plugin) it.next();
                        if ( plugin.isExtensions() )
                        {
                            managedPluginsWithExtensionsFlag.add( plugin.getKey() );
                        }
                    }
                }
            }
        }
    }

    private String createKey( Model model )
    {
        Parent parent = model.getParent();

        String groupId = model.getGroupId();
        if ( groupId == null )
        {
            groupId = parent.getGroupId();
        }

        String artifactId = model.getArtifactId();

        return groupId + ":" + artifactId;
    }

    private void checkModulesForExtensions( File containingPom,
                                            Model model,
                                            MavenExecutionRequest request,
                                            List originalRemoteRepositories,
                                            List visitedModelIds,
                                            List reactorFiles,
                                            boolean ignoreMissingModules )
        throws ExtensionScanningException, MissingModuleException
    {
        // FIXME: This gets a little sticky, because modules can be added by profiles that require
        // an extension in place before they can be activated.
        List modules = model.getModules();

        if ( modules != null )
        {
            File basedir = containingPom.getParentFile();
            getLogger().debug( "Basedir is: " + basedir );

            for ( Iterator it = modules.iterator(); it.hasNext(); )
            {
                // TODO: change this if we ever find a way to replace module definitions with g:a:v
                String moduleSubpath = (String) it.next();

                getLogger().debug( "Scanning module: " + moduleSubpath );

                File modulePomDirectory;

                try
                {
                    modulePomDirectory = new File( basedir, moduleSubpath ).getCanonicalFile();

                    // ----------------------------------------------------------------------------
                    // We need to make sure we don't loop infinitely in the case where we have
                    // something like:
                    //
                    // <modules>
                    //    <module>../MNGECLIPSE-256web</module>
                    //    <module>../MNGECLIPSE-256utility</module>
                    // </modules>
                    //
                    // Where once we walk into the first module it will just get its parent dir
                    // containing its POM over and over again unless we make a comparison to
                    // basedir and the modulePomDirectory.
                    // ----------------------------------------------------------------------------

                    if ( modulePomDirectory.equals( basedir.getCanonicalFile() ) )
                    {
                        break;
                    }
                }
                catch ( IOException e )
                {
                    throw new ExtensionScanningException( "Error getting canonical path for modulePomDirectory.", containingPom, moduleSubpath, e );
                }

                if ( modulePomDirectory.isDirectory() )
                {
                    getLogger().debug(
                                       "Assuming POM file 'pom.xml' in module: " + moduleSubpath + " under basedir: "
                                           + basedir );
                    modulePomDirectory = new File( modulePomDirectory, "pom.xml" );
                }

                if ( !modulePomDirectory.exists() )
                {
                    if ( ignoreMissingModules )
                    {
                        continue;
                    }
                    else
                    {
                        throw new MissingModuleException( moduleSubpath, modulePomDirectory, containingPom );
                    }
                }

                reactorFiles.add( modulePomDirectory );

                scanInternal( modulePomDirectory, request, visitedModelIds, reactorFiles, ignoreMissingModules );
            }
        }
    }

    private void checkModelBuildForExtensions( Model model,
                                               MavenExecutionRequest request,
                                               List remoteRepositories,
                                               Set managedPluginsWithExtensionsFlag )
        throws ExtensionScanningException
    {
        getLogger().debug( "Checking " + model.getId() + " for extensions." );

        Build build = model.getBuild();

        if ( build != null )
        {
            List extensions = build.getExtensions();

            if ( ( extensions != null ) && !extensions.isEmpty() )
            {
                // thankfully, we don't have to deal with dependencyManagement here, yet.
                // TODO Revisit if/when extensions are made to use the info in dependencyManagement
                for ( Iterator extensionIterator = extensions.iterator(); extensionIterator.hasNext(); )
                {
                    Extension extension = (Extension) extensionIterator.next();

                    getLogger().debug(
                                       "Adding extension: "
                                           + ArtifactUtils.versionlessKey( extension.getGroupId(), extension
                                               .getArtifactId() ) + " from model: " + model.getId() );

                    try
                    {
                        extensionManager.addExtension( extension, model, remoteRepositories, request );
                    }
                    catch ( ExtensionManagerException e )
                    {
                        throw new ExtensionScanningException( "Cannot resolve pre-scanned extension artifact: "
                            + extension.getGroupId() + ":" + extension.getArtifactId() + ": " + e.getMessage(), model, extension, e );
                    }
                }
            }

            List plugins = build.getPlugins();

            if ( ( plugins != null ) && !plugins.isEmpty() )
            {
                for ( Iterator extensionIterator = plugins.iterator(); extensionIterator.hasNext(); )
                {
                    Plugin plugin = (Plugin) extensionIterator.next();

                    if ( plugin.isExtensions() || managedPluginsWithExtensionsFlag.contains( plugin.getKey() ) )
                    {
                        getLogger().debug( "Adding plugin: " + plugin.getKey() + " as an extension(from model: " + model.getId() + ")" );

                        try
                        {
                            extensionManager.addPluginAsExtension( plugin, model, remoteRepositories, request );
                        }
                        catch ( ExtensionManagerException e )
                        {
                            throw new ExtensionScanningException( "Cannot resolve pre-scanned plugin artifact (for use as an extension): "
                                + plugin.getKey() + ": " + e.getMessage(), model, plugin, e );
                        }
                    }
                }
            }
        }
    }

    private ModelLineage buildModelLineage( File pom, ProjectBuilderConfiguration config,
                                            List originalRemoteRepositories )
        throws ExtensionScanningException
    {
        ProfileManager profileManager = config.getGlobalProfileManager();

        ProfileActivationContext profileActivationContext = profileManager == null
                        ? new DefaultProfileActivationContext( config.getExecutionProperties(), false )
                        : profileManager.getProfileActivationContext();

        boolean suppressActivatorFailure = profileActivationContext.isCustomActivatorFailureSuppressed();

        ModelLineage lineage;
        try
        {
            getLogger().debug( "Building model-lineage for: " + pom + " to pre-scan for extensions." );

            // NOTE: We're assuming that this scan happens only for local filesystem POMs,
            // not for POMs from the repository...otherwise, we would need to be more careful with
            // the last parameter here and determine whether it's appropriate for the POM to have
            // an accompanying profiles.xml file.
            profileActivationContext.setCustomActivatorFailureSuppressed( true );

            lineage = modelLineageBuilder.buildModelLineage( pom, config, originalRemoteRepositories, false, true );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ExtensionScanningException( "Error building model lineage in order to pre-scan for extensions: "
                + e.getMessage(), pom, e );
        }
        finally
        {
            profileActivationContext.setCustomActivatorFailureSuppressed( suppressActivatorFailure );
        }

        return lineage;
    }

    private List<ArtifactRepository> getInitialRemoteRepositories( ProjectBuilderConfiguration config )
        throws ExtensionScanningException
    {
        if ( basicSuperProject == null )
        {
            try
            {
                basicSuperProject = projectBuilder.buildStandaloneSuperProject( config );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ExtensionScanningException(
                                                      "Error building super-POM for retrieving the default remote repository list: "
                                                          + e.getMessage(), e );
            }
        }

        return basicSuperProject.getRemoteArtifactRepositories();
    }

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultBuildExtensionScanner:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
