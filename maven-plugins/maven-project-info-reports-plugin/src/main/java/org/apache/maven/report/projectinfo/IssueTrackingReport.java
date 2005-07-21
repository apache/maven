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

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generates the Project Issue Tracking report.
 * 
 * @goal issue-tracking
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 */
public class IssueTrackingReport
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
        return getBundle( locale ).getString( "report.issuetracking.name" );
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
        return getBundle( locale ).getString( "report.issuetracking.description" );
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
            IssueTrackingRenderer r = new IssueTrackingRenderer( getSink(), getProject().getModel(), locale );

            r.render();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't write the report " + getOutputName(), e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "issue-tracking";
    }

    static class IssueTrackingRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private Locale locale;

        public IssueTrackingRenderer( Sink sink, Model model, Locale locale )
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
            return getBundle( locale ).getString( "report.issuetracking.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            IssueManagement issueManagement = model.getIssueManagement();
            if ( issueManagement == null )
            {
                startSection( getTitle() );

                paragraph( getBundle( locale ).getString( "report.issuetracking.noissueManagement" ) );

                endSection();

                return;
            }

            String system = issueManagement.getSystem();
            String url = issueManagement.getUrl();

            // Overview
            startSection( getBundle( locale ).getString( "report.issuetracking.overview.title" ) );

            if ( isIssueManagementSystem( system, "jira" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.issuetracking.jira.intro" ) );
            }
            else if ( isIssueManagementSystem( system, "bugzilla" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.issuetracking.bugzilla.intro" ) );
            }
            else if ( isIssueManagementSystem( system, "scarab" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.issuetracking.scarab.intro" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.issuetracking.general.intro" ) );
            }

            endSection();

            // Connection
            startSection( getTitle() );

            paragraph( getBundle( locale ).getString( "report.issuetracking.intro" ) );

            verbatimLink( url, url );

            endSection();
        }

        /**
         * Checks if a issue management system is Jira, bugzilla...
         * 
         * @return true if the issue management system is Jira, bugzilla, false
         *         otherwise.
         */
        private boolean isIssueManagementSystem( String system, String im )
        {
            if ( StringUtils.isEmpty( system ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( im ) )
            {
                return false;
            }

            if ( system.toLowerCase().startsWith( im.toLowerCase() ) )
            {
                return true;
            }

            return false;
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, IssueTrackingReport.class.getClassLoader() );
    }
}