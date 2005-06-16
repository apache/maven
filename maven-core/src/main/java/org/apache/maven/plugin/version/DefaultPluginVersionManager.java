package org.apache.maven.plugin.version;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.PluginHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.registry.MavenPluginRegistryBuilder;
import org.apache.maven.plugin.registry.PluginRegistry;
import org.apache.maven.plugin.registry.PluginRegistryUtils;
import org.apache.maven.plugin.registry.TrackableBase;
import org.apache.maven.plugin.registry.io.xpp3.PluginRegistryXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.inputhandler.InputHandler;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DefaultPluginVersionManager
    extends AbstractLogEnabled
    implements PluginVersionManager
{

    private MavenPluginRegistryBuilder mavenPluginRegistryBuilder;

    private ArtifactResolver artifactResolver;

    private ArtifactFactory artifactFactory;

    private InputHandler inputHandler;

    public String resolvePluginVersion( String groupId, String artifactId, MavenProject project,
                                       ArtifactRepository localRepository, boolean interactiveMode )
        throws PluginVersionResolutionException
    {
        // first pass...if the plugin is specified in the pom, try to retrieve the version from there.
        String version = getVersionFromPluginConfig( groupId, artifactId, project );

        // TODO: we're NEVER going to persist POM-derived plugin versions...dunno if this is 'right' or not.
        String updatedVersion = null;

        boolean promptToPersistUpdatedVersion = false;

        // second pass...if the plugin is listed in the settings.xml, use the version from <useVersion/>.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve existing useVersion.
            version = resolveExistingFromPluginRegistry( groupId, artifactId );

            if ( StringUtils.isNotEmpty( version ) )
            {
                // TODO: 2. check for updates. Determine whether this is the right time to attempt to update the version.
                boolean checkForUpdates = true;

                if ( checkForUpdates )
                {
                    updatedVersion = resolveReleaseVersion( groupId, artifactId, project
                        .getRemoteArtifactRepositories(), localRepository );

                    if ( StringUtils.isNotEmpty( updatedVersion ) && !updatedVersion.equals( version ) )
                    {
                        boolean isRejected = checkForRejectedStatus( groupId, artifactId, updatedVersion );

                        // we should only prompt to use this version if the user has not previously rejected it.
                        promptToPersistUpdatedVersion = !isRejected;

                        if ( isRejected )
                        {
                            updatedVersion = null;
                        }
                        else
                        {
                            getLogger()
                                .info( "Plugin {" + constructPluginKey( groupId, artifactId ) + "} has updates." );
                        }
                    }
                    else
                    {
                        // let's be very careful about making this code resistant to change...
                        promptToPersistUpdatedVersion = false;
                    }
                }
                else
                {
                    // let's be very careful about making this code resistant to change...
                    promptToPersistUpdatedVersion = false;
                }
            }
            else
            {
                // let's be very careful about making this code resistant to change...
                promptToPersistUpdatedVersion = false;
            }
        }

        // final pass...retrieve the version for RELEASE and also set that resolved version as the <useVersion/> 
        // in settings.xml.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve the version to be used THIS TIME
            version = resolveReleaseVersion( groupId, artifactId, project.getRemoteArtifactRepositories(),
                                             localRepository );

            // 2. Set the updatedVersion so the user will be prompted whether to make this version permanent.
            updatedVersion = version;

            promptToPersistUpdatedVersion = true;
        }

        // if we still haven't found a version, then fail early before we get into the update goop.
        if ( StringUtils.isEmpty( version ) )
        {
            throw new PluginVersionResolutionException( groupId, artifactId,
                                                        "Failed to resolve a valid version for this plugin" );
        }

        // if we're not in interactive mode, then the default is to update and NOT prompt.
        // TODO: replace this with proper update policy-based determination related to batch mode.
        boolean persistUpdatedVersion = promptToPersistUpdatedVersion && !interactiveMode;

        // don't prompt if not in interactive mode.
        if ( promptToPersistUpdatedVersion && interactiveMode )
        {
            persistUpdatedVersion = promptToPersistPluginUpdate( version, updatedVersion, groupId, artifactId );
        }

        // if it is determined that we should use this version, persist it as useVersion.
        if ( persistUpdatedVersion )
        {
            updatePluginVersionInRegistry( groupId, artifactId, updatedVersion );
        }
        // otherwise, if we prompted the user to update, we should treat this as a rejectedVersion, and
        // persist it iff the plugin pre-exists and is in the user-level registry.
        else if ( promptToPersistUpdatedVersion && interactiveMode )
        {
            addNewVersionToRejectedListInExisting( groupId, artifactId, updatedVersion );
        }

        return version;
    }

    private boolean checkForRejectedStatus( String groupId, String artifactId, String version )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        return plugin.getRejectedVersions().contains( version );
    }

    private boolean promptToPersistPluginUpdate( String version, String updatedVersion, String groupId,
                                                String artifactId )
        throws PluginVersionResolutionException
    {
        try
        {
            StringBuffer message = new StringBuffer();

            // this means that the plugin is not registered.
            if ( version != null && version.equals( updatedVersion ) )
            {
                message.append( "Unregistered plugin detected.\n\n" );
            }
            else
            {
                message.append( "New plugin version detected.\n\n" );
            }

            message.append( "Group ID: " ).append( groupId ).append( "\n" );
            message.append( "Artifact ID: " ).append( artifactId ).append( "\n" );
            message.append( "\n" );

            // this means that we've detected a new, non-rejected plugin version.
            if ( version != null && !version.equals( updatedVersion ) )
            {
                message.append( "Registered Version: " ).append( version ).append( "\n" );
            }

            message.append( "Detected (NEW) Version: " ).append( updatedVersion ).append( "\n" );
            message.append( "\n" );
            message.append( "Would you like to use this new version from now on? [Y/n] " );

            // TODO: check the GUI-friendliness of this approach to collecting input.
            // If we can't port this prompt into a GUI, IDE-integration will not work well.
            getLogger().info( message.toString() );

            String persistAnswer = inputHandler.readLine();

            return StringUtils.isEmpty( persistAnswer ) || "y".equalsIgnoreCase( persistAnswer );

        }
        catch ( Exception e )
        {
            throw new PluginVersionResolutionException( groupId, artifactId, "Can't read user input.", e );
        }
    }

    private void addNewVersionToRejectedListInExisting( String groupId, String artifactId, String rejectedVersion )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        String pluginKey = constructPluginKey( groupId, artifactId );

        if ( plugin != null && !TrackableBase.GLOBAL_LEVEL.equals( plugin.getSourceLevel() ) )
        {
            plugin.addRejectedVersion( rejectedVersion );

            writeUserRegistry( groupId, artifactId, pluginRegistry );

            getLogger().warn(
                              "Plugin version: " + rejectedVersion + " added to your rejectedVersions list.\n"
                                  + "You will not be prompted for this version again.\n\nPlugin: " + pluginKey );
        }
        else
        {
            getLogger().warn( "Cannot add rejectedVersion entry for: " + rejectedVersion + ".\n\nPlugin: " + pluginKey );
        }
    }

    private String resolveExistingFromPluginRegistry( String groupId, String artifactId )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        String version = null;

        if ( plugin != null )
        {
            version = plugin.getUseVersion();
        }

        return version;
    }

    private org.apache.maven.plugin.registry.Plugin getPlugin( String groupId, String artifactId,
                                                              PluginRegistry pluginRegistry )
    {
        Map pluginsByKey = null;

        if ( pluginRegistry != null )
        {
            pluginsByKey = pluginRegistry.getPluginsByKey();
        }
        else
        {
            pluginsByKey = new HashMap();
        }

        String pluginKey = constructPluginKey( groupId, artifactId );

        return (org.apache.maven.plugin.registry.Plugin) pluginsByKey.get( pluginKey );
    }

    private String constructPluginKey( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }

    private String getVersionFromPluginConfig( String groupId, String artifactId, MavenProject project )
    {
        String version = null;

        Plugin pluginConfig = null;

        for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
            {
                pluginConfig = plugin;

                break;
            }
        }

        // won't this overwrite the above loop if it exists in both places (unlikely, I know)??
        // maybe that's the idea...?
        if ( project.getReports() != null )
        {
            for ( Iterator it = project.getReports().getPlugins().iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();

                if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                {
                    pluginConfig = plugin;

                    break;
                }
            }
        }

        if ( pluginConfig != null )
        {
            version = pluginConfig.getVersion();
        }

        return version;
    }

    private void updatePluginVersionInRegistry( String groupId, String artifactId, String version )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        // if we can find the plugin, but we've gotten here, the useVersion must be missing; fill it in.
        if ( plugin != null )
        {
            if ( PluginRegistry.GLOBAL_LEVEL.equals( plugin.getSourceLevel() ) )
            {
                // do nothing. We don't rewrite the globals, under any circumstances.
                getLogger().warn(
                                  "Cannot update registered version for plugin {" + groupId + ":" + artifactId
                                      + "}; it is specified in the global registry." );
            }
            else
            {
                plugin.setUseVersion( version );
            }
        }
        else
        {
            plugin = new org.apache.maven.plugin.registry.Plugin();

            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );
            plugin.setUseVersion( version );
            plugin.setAutoUpdate( false );

            pluginRegistry.addPlugin( plugin );
        }

        writeUserRegistry( groupId, artifactId, pluginRegistry );
    }

    private void writeUserRegistry( String groupId, String artifactId, PluginRegistry pluginRegistry )
        throws PluginVersionResolutionException
    {
        File pluginRegistryFile = pluginRegistry.getFile();

        PluginRegistry extractedUserRegistry = PluginRegistryUtils.extractUserPluginRegistry( pluginRegistry );

        // only rewrite the user-level registry if one existed before, or if we've created user-level data here.
        if ( extractedUserRegistry != null )
        {
            FileWriter fWriter = null;

            try
            {
                fWriter = new FileWriter( pluginRegistryFile );

                PluginRegistryXpp3Writer writer = new PluginRegistryXpp3Writer();

                writer.write( fWriter, PluginRegistryUtils.extractUserPluginRegistry( pluginRegistry ) );
            }
            catch ( IOException e )
            {
                // TODO: should we soften this to a warning??
                throw new PluginVersionResolutionException(
                                                            groupId,
                                                            artifactId,
                                                            "Cannot rewrite user-level plugin-registry.xml with new plugin version.",
                                                            e );
            }
            finally
            {
                IOUtil.close( fWriter );
            }
        }
    }

    private PluginRegistry getPluginRegistry( String groupId, String artifactId )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = null;

        try
        {
            pluginRegistry = mavenPluginRegistryBuilder.buildPluginRegistry();
        }
        catch ( IOException e )
        {
            throw new PluginVersionResolutionException( groupId, artifactId, "Cannot read plugin registry", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new PluginVersionResolutionException( groupId, artifactId, "Cannot parse plugin registry", e );
        }

        if ( pluginRegistry == null )
        {
            pluginRegistry = mavenPluginRegistryBuilder.createUserPluginRegistry();
        }

        return pluginRegistry;
    }

    private String resolveReleaseVersion( String groupId, String artifactId, List remoteRepositories,
                                         ArtifactRepository localRepository )
        throws PluginVersionResolutionException
    {
        Artifact releaseArtifact = artifactFactory.createArtifact( groupId, artifactId, "RELEASE",
                                                                   Artifact.SCOPE_RUNTIME, PluginHandler.PLUGIN_TYPE );

        try
        {
            artifactResolver.resolve( releaseArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginVersionResolutionException( groupId, artifactId,
                                                        "Cannot resolve RELEASE version of this plugin.", e );
        }

        return releaseArtifact.getVersion();
    }

}
