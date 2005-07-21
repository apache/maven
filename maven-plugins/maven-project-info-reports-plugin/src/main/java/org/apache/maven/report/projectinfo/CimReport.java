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

import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generates the Project Continuous Integration System report.
 * 
 * @goal cim
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 */
public class CimReport
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
        return getBundle( locale ).getString( "report.cim.name" );
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
        return getBundle( locale ).getString( "report.cim.description" );
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
        CimRenderer r = new CimRenderer( getSink(), getProject().getModel(), locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "integration";
    }

    static class CimRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private Locale locale;

        public CimRenderer( Sink sink, Model model, Locale locale )
        {
            super( sink );

            this.model = model;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.cim.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            CiManagement cim = model.getCiManagement();
            if ( cim == null )
            {
                startSection( getTitle() );

                paragraph( getBundle( locale ).getString( "report.cim.nocim" ) );

                endSection();

                return;
            }

            String system = cim.getSystem();
            String url = cim.getUrl();
            List notifiers = cim.getNotifiers();

            // Overview
            startSection( getBundle( locale ).getString( "report.cim.overview.title" ) );

            if ( isCimSystem( system, "continuum" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.cim.continuum.intro" ) );
            }
            else if ( isCimSystem( system, "bugzilla" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.cim.bugzilla.intro" ) );
            }
            else
            {
                linkPatternedText( getBundle( locale ).getString( "report.cim.general.intro" ) );
            }

            endSection();

            // Access
            startSection( getBundle( locale ).getString( "report.cim.access" ) );

            if ( !StringUtils.isEmpty( url ) )
            {
                paragraph( getBundle( locale ).getString( "report.cim.url" ) );

                verbatimLink( url, url );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.cim.nourl" ) );
            }

            endSection();

            // Notifiers
            startSection( getBundle( locale ).getString( "report.cim.notifiers.title" ) );

            if ( ( notifiers == null ) || ( notifiers.isEmpty() ) )
            {
                paragraph( getBundle( locale ).getString( "report.cim.notifiers.nolist" ) );
            }
            else
            {
                startTable();

                tableCaption( getBundle( locale ).getString( "report.cim.notifiers.intro" ) );

                String type = getBundle( locale ).getString( "report.cim.notifiers.column.type" );
                String address = getBundle( locale ).getString( "report.cim.notifiers.column.address" );
                String configuration = getBundle( locale ).getString( "report.cim.notifiers.column.configuration" );

                tableHeader( new String[] { type, address, configuration } );

                for ( Iterator i = notifiers.iterator(); i.hasNext(); )
                {
                    Notifier notifier = (Notifier) i.next();

                    tableRow( new String[] {
                        notifier.getType(),
                        createLinkPatternedText( notifier.getAddress(), notifier.getAddress() ),
                        propertiesToString( notifier.getConfiguration() ) } );
                }

                endTable();
            }

            endSection();
        }

        /**
         * Checks if a CIM system is bugzilla, continium...
         * 
         * @return true if the CIM system is bugzilla, continium..., false
         *         otherwise.
         */
        private boolean isCimSystem( String connection, String cim )
        {
            if ( StringUtils.isEmpty( connection ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( cim ) )
            {
                return false;
            }

            if ( connection.toLowerCase().startsWith( cim.toLowerCase() ) )
            {
                return true;
            }

            return false;
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, CimReport.class.getClassLoader() );
    }
}