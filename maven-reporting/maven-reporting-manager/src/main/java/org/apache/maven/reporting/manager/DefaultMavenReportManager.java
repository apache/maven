package org.apache.maven.reporting.manager;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Reports;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.ArtifactEnabledContainerException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage the set of available reports.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DefaultMavenReportManager.java 163376 2005-02-23 00:06:06Z brett $
 * @plexus.component
 */
public class DefaultMavenReportManager
    extends AbstractLogEnabled
    implements MavenReportManager, Contextualizable
{
    private ArtifactFactory artifactFactory;

    protected PlexusContainer container;

    private Map mavenReports;

    public void addReports( Reports reports, ArtifactRepository localRepository, List remoteRepositories )
        throws ReportManagerException, ReportNotFoundException
    {
        for ( Iterator i = reports.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin pluginReport = (Plugin) i.next();

            String groupId = pluginReport.getGroupId();

            String artifactId = pluginReport.getArtifactId();

            String version = pluginReport.getVersion();

            try
            {
                Artifact pluginArtifact = artifactFactory.createArtifact( pluginReport.getGroupId(),
                                                                          pluginReport.getArtifactId(),
                                                                          pluginReport.getVersion(),
                                                                          null, "maven-plugin", null );

                addPlugin( pluginArtifact, localRepository, remoteRepositories );
            }
            catch ( ArtifactEnabledContainerException e )
            {
                throw new ReportManagerException( "Error occurred in the artifact container attempting to download plugin " +
                                                  groupId + ":" + artifactId, e );
            }
            catch ( ArtifactResolutionException e )
            {
                if ( groupId.equals( e.getGroupId() ) && artifactId.equals( e.getArtifactId() ) &&
                    version.equals( e.getVersion() ) && "maven-plugin".equals( e.getType() ) )
                {
                    throw new ReportNotFoundException( groupId, artifactId, version, e );
                }
                else
                {
                    throw new ReportManagerException( "Error occurred in the artifact resolver attempting to download plugin " +
                                                      groupId + ":" + artifactId, e );
                }
            }
            catch ( ComponentLookupException e )
            {
                throw new ReportManagerException( "Error occurred in the container attempting to load report plugin " +
                                                  groupId + ":" + artifactId, e );
            }
        }
    }

    private void addPlugin( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactEnabledContainerException, ArtifactResolutionException, ComponentLookupException
    {
        ArtifactResolver artifactResolver = null;

        MavenProjectBuilder mavenProjectBuilder = null;

        try
        {
            artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );

            MavenMetadataSource metadataSource = new MavenMetadataSource( artifactResolver, mavenProjectBuilder );

            ( (ArtifactEnabledContainer) container ).addComponent( artifact, artifactResolver,
                                                                   remoteRepositories,
                                                                   localRepository, metadataSource,
                                                                   null );
        }
        finally
        {
            if ( artifactResolver != null )
            {
                releaseComponent( artifactResolver );
            }
            if ( mavenProjectBuilder != null )
            {
                releaseComponent( mavenProjectBuilder );
            }
        }
    }

    private void releaseComponent( Object component )
    {
        try
        {
            container.release( component );
        }
        catch ( ComponentLifecycleException e )
        {
            getLogger().error( "Error releasing component - ignoring", e );
        }
    }

    /**
     * @todo we need some type of response
     */
    public void executeReport( String name, MavenReportConfiguration config, String outputDirectory )
        throws MavenReportException
    {
        MavenReport report = (MavenReport) mavenReports.get( name );

        if ( report != null )
        {
            report.execute( config );
        }
        else
        {
            throw new MavenReportException( "The report " + name + " doesn't exist." );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
