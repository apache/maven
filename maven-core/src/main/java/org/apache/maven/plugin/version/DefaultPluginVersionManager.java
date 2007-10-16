package org.apache.maven.plugin.version;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.registry.MavenPluginRegistryBuilder;
import org.apache.maven.plugin.registry.PluginRegistry;
import org.apache.maven.plugin.registry.PluginRegistryUtils;
import org.apache.maven.plugin.registry.TrackableBase;
import org.apache.maven.plugin.registry.io.xpp3.PluginRegistryXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultPluginVersionManager
    extends AbstractLogEnabled
    implements PluginVersionManager
{
    private MavenPluginRegistryBuilder mavenPluginRegistryBuilder;

    private ArtifactFactory artifactFactory;

    private InputHandler inputHandler;

    private ArtifactMetadataSource artifactMetadataSource;

    // TODO: [jc] Revisit to remove this piece of state. PLUGIN REGISTRY MAY BE UPDATED ON DISK OUT-OF-PROCESS!
    private PluginRegistry pluginRegistry;

    private MavenProjectBuilder mavenProjectBuilder;

    private RuntimeInformation runtimeInformation;

    // TODO: Revisit to remove this piece of state. PLUGIN REGISTRY MAY BE UPDATED ON DISK OUT-OF-PROCESS!
    private Map resolvedMetaVersions = new HashMap();

    public String resolvePluginVersion( String groupId, String artifactId, MavenProject project, Settings settings,
                                        ArtifactRepository localRepository )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        return resolvePluginVersion( groupId, artifactId, project, settings, localRepository, false );
    }

    public String resolveReportPluginVersion( String groupId, String artifactId, MavenProject project,
                                              Settings settings, ArtifactRepository localRepository )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        return resolvePluginVersion( groupId, artifactId, project, settings, localRepository, true );
    }

    private String resolvePluginVersion( String groupId, String artifactId, MavenProject project, Settings settings,
                                         ArtifactRepository localRepository, boolean resolveAsReportPlugin )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        // first pass...if the plugin is specified in the pom, try to retrieve the version from there.
        String version = getVersionFromPluginConfig( groupId, artifactId, project, resolveAsReportPlugin );

        // NOTE: We CANNOT check the current project version here, so delay it until later.
        // It will prevent plugins from building themselves, if they are part of the lifecycle mapping.

        // if there was no explicit version, try for one in the reactor
        if ( version == null )
        {
            if ( project.getProjectReferences() != null )
            {
                String refId = ArtifactUtils.versionlessKey( groupId, artifactId );
                MavenProject ref = (MavenProject) project.getProjectReferences().get( refId );
                if ( ref != null )
                {
                    version = ref.getVersion();
                }
            }
        }

        // we're NEVER going to persist POM-derived plugin versions.
        String updatedVersion = null;

        // we're not going to prompt the user to accept a plugin update until we find one.
        boolean promptToPersist = false;

        RuntimeInfo settingsRTInfo = settings.getRuntimeInfo();

        // determine the behavior WRT prompting the user and installing plugin updates.
        Boolean pluginUpdateOverride = settingsRTInfo.getPluginUpdateOverride();

        // second pass...if we're using the plugin registry, and the plugin is listed in the plugin-registry.xml, use
        // the version from <useVersion/>.
        if ( StringUtils.isEmpty( version ) && settings.isUsePluginRegistry() )
        {
            // resolve existing useVersion.
            version = resolveExistingFromPluginRegistry( groupId, artifactId );

            if ( StringUtils.isNotEmpty( version ) )
            {
                // 2. check for updates. Determine whether this is the right time to attempt to update the version.
                // Only check for plugin updates if:
                //
                //  a. the CLI switch to force plugin updates is set, OR BOTH OF THE FOLLOWING:
                //  b. the CLI switch to suppress plugin updates is NOT set, AND
                //  c. the update interval for the plugin has triggered an update check.
                if ( Boolean.TRUE.equals( pluginUpdateOverride ) ||
                    ( !Boolean.FALSE.equals( pluginUpdateOverride ) && shouldCheckForUpdates( groupId, artifactId ) ) )
                {
                    updatedVersion =
                        resolveMetaVersion( groupId, artifactId, project, localRepository, Artifact.LATEST_VERSION );

                    if ( StringUtils.isNotEmpty( updatedVersion ) && !updatedVersion.equals( version ) )
                    {
                        // see if this version we've resolved is on our previously rejected list.
                        boolean isRejected = checkForRejectedStatus( groupId, artifactId, updatedVersion );

                        // we should only prompt to use this version if the user has not previously rejected it.
                        promptToPersist = !isRejected;

                        // if we've rejected this version previously, forget about updating.
                        if ( isRejected )
                        {
                            updatedVersion = null;
                        }
                        else
                        {
                            getLogger().info(
                                "Plugin \'" + constructPluginKey( groupId, artifactId ) + "\' has updates." );
                        }
                    }
                }
            }
        }

        boolean forcePersist = false;

        // third pass...we're always checking for latest install/deploy, so retrieve the version for LATEST metadata and
        // also set that resolved version as the <useVersion/> in settings.xml.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve the version to be used
            version = resolveMetaVersion( groupId, artifactId, project, localRepository, Artifact.LATEST_VERSION );

            if ( version != null )
            {
                // 2. Set the updatedVersion so the user will be prompted whether to make this version permanent.
                updatedVersion = version;

                // 3. Persist this version without prompting.
                forcePersist = true;
                promptToPersist = false;
            }
        }

        // final pass...retrieve the version for RELEASE and also set that resolved version as the <useVersion/>
        // in settings.xml.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve the version to be used
            version = resolveMetaVersion( groupId, artifactId, project, localRepository, Artifact.RELEASE_VERSION );

            if ( version != null )
            {
                // 2. Set the updatedVersion so the user will be prompted whether to make this version permanent.
                updatedVersion = version;

                // 3. Persist this version without prompting.
                forcePersist = true;
                promptToPersist = false;
            }
        }

        // if we're still empty here, and the current project matches the plugin in question, use the current project's
        // version, I guess...
        if ( StringUtils.isEmpty( version ) && project.getGroupId().equals( groupId ) &&
            project.getArtifactId().equals( artifactId ) )
        {
            version = project.getVersion();
        }

        // if we still haven't found a version, then fail early before we get into the update goop.
        if ( StringUtils.isEmpty( version ) )
        {
            throw new PluginVersionNotFoundException( groupId, artifactId );
        }

        // if the plugin registry is inactive, then the rest of this goop is useless...
        if ( settings.isUsePluginRegistry() )
        {
            // determine whether this build is running in interactive mode
            // If it's not, then we'll defer to the autoUpdate setting from the registry
            // for a decision on updating the plugin in the registry...rather than prompting
            // the user.
            boolean inInteractiveMode = settings.isInteractiveMode();

            // determines what should be done if we're in non-interactive mode.
            // if true, then just update the registry with the new versions.
            String s = getPluginRegistry( groupId, artifactId ).getAutoUpdate();
            boolean autoUpdate = true;
            if ( s != null )
            {
                autoUpdate = Boolean.valueOf( s ).booleanValue();
            }

            // We should persist by default if:
            //
            // 0. RELEASE or LATEST was used to resolve the plugin version (it's not in the registry)
            //
            // -OR-
            //
            // 1. we detected a change in the plugin version from what was in the registry, or
            //      a. the plugin is not registered
            // 2. the pluginUpdateOverride flag has NOT been set to Boolean.FALSE (suppression mode)
            // 3. we're in interactive mode, or
            //      a. the registry is declared to be in autoUpdate mode
            //
            // NOTE: This is only the default value; it may be changed as the result of prompting the user.
            boolean persistUpdate = forcePersist || ( promptToPersist &&
                !Boolean.FALSE.equals( pluginUpdateOverride ) && ( inInteractiveMode || autoUpdate ) );

            // retrieve the apply-to-all flag, if it's been set previously.
            Boolean applyToAll = settings.getRuntimeInfo().getApplyToAllPluginUpdates();

            // Incorporate interactive-mode CLI overrides, and previous decisions on apply-to-all, if appropriate.
            //
            // don't prompt if RELEASE or LATEST was used to resolve the plugin version
            // don't prompt if not in interactive mode.
            // don't prompt if the CLI pluginUpdateOverride is set (either suppression or force mode will stop prompting)
            // don't prompt if the user has selected ALL/NONE previously in this session
            //
            // NOTE: We're incorporating here, to make the usages of this check more consistent and 
            // resistant to change.
            promptToPersist =
                promptToPersist && pluginUpdateOverride == null && applyToAll == null && inInteractiveMode;

            if ( promptToPersist )
            {
                persistUpdate = promptToPersistPluginUpdate( version, updatedVersion, groupId, artifactId, settings );
            }

            // if it is determined that we should use this version, persist it as useVersion.
            // cases where this version will be persisted:
            // 1. the user is prompted and answers yes or all
            // 2. the user has previously answered all in this session
            // 3. the build is running in non-interactive mode, and the registry setting is for auto-update
            if ( !Boolean.FALSE.equals( applyToAll ) && persistUpdate )
            {
                updatePluginVersionInRegistry( groupId, artifactId, updatedVersion );

                // we're using the updated version of the plugin in this session as well.
                version = updatedVersion;
            }
            // otherwise, if we prompted the user to update, we should treat this as a rejectedVersion, and
            // persist it iff the plugin pre-exists and is in the user-level registry.
            else if ( promptToPersist )
            {
                addNewVersionToRejectedListInExisting( groupId, artifactId, updatedVersion );
            }
        }

        return version;
    }

    private boolean shouldCheckForUpdates( String groupId, String artifactId )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        if ( plugin == null )
        {
            return true;
        }
        else
        {
            String lastChecked = plugin.getLastChecked();

            if ( StringUtils.isEmpty( lastChecked ) )
            {
                return true;
            }
            else
            {
                SimpleDateFormat format =
                    new SimpleDateFormat( org.apache.maven.plugin.registry.Plugin.LAST_CHECKED_DATE_FORMAT );

                try
                {
                    Date lastCheckedDate = format.parse( lastChecked );

                    return IntervalUtils.isExpired( pluginRegistry.getUpdateInterval(), lastCheckedDate );
                }
                catch ( ParseException e )
                {
                    getLogger().warn( "Last-checked date for plugin {" + constructPluginKey( groupId, artifactId ) +
                        "} is invalid. Checking for updates." );

                    return true;
                }
            }
        }
    }

    private boolean checkForRejectedStatus( String groupId, String artifactId, String version )
        throws PluginVersionResolutionException
    {
        PluginRegistry pluginRegistry = getPluginRegistry( groupId, artifactId );

        org.apache.maven.plugin.registry.Plugin plugin = getPlugin( groupId, artifactId, pluginRegistry );

        return plugin.getRejectedVersions().contains( version );
    }

    private boolean promptToPersistPluginUpdate( String version, String updatedVersion, String groupId,
                                                 String artifactId, Settings settings )
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

            message.append( "Detected plugin version: " ).append( updatedVersion ).append( "\n" );
            message.append( "\n" );
            message.append( "Would you like to use this new version from now on? ( [Y]es, [n]o, [a]ll, n[o]ne ) " );

            // TODO: check the GUI-friendliness of this approach to collecting input.
            // If we can't port this prompt into a GUI, IDE-integration will not work well.
            getLogger().info( message.toString() );

            String persistAnswer = inputHandler.readLine();

            boolean shouldPersist = true;

            if ( !StringUtils.isEmpty( persistAnswer ) )
            {
                persistAnswer = persistAnswer.toLowerCase();

                if ( persistAnswer.startsWith( "y" ) )
                {
                    shouldPersist = true;
                }
                else if ( persistAnswer.startsWith( "a" ) )
                {
                    shouldPersist = true;

                    settings.getRuntimeInfo().setApplyToAllPluginUpdates( Boolean.TRUE );
                }
                else if ( persistAnswer.indexOf( "o" ) > -1 )
                {
                    settings.getRuntimeInfo().setApplyToAllPluginUpdates( Boolean.FALSE );
                }
                else if ( persistAnswer.startsWith( "n" ) )
                {
                    shouldPersist = false;
                }
                else
                {
                    // default to yes.
                    shouldPersist = true;
                }
            }

            if ( shouldPersist )
            {
                getLogger().info( "Updating plugin version to " + updatedVersion );
            }
            else
            {
                getLogger().info( "NOT updating plugin version to " + updatedVersion );
            }

            return shouldPersist;

        }
        catch ( IOException e )
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

            getLogger().warn( "Plugin version: " + rejectedVersion + " added to your rejectedVersions list.\n" +
                "You will not be prompted for this version again.\n\nPlugin: " + pluginKey );
        }
        else
        {
            getLogger().warn(
                "Cannot add rejectedVersion entry for: " + rejectedVersion + ".\n\nPlugin: " + pluginKey );
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
        Map pluginsByKey;

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

    private String getVersionFromPluginConfig( String groupId, String artifactId, MavenProject project,
                                               boolean resolveAsReportPlugin )
    {
        String version = null;

        if ( resolveAsReportPlugin )
        {
            if ( project.getReportPlugins() != null )
            {
                for ( Iterator it = project.getReportPlugins().iterator(); it.hasNext() && version == null; )
                {
                    ReportPlugin plugin = (ReportPlugin) it.next();

                    if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                    {
                        version = plugin.getVersion();
                    }
                }
            }
        }
        else
        {
            if ( project.getBuildPlugins() != null )
            {
                for ( Iterator it = project.getBuildPlugins().iterator(); it.hasNext() && version == null; )
                {
                    Plugin plugin = (Plugin) it.next();

                    if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                    {
                        version = plugin.getVersion();
                    }
                }
            }
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
                getLogger().warn( "Cannot update registered version for plugin {" + groupId + ":" + artifactId +
                    "}; it is specified in the global registry." );
            }
            else
            {
                plugin.setUseVersion( version );

                SimpleDateFormat format =
                    new SimpleDateFormat( org.apache.maven.plugin.registry.Plugin.LAST_CHECKED_DATE_FORMAT );

                plugin.setLastChecked( format.format( new Date() ) );
            }
        }
        else
        {
            plugin = new org.apache.maven.plugin.registry.Plugin();

            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );
            plugin.setUseVersion( version );

            pluginRegistry.addPlugin( plugin );

            pluginRegistry.flushPluginsByKey();
        }

        writeUserRegistry( groupId, artifactId, pluginRegistry );
    }

    private void writeUserRegistry( String groupId, String artifactId, PluginRegistry pluginRegistry )
    {
        File pluginRegistryFile = pluginRegistry.getRuntimeInfo().getFile();

        PluginRegistry extractedUserRegistry = PluginRegistryUtils.extractUserPluginRegistry( pluginRegistry );

        // only rewrite the user-level registry if one existed before, or if we've created user-level data here.
        if ( extractedUserRegistry != null )
        {
            Writer fWriter = null;

            try
            {
                pluginRegistryFile.getParentFile().mkdirs();
                fWriter = WriterFactory.newXmlWriter( pluginRegistryFile );

                PluginRegistryXpp3Writer writer = new PluginRegistryXpp3Writer();

                writer.write( fWriter, extractedUserRegistry );
            }
            catch ( IOException e )
            {
                getLogger().warn(
                    "Cannot rewrite user-level plugin-registry.xml with new plugin version of plugin: \'" + groupId +
                        ":" + artifactId + "\'.", e );
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
        if ( pluginRegistry == null )
        {
            try
            {
                pluginRegistry = mavenPluginRegistryBuilder.buildPluginRegistry();
            }
            catch ( IOException e )
            {
                throw new PluginVersionResolutionException( groupId, artifactId,
                                                            "Error reading plugin registry: " + e.getMessage(), e );
            }
            catch ( XmlPullParserException e )
            {
                throw new PluginVersionResolutionException( groupId, artifactId,
                                                            "Error parsing plugin registry: " + e.getMessage(), e );
            }

            if ( pluginRegistry == null )
            {
                pluginRegistry = mavenPluginRegistryBuilder.createUserPluginRegistry();
            }
        }

        return pluginRegistry;
    }

    private String resolveMetaVersion( String groupId, String artifactId, MavenProject project,
                                       ArtifactRepository localRepository, String metaVersionId )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        Artifact artifact = artifactFactory.createProjectArtifact( groupId, artifactId, metaVersionId );

        String key = artifact.getDependencyConflictId();
        if ( resolvedMetaVersions.containsKey( key ) )
        {
            return (String) resolvedMetaVersions.get( key );
        }

        String version = null;

        // This takes the spec version and resolves a real version
        try
        {
            ResolutionGroup resolutionGroup =
                artifactMetadataSource.retrieve( artifact, localRepository, project.getPluginArtifactRepositories() );

            // switching this out with the actual resolved artifact instance, since the MMSource re-creates the pom
            // artifact.
            artifact = resolutionGroup.getPomArtifact();
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new PluginVersionResolutionException( groupId, artifactId, e.getMessage(), e );
        }

        String artifactVersion = artifact.getVersion();

        // make sure this artifact was actually resolved to a file in the repo...
        if ( artifact.getFile() != null )
        {
            boolean pluginValid = false;

            while ( !pluginValid && artifactVersion != null )
            {
                pluginValid = true;
                MavenProject pluginProject;
                try
                {
                    artifact = artifactFactory.createProjectArtifact( groupId, artifactId, artifactVersion );
                    pluginProject = mavenProjectBuilder.buildFromRepository( artifact,
                                                                             project.getPluginArtifactRepositories(),
                                                                             localRepository, false );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new InvalidPluginException( "Unable to build project information for plugin '" +
                        ArtifactUtils.versionlessKey( groupId, artifactId ) + "': " + e.getMessage(), e );
                }

                // if we don't have the required Maven version, then ignore an update
                if ( pluginProject.getPrerequisites() != null && pluginProject.getPrerequisites().getMaven() != null )
                {
                    DefaultArtifactVersion requiredVersion =
                        new DefaultArtifactVersion( pluginProject.getPrerequisites().getMaven() );

                    if ( runtimeInformation.getApplicationVersion().compareTo( requiredVersion ) < 0 )
                    {
                        getLogger().info( "Ignoring available plugin update: " + artifactVersion +
                            " as it requires Maven version " + requiredVersion );

                        VersionRange vr;
                        try
                        {
                            vr = VersionRange.createFromVersionSpec( "(," + artifactVersion + ")" );
                        }
                        catch ( InvalidVersionSpecificationException e )
                        {
                            throw new PluginVersionResolutionException( groupId, artifactId,
                                                                        "Error getting available plugin versions: " +
                                                                            e.getMessage(), e );
                        }

                        getLogger().debug( "Trying " + vr );
                        try
                        {
                            List versions = artifactMetadataSource.retrieveAvailableVersions( artifact, localRepository,
                                                                                              project.getPluginArtifactRepositories() );
                            ArtifactVersion v = vr.matchVersion( versions );
                            artifactVersion = v != null ? v.toString() : null;
                        }
                        catch ( ArtifactMetadataRetrievalException e )
                        {
                            throw new PluginVersionResolutionException( groupId, artifactId,
                                                                        "Error getting available plugin versions: " +
                                                                            e.getMessage(), e );
                        }

                        if ( artifactVersion != null )
                        {
                            getLogger().debug( "Found " + artifactVersion );
                            pluginValid = false;
                        }
                    }
                }
            }
        }

        if ( !metaVersionId.equals( artifactVersion ) )
        {
            version = artifactVersion;
            resolvedMetaVersions.put( key, version );
        }

        return version;
    }

}
