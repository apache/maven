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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * @goal dependencies
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
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
        return "Dependencies";
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
        return "This document lists the projects dependencies and provides information on each dependency.";
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
            DependenciesRenderer r = new DependenciesRenderer( getSink(), getProject().getModel() );

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
        private Model model;

        public DependenciesRenderer( Sink sink, Model model )
        {
            super( sink );

            this.model = model;
        }

        // How to i18n these ...
        public String getTitle()
        {
            return "Project Dependencies";
        }

        public void renderBody()
        {
            startSection( getTitle() );

            if ( model.getDependencies().isEmpty() )
            {
                // TODO: should the report just be excluded?
                paragraph( "There are no dependencies for this project. It is a standalone " +
                           "application that does not depend on any other project." );
            }
            else
            {
                startTable();

                tableCaption( "The following is a list of dependencies for this project. These dependencies " +
                              "are required to compile and run the application:" );

                tableHeader( new String[]{"GroupId", "ArtifactId", "Version"} );

                for ( Iterator i = model.getDependencies().iterator(); i.hasNext(); )
                {
                    Dependency d = (Dependency) i.next();

                    tableRow( new String[]{d.getGroupId(), d.getArtifactId(), d.getVersion()} );
                }

                endTable();
            }

            endSection();
        }

    }
}
