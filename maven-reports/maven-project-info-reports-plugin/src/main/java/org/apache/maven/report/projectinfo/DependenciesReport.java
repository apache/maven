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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Generates the dependencies report.
 * 
 * @goal dependencies
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
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
        try
        {
            DependenciesRenderer r = new DependenciesRenderer( getSink(), getProject(), locale );

            r.render();
        }
        catch( IOException e )
        {
            throw new MavenReportException( "Can't write the report " + getOutputName(), e );
        }
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

        public DependenciesRenderer( Sink sink, MavenProject project, Locale locale )
        {
            super( sink );

            this.project = project;

            this.locale = locale;
        }

        public String getTitle()
        {
            return getBundle( locale ).getString( "report.dependencies.title" );
        }

        public void renderBody()
        {
            startSection( getTitle() );

            // Dependencies report
            List dependencies = project.getDependencies();
            
            if ( dependencies.isEmpty() )
            {
                // TODO: should the report just be excluded?
                paragraph( getBundle( locale ).getString( "report.dependencies.nolist" ) );
            }
            else
            {
                startTable();

                tableCaption( getBundle( locale ).getString( "report.dependencies.intro" ) );

                String groupId = getBundle( locale ).getString( "report.dependencies.column.groupId" );
                String artifactId = getBundle( locale ).getString( "report.dependencies.column.artifactId" );
                String version = getBundle( locale ).getString( "report.dependencies.column.version" );

                tableHeader( new String[]{groupId, artifactId, version} );

                for ( Iterator i = dependencies.iterator(); i.hasNext(); )
                {
                    Dependency d = (Dependency) i.next();

                    tableRow( new String[]{d.getGroupId(), d.getArtifactId(), d.getVersion()} );
                }

                endTable();
            }
            
            endSection();

            // Transitive dependencies
            if ( !dependencies.isEmpty() )
            {
                Set artifacts = getTransitiveDependencies( project );
                
                startSection( getBundle( locale ).getString( "report.transitivedependencies.title" ) );
    
                if ( artifacts.isEmpty() )
                {
                    // TODO: should the report just be excluded?
                    paragraph( getBundle( locale ).getString( "report.transitivedependencies.nolist" ) );
                }
                else
                {
                    startTable();
    
                    tableCaption( getBundle( locale ).getString( "report.transitivedependencies.intro" ) );
    
                    String groupId = getBundle( locale ).getString( "report.transitivedependencies.column.groupId" );
                    String artifactId = getBundle( locale ).getString( "report.transitivedependencies.column.artifactId" );
                    String version = getBundle( locale ).getString( "report.transitivedependencies.column.version" );
    
                    tableHeader( new String[]{groupId, artifactId, version} );
    
                    for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                    {
                        Artifact artifact = (Artifact) i.next();
    
                        tableRow( new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()} );
                    }
    
                    endTable();
                }

                endSection();
            }

        }

        /**
         * Return a set of artifact which are not already present in the dependencies list.
         *  
         * @param project a Maven project
         * @return a set of transitive dependencies
         */
        private Set getTransitiveDependencies( MavenProject project ) 
        {            
            Set result = new HashSet();

            if ( ( project.getDependencies() == null ) ||
                    ( project.getArtifacts() == null ) )
            {
                return result;
            }
            
            List dependencies = project.getDependencies();
            Set artifacts = project.getArtifacts();

            for ( Iterator j = artifacts.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact)j.next();

                boolean toadd = true;
                for ( Iterator i = dependencies.iterator(); i.hasNext(); )
                {
                    Dependency dependency = (Dependency) i.next();
                    if ( ( artifact.getArtifactId().equals( dependency.getArtifactId() ) ) && 
                            ( artifact.getGroupId().equals( dependency.getGroupId() ) )  && 
                            ( artifact.getVersion().equals( dependency.getVersion() ) ) )
                    {
                        toadd = false;
                        break;
                    }
                }
                
                if ( toadd )
                {
                    result.add( artifact );
                }
            }
            
            return result;
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle("project-info-report", locale, DependenciesReport.class.getClassLoader() );
    }
}
