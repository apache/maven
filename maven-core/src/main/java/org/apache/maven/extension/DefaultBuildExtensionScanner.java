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
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    public void scanForBuildExtensions( List files,
                                        MavenExecutionRequest request )
        throws ExtensionScanningException
    {
        List visited = new ArrayList();

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            File pom = (File) it.next();

            scanInternal( pom, request, visited, files );
        }
    }

    public void scanForBuildExtensions( File pom,
                                        MavenExecutionRequest request )
        throws ExtensionScanningException
    {
        scanInternal( pom, request, new ArrayList(), Collections.singletonList( pom ) );
    }

    // TODO: Use a build-context cache object for visitedModelIdx and reactorFiles,
    //       once we move to just-in-time project scanning.
    private void scanInternal( File pom,
                               MavenExecutionRequest request,
                               List visitedModelIds,
                               List reactorFiles )
        throws ExtensionScanningException
    {

        try
        {
            List originalRemoteRepositories = getInitialRemoteRepositories();

            getLogger().debug( "Pre-scanning POM lineage of: " + pom + " for build extensions." );

            ModelLineage lineage = buildModelLineage( pom, request, originalRemoteRepositories );

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

                if ( visitedModelIds.contains( key ) )
                {
                    getLogger().debug( "Already visited: " + key + "; continuing." );
                    continue;
                }

                visitedModelIds.add( key );

                getLogger().debug(
                                   "Checking: " + model.getId() + " for extensions. (It has "
                                       + model.getModules().size() + " modules.)" );

                if ( inheritedInterpolationValues == null )
                {
                    inheritedInterpolationValues = new HashMap();
                }

                model = modelInterpolator.interpolate( model, inheritedInterpolationValues, false );

                grabManagedPluginsWithExtensionsFlagTurnedOn( model, managedPluginsWithExtensionsFlag );

                checkModelBuildForExtensions( model, request, inheritedRemoteRepositories, managedPluginsWithExtensionsFlag );

                if ( !reactorFiles.contains( modelPom ) )
                {
                    getLogger().debug(
                                       "POM: " + modelPom
                                           + " is not in the current reactor. Its modules will not be scanned." );
                }
                else
                {
                    checkModulesForExtensions( modelPom,
                                               model,
                                               request,
                                               originalRemoteRepositories,
                                               visitedModelIds,
                                               reactorFiles );
                }

                Properties modelProps = model.getProperties();
                if ( modelProps != null )
                {
                    inheritedInterpolationValues.putAll( modelProps );
                }
            }

            getLogger().debug( "Finished pre-scanning: " + pom + " for build extensions." );

            extensionManager.registerWagons();
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
                                            List reactorFiles )
        throws ExtensionScanningException
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
                    getLogger().debug(
                                       "Cannot find POM for module: " + moduleSubpath
                                           + "; continuing scan with next module. (Full path was: "
                                           + modulePomDirectory + ")" );
                    continue;
                }

                scanInternal( modulePomDirectory, request, visitedModelIds, reactorFiles );
            }
        }
    }

    private void checkModelBuildForExtensions( Model model,
                                               MavenExecutionRequest request,
                                               List remoteRepositories,
                                               Set managedPluginsWithExtensionsFlag )
        throws ExtensionScanningException
    {
        // FIXME: Fix the log level here.
        getLogger().info( "Checking " + model.getId() + " for extensions." );

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

    private ModelLineage buildModelLineage( File pom, MavenExecutionRequest request,
                                            List originalRemoteRepositories )
        throws ExtensionScanningException
    {
        ProfileManager profileManager = request.getProfileManager();

        ProfileActivationContext profileActivationContext = profileManager == null
                        ? new DefaultProfileActivationContext( System.getProperties(), false )
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
            lineage = modelLineageBuilder.buildModelLineage( pom, request.getLocalRepository(), originalRemoteRepositories,
                                                             request.getProfileManager(), false, true );
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

    private List getInitialRemoteRepositories()
        throws ExtensionScanningException
    {
        MavenProject superProject;

        try
        {
            superProject = projectBuilder.buildStandaloneSuperProject();
        }
        catch ( ProjectBuildingException e )
        {
            throw new ExtensionScanningException(
                                                  "Error building super-POM for retrieving the default remote repository list: "
                                                      + e.getMessage(), e );
        }

        return superProject.getRemoteArtifactRepositories();
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
