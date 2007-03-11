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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultPluginVersionManager
    extends AbstractLogEnabled
    implements PluginVersionManager
{
    private ArtifactFactory artifactFactory;

    private ArtifactMetadataSource artifactMetadataSource;

    private MavenProjectBuilder mavenProjectBuilder;

    private RuntimeInformation runtimeInformation;

    // TODO: Revisit to remove this piece of state. PLUGIN REGISTRY MAY BE UPDATED ON DISK OUT-OF-PROCESS!
    private Map resolvedMetaVersions = new HashMap();

    public String resolvePluginVersion( String groupId,
                                        String artifactId,
                                        MavenProject project,
                                        MavenSession session )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        return resolvePluginVersion( groupId, artifactId, project, session, false );
    }

    public String resolveReportPluginVersion( String groupId,
                                              String artifactId,
                                              MavenProject project,
                                              MavenSession session )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        return resolvePluginVersion( groupId, artifactId, project, session, true );
    }

    private String resolvePluginVersion( String groupId,
                                         String artifactId,
                                         MavenProject project,
                                         MavenSession session,
                                         boolean resolveAsReportPlugin )
        throws PluginVersionResolutionException, InvalidPluginException, PluginVersionNotFoundException
    {
        ArtifactRepository localRepository = session.getLocalRepository();

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

        // third pass...we're always checking for latest install/deploy, so retrieve the version for LATEST metadata and
        // also set that resolved version as the <useVersion/> in settings.xml.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve the version to be used
            version = resolveMetaVersion( groupId, artifactId, project, localRepository, Artifact.LATEST_VERSION );
        }

        // final pass...retrieve the version for RELEASE and also set that resolved version as the <useVersion/>
        // in settings.xml.
        if ( StringUtils.isEmpty( version ) )
        {
            // 1. resolve the version to be used
            version = resolveMetaVersion( groupId, artifactId, project, localRepository, Artifact.RELEASE_VERSION );
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

        return version;
    }

    private String getVersionFromPluginConfig( String groupId,
                                               String artifactId,
                                               MavenProject project,
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

    private String resolveMetaVersion( String groupId,
                                       String artifactId,
                                       MavenProject project,
                                       ArtifactRepository localRepository,
                                       String metaVersionId )
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
