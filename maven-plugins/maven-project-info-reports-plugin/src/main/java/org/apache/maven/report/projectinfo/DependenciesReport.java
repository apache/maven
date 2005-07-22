package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal dependencies
 * @plexus.component
 */
public class DependenciesReport
    extends AbstractMavenReport
{
    /**
     * @parameter expression="${project.build.directory}/site"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectBuilder}"
     * @required
     * @readonly
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.dependencies.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.dependencies.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        DependenciesRenderer r = new DependenciesRenderer( getSink(), getProject(), locale, mavenProjectBuilder,
                                                           artifactFactory, localRepository );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependencies";
    }

    static class DependenciesRenderer
        extends AbstractMavenReportRenderer
    {
        private MavenProject project;

        private Locale locale;

        private ArtifactFactory artifactFactory;

        private MavenProjectBuilder mavenProjectBuilder;

        private ArtifactRepository localRepository;

        public DependenciesRenderer( Sink sink, MavenProject project, Locale locale,
                                     MavenProjectBuilder mavenProjectBuilder, ArtifactFactory artifactFactory,
                                     ArtifactRepository localRepository )
        {
            super( sink );

            this.project = project;

            this.locale = locale;

            this.mavenProjectBuilder = mavenProjectBuilder;

            this.artifactFactory = artifactFactory;

            this.localRepository = localRepository;
        }

        public String getTitle()
        {
            return getBundle( locale ).getString( "report.dependencies.title" );
        }

        public void renderBody()
        {
            // Dependencies report
            List dependencies = project.getDependencies();

            if ( dependencies == null || dependencies.isEmpty() )
            {
                startSection( getTitle() );

                // TODO: should the report just be excluded?
                paragraph( getBundle( locale ).getString( "report.dependencies.nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            startTable();

            tableCaption( getBundle( locale ).getString( "report.dependencies.intro" ) );

            String groupId = getBundle( locale ).getString( "report.dependencies.column.groupId" );
            String artifactId = getBundle( locale ).getString( "report.dependencies.column.artifactId" );
            String version = getBundle( locale ).getString( "report.dependencies.column.version" );
            String description = getBundle( locale ).getString( "report.dependencies.column.description" );
            String url = getBundle( locale ).getString( "report.dependencies.column.url" );

            tableHeader( new String[]{groupId, artifactId, version, description, url} );

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dependency = (Dependency) i.next();

                Artifact artifact = artifactFactory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                                    dependency.getVersion(), dependency.getScope(),
                                                                    dependency.getType() );
                MavenProject artifactProject;
                try
                {
                    // TODO: can we use @requiresDependencyResolution instead, and capture the depth of artifacts in the artifact itself?
                    artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new IllegalArgumentException(
                        "Can't find a valid Maven project in the repository for the artifact [" + artifact + "]." );
                }

                tableRow( new String[]{dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                    artifactProject.getDescription(),
                    createLinkPatternedText( artifactProject.getUrl(), artifactProject.getUrl() )} );
            }

            endTable();

            endSection();

            // Transitive dependencies
            Set artifacts = getTransitiveDependencies( project );

            startSection( getBundle( locale ).getString( "report.transitivedependencies.title" ) );

            if ( artifacts.isEmpty() )
            {
                paragraph( getBundle( locale ).getString( "report.transitivedependencies.nolist" ) );
            }
            else
            {
                startTable();

                tableCaption( getBundle( locale ).getString( "report.transitivedependencies.intro" ) );

                tableHeader( new String[]{groupId, artifactId, version, description, url} );

                for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                {
                    Artifact artifact = (Artifact) i.next();

                    MavenProject artifactProject;
                    try
                    {
                        artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                    }
                    catch ( ProjectBuildingException e )
                    {
                        throw new IllegalArgumentException(
                            "Can't find a valid Maven project in the repository for the artifact [" + artifact + "]." );
                    }
                    tableRow( new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                        artifactProject.getDescription(),
                        createLinkPatternedText( artifactProject.getUrl(), artifactProject.getUrl() )} );
                }

                endTable();
            }

            endSection();
        }

        /**
         * Return a set of <code>Artifacts</code> which are not already
         * present in the dependencies list.
         *
         * @param project a Maven project
         * @return a set of transitive dependencies as artifacts
         */
        private Set getTransitiveDependencies( MavenProject project )
        {
            Set transitiveDependencies = new HashSet();

            List dependencies = project.getDependencies();
            Set artifacts = project.getArtifacts();

            if ( dependencies == null || artifacts == null )
            {
                return transitiveDependencies;
            }

            List dependenciesAsArtifacts = new ArrayList( dependencies.size() );
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dependency = (Dependency) i.next();

                Artifact artifact = artifactFactory.createArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                                    dependency.getVersion(), dependency.getScope(),
                                                                    dependency.getType() );
                dependenciesAsArtifacts.add( artifact );
            }

            for ( Iterator j = artifacts.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                if ( !dependenciesAsArtifacts.contains( artifact ) )
                {
                    transitiveDependencies.add( artifact );
                }
            }

            return transitiveDependencies;
        }

        /**
         * Get the <code>Maven project</code> from the repository depending
         * the <code>Artifact</code> given.
         *
         * @param artifact an artifact
         * @return the Maven project for the given artifact
         * @throws org.apache.maven.project.ProjectBuildingException if any
         */
        private MavenProject getMavenProjectFromRepository( Artifact artifact, ArtifactRepository localRepository )
            throws ProjectBuildingException
        {
            // TODO: we should use the MavenMetadataSource instead
            return mavenProjectBuilder.buildFromRepository( artifact, project.getRepositories(), localRepository );
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, DependenciesReport.class.getClassLoader() );
    }
}